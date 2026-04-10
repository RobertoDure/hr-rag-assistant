package ie.com.rag.security;

import ie.com.rag.entity.SystemUser;
import ie.com.rag.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        // Arrange
        SystemUser systemUser = new SystemUser();
        systemUser.setUsername("john");
        systemUser.setPassword("encodedPassword");
        systemUser.setRole("ADMIN");
        systemUser.setAccountNonExpired(true);
        systemUser.setAccountNonLocked(true);
        systemUser.setCredentialsNonExpired(true);
        systemUser.setEnabled(true);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(systemUser));

        // Act
        UserDetails result = service.loadUserByUsername("john");

        // Assert
        assertThat(result.getUsername()).isEqualTo("john");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void loadUserByUsername_disabledUser_returnsDisabledDetails() {
        // Arrange
        SystemUser systemUser = new SystemUser();
        systemUser.setUsername("inactive");
        systemUser.setPassword("pass");
        systemUser.setRole("USER");
        systemUser.setAccountNonExpired(true);
        systemUser.setAccountNonLocked(true);
        systemUser.setCredentialsNonExpired(true);
        systemUser.setEnabled(false);

        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(systemUser));

        // Act
        UserDetails result = service.loadUserByUsername("inactive");

        // Assert
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_lockedUser_returnsLockedDetails() {
        // Arrange
        SystemUser systemUser = new SystemUser();
        systemUser.setUsername("locked");
        systemUser.setPassword("pass");
        systemUser.setRole("USER");
        systemUser.setAccountNonExpired(true);
        systemUser.setAccountNonLocked(false);
        systemUser.setCredentialsNonExpired(true);
        systemUser.setEnabled(true);

        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(systemUser));

        // Act
        UserDetails result = service.loadUserByUsername("locked");

        // Assert
        assertThat(result.isAccountNonLocked()).isFalse();
    }
}
