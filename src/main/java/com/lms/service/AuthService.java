package com.lms.service;

import com.lms.dto.*;
import com.lms.model.EmailVerification;
import com.lms.model.PasswordResetToken;
import com.lms.model.RefreshToken;
import com.lms.model.User;
import com.lms.repository.*;
import com.lms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final TenantService tenantService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndTenantSubdomain(request.getEmail(), com.lms.tenant.TenantContext.getCurrentTenant())) {
            throw new RuntimeException("Email already exists in this organization");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setTenant(tenantService.getCurrentTenant());

        User savedUser = userRepository.save(user);

        // Send email verification
        sendVerificationEmail(savedUser);

        String accessToken = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId());
        String refreshToken = createRefreshToken(savedUser);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String currentTenant = com.lms.tenant.TenantContext.getCurrentTenant();

        // Resolve user with tenant isolation
        User user;
        if (currentTenant == null || currentTenant.isBlank() || currentTenant.equals("global")) {
            // Global context: allow super admins and users whose tenant is "global"
            user = userRepository.findByEmailAndTenantSubdomain(request.getEmail(), "global")
                    .or(() -> userRepository.findByEmail(request.getEmail())
                            .filter(u -> u.getRole() == User.Role.SUPER_ADMIN))
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        } else {
            // Specific tenant context: only look up within that tenant
            user = userRepository.findByEmailAndTenantSubdomain(request.getEmail(), currentTenant)
                    .orElseThrow(() -> new RuntimeException("Invalid email or password for this organization"));
        }

        // Verify password via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled. Please contact your administrator.");
        }

        if (user.getStatus() != null && user.getStatus() == User.Status.SUSPENDED) {
            throw new RuntimeException("Account is suspended. Please contact your administrator.");
        }

        // Revoke existing refresh tokens and issue new ones
        refreshTokenRepository.revokeAllByUser(user);

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        String newRefreshToken = createRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerification verification = emailVerificationRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new RuntimeException("Invalid or already used verification token"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired");
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }
        emailVerificationRepository.invalidateAllByUser(user);
        sendVerificationEmail(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.invalidateAllByUser(user);
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(token);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
        });
        // Always return success (don't leak email existence)
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new RuntimeException("Invalid or already used reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    private void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        EmailVerification ev = new EmailVerification();
        ev.setUser(user);
        ev.setToken(token);
        ev.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationRepository.save(ev);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        String subdomain = (user.getTenant() != null) ? user.getTenant().getSubdomain() : "global";
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole().name());
        response.setUserId(user.getId());
        response.setEmailVerified(user.isEmailVerified());
        response.setTenantSubdomain(subdomain);
        return response;
    }
}
