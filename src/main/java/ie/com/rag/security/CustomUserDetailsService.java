package ie.com.rag.security;

import ie.com.rag.entity.SystemUser;
import ie.com.rag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

/**
 * Custom User Details Service for database-backed authentication
 * Loads user details from PostgreSQL database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Load user from database
        SystemUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("❌ User not found: {}", username);
                return new UsernameNotFoundException("User not found: " + username);
            });

        // Map User entity to Spring Security UserDetails
        // CRITICAL: These flags are INVERTED when building UserDetails!
        // accountExpired(true) means account IS expired
        // We have accountNonExpired(true) from DB, so we need accountExpired(false)

        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword()) // Already BCrypt encoded from database
            .authorities(Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
            ))
            .accountExpired(!user.getAccountNonExpired())      // DB: true -> Builder: false
            .accountLocked(!user.getAccountNonLocked())        // DB: true -> Builder: false
            .credentialsExpired(!user.getCredentialsNonExpired()) // DB: true -> Builder: false
            .disabled(!user.getEnabled())                      // DB: true -> Builder: false
            .build();
    }
}

