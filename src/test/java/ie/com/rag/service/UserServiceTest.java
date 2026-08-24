package ie.com.rag.service;

import ie.com.rag.dto.RegisterRequestDTO;
import ie.com.rag.dto.UpdateUserRequestDTO;
import ie.com.rag.dto.UserResponseDTO;
import ie.com.rag.entity.SystemUser;
import ie.com.rag.entity.UserRole;
import ie.com.rag.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private SystemUser buildUser(final String id, final String username, final String email, final String role) {
        return SystemUser.builder()
                .id(id)
                .username(username)
                .email(email)
                .password("encoded")
                .firstName("John")
                .lastName("Doe")
                .role(role)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();
    }

    private RegisterRequestDTO buildRegisterRequest(final String role) {
        return new RegisterRequestDTO("john.doe", "john@example.com", "Password1", "John", "Doe", role);
    }

    // ------------------------------------------------------------------
    // registerUser
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should register a user successfully with encoded password and USER role")
    void registerUser_success_returnsUserResponse() {
        // Given
        final RegisterRequestDTO request = buildRegisterRequest(null);
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> {
                    final SystemUser user = invocation.getArgument(0);
                    user.setId("u-1");
                    return user;
                });

        // When
        final UserResponseDTO result = userService.registerUser(request);

        // Then
        assertThat(result.id()).isEqualTo("u-1");
        assertThat(result.username()).isEqualTo("john.doe");
        assertThat(result.email()).isEqualTo("john@example.com");
        assertThat(result.role()).isEqualTo("USER");
        assertThat(result.enabled()).isTrue();

        final ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should keep the requested role when a valid role is provided")
    void registerUser_validRole_keepsRole() {
        // Given
        final RegisterRequestDTO request = buildRegisterRequest("ADMIN");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(SystemUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        userService.registerUser(request);

        // Then
        final ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should default to USER role when role is invalid")
    void registerUser_invalidRole_defaultsToUser() {
        // Given
        final RegisterRequestDTO request = buildRegisterRequest("SUPERUSER");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(SystemUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        userService.registerUser(request);

        // Then
        final ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when request is null")
    void registerUser_nullRequest_throws() {
        assertThatThrownBy(() -> userService.registerUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Should throw when username already exists")
    void registerUser_duplicateUsername_throws() {
        // Given
        when(userRepository.existsByUsername("john.doe")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> userService.registerUser(buildRegisterRequest(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when email already exists")
    void registerUser_duplicateEmail_throws() {
        // Given
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> userService.registerUser(buildRegisterRequest(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // getters
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return user by ID when found")
    void getUserById_found_returnsUser() {
        // Given
        when(userRepository.findById("u-1")).thenReturn(Optional.of(buildUser("u-1", "john.doe", "john@example.com", "USER")));

        // When
        final UserResponseDTO result = userService.getUserById("u-1");

        // Then
        assertThat(result.id()).isEqualTo("u-1");
        assertThat(result.username()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("Should throw when user by ID is not found")
    void getUserById_notFound_throws() {
        // Given
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.getUserById("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with ID: missing");
    }

    @Test
    @DisplayName("Should return user by username when found")
    void getUserByUsername_found_returnsUser() {
        // Given
        when(userRepository.findByUsername("john.doe"))
                .thenReturn(Optional.of(buildUser("u-1", "john.doe", "john@example.com", "USER")));

        // When
        final UserResponseDTO result = userService.getUserByUsername("john.doe");

        // Then
        assertThat(result.username()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("Should throw when user by username is not found")
    void getUserByUsername_notFound_throws() {
        // Given
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.getUserByUsername("nobody"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: nobody");
    }

    @Test
    @DisplayName("Should return all users")
    void getAllUsers_returnsAll() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of(
                buildUser("u-1", "a", "a@example.com", "USER"),
                buildUser("u-2", "b", "b@example.com", "ADMIN")
        ));

        // When
        final List<UserResponseDTO> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserResponseDTO::username).containsExactly("a", "b");
    }

    @Test
    @DisplayName("Should return only enabled users")
    void getAllEnabledUsers_returnsEnabled() {
        // Given
        when(userRepository.findAllEnabledUsers()).thenReturn(List.of(
                buildUser("u-1", "a", "a@example.com", "USER")
        ));

        // When
        final List<UserResponseDTO> result = userService.getAllEnabledUsers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("a");
    }

    @Test
    @DisplayName("Should return users by valid role")
    void getUsersByRole_validRole_returnsUsers() {
        // Given
        when(userRepository.findByRole(UserRole.HR_MANAGER)).thenReturn(List.of(
                buildUser("u-1", "hr", "hr@example.com", "HR_MANAGER")
        ));

        // When
        final List<UserResponseDTO> result = userService.getUsersByRole("hr_manager");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo("HR_MANAGER");
    }

    @Test
    @DisplayName("Should throw when role is invalid")
    void getUsersByRole_invalidRole_throws() {
        assertThatThrownBy(() -> userService.getUsersByRole("NOPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role: NOPE");
    }

    @Test
    @DisplayName("Should search users applying wildcard filters")
    void searchUsers_withFilters_delegatesToRepository() {
        // Given
        final PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.searchUsers("%jo%", "%mail%", "USER", pageable))
                .thenReturn(new PageImpl<>(List.of(buildUser("u-1", "jo", "jo@mail.com", "USER")), pageable, 1));

        // When
        final Page<UserResponseDTO> result = userService.searchUsers("jo", "mail", "user", pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("jo");
    }

    @Test
    @DisplayName("Should throw when searching with an invalid role")
    void searchUsers_invalidRole_throws() {
        assertThatThrownBy(() -> userService.searchUsers(null, null, "BOGUS", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role: BOGUS");
    }

    // ------------------------------------------------------------------
    // updateUser
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should update user fields when provided")
    void updateUser_success_updatesFields() {
        // Given
        final SystemUser existing = buildUser("u-1", "john.doe", "old@example.com", "USER");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(SystemUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("NewPass1")).thenReturn("new-encoded");

        final UpdateUserRequestDTO request = UpdateUserRequestDTO.builder()
                .email("new@example.com")
                .password("NewPass1")
                .firstName("Jane")
                .role("ADMIN")
                .enabled(false)
                .build();

        // When
        final UserResponseDTO result = userService.updateUser("u-1", request);

        // Then
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.firstName()).isEqualTo("Jane");
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.enabled()).isFalse();
        verify(userRepository).save(existing);
        assertThat(existing.getPassword()).isEqualTo("new-encoded");
    }

    @Test
    @DisplayName("Should throw when updating a non-existent user")
    void updateUser_notFound_throws() {
        // Given
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.updateUser("missing", UpdateUserRequestDTO.builder().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with ID: missing");
    }

    @Test
    @DisplayName("Should throw when updating to an email already used by another user")
    void updateUser_duplicateEmail_throws() {
        // Given
        final SystemUser existing = buildUser("u-1", "john.doe", "old@example.com", "USER");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> userService.updateUser("u-1",
                UpdateUserRequestDTO.builder().email("taken@example.com").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should throw when updating to an invalid role")
    void updateUser_invalidRole_throws() {
        // Given
        final SystemUser existing = buildUser("u-1", "john.doe", "old@example.com", "USER");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(existing));

        // When / Then
        assertThatThrownBy(() -> userService.updateUser("u-1",
                UpdateUserRequestDTO.builder().role("BOSS").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role: BOSS");
    }

    // ------------------------------------------------------------------
    // deleteUser
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should delete an existing user")
    void deleteUser_existingUser_deletes() {
        // Given
        when(userRepository.existsById("u-1")).thenReturn(true);

        // When
        userService.deleteUser("u-1");

        // Then
        verify(userRepository).deleteById("u-1");
    }

    @Test
    @DisplayName("Should throw when deleting a non-existent user")
    void deleteUser_missingUser_throws() {
        // Given
        when(userRepository.existsById("missing")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> userService.deleteUser("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with ID: missing");
        verify(userRepository, never()).deleteById(anyString());
    }
}
