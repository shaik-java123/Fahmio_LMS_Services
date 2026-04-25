package com.lms.security;

import com.lms.model.User;
import com.lms.repository.UserRepository;
import com.lms.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String currentTenant = TenantContext.getCurrentTenant();

        User user;
        if (currentTenant == null || currentTenant.isBlank() || currentTenant.equals("global")) {
            // Global context: find by email only (covers super admin + global tenant users)
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        } else {
            // Tenant context: find within the specific tenant first, fallback to email only
            user = userRepository.findByEmailAndTenantSubdomain(email, currentTenant)
                    .or(() -> userRepository.findByEmail(email)
                            .filter(u -> u.getRole() == User.Role.SUPER_ADMIN))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
