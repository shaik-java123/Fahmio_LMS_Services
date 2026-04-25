package com.lms.repository;

import com.lms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndTenantSubdomain(String email, String subdomain);
    boolean existsByEmail(String email);
    boolean existsByEmailAndTenantSubdomain(String email, String subdomain);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndTenantSubdomain(User.Role role, String subdomain);
    List<User> findByTenantSubdomain(String subdomain);
    List<User> findTop10ByTenantSubdomainOrderByLearningPointsDesc(String subdomain);
}
