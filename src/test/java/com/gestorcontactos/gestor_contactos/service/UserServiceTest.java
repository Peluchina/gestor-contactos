package com.gestorcontactos.gestor_contactos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestorcontactos.gestor_contactos.dto.CreateUserRequest;
import com.gestorcontactos.gestor_contactos.dto.EditUserRequest;
import com.gestorcontactos.gestor_contactos.dto.ResetPasswordRequest;
import com.gestorcontactos.gestor_contactos.dto.UserListItem;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.UserManagementException;
import com.gestorcontactos.gestor_contactos.exception.UserConflictException;
import com.gestorcontactos.gestor_contactos.repository.UserRepository;
import com.gestorcontactos.gestor_contactos.security.UserSessionManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ADMIN_ID = 10L;
    private static final Long TARGET_ID = 20L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-26T17:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionManager userSessionManager;

    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User administrator;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder, userSessionManager,
                Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC")));
        administrator = user(ADMIN_ID, "admin", Role.ADMINISTRADOR, true);
        lenient().when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrator));
    }

    @Test
    void createsUserWithBcryptAndCreationAudit() {
        CreateUserRequest request = createRequest("new.user", "initial-password", Role.COMPRAS);
        when(userRepository.existsByUsernameIgnoreCase("new.user")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserListItem result = userService.createUser(request, ADMIN_ID);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        User created = savedUser.getValue();
        assertEquals("new.user", created.getUsername());
        assertEquals(Role.COMPRAS, created.getRole());
        assertTrue(created.isEnabled());
        assertNotEquals("initial-password", created.getPasswordHash());
        assertTrue(passwordEncoder.matches("initial-password", created.getPasswordHash()));
        assertEquals(administrator, created.getCreatedBy());
        assertEquals(LocalDateTime.of(2026, 11, 26, 12, 0), created.getCreatedAt());
        assertEquals("new.user", result.username());
    }

    @Test
    void rejectsUsernameAlreadyUsedWithoutSaving() {
        when(userRepository.existsByUsernameIgnoreCase("ADMIN")).thenReturn(true);

        UserConflictException exception = assertThrows(UserConflictException.class,
                () -> userService.createUser(createRequest("ADMIN", "password", Role.VENDEDOR), ADMIN_ID));

        assertEquals("Ese username ya está registrado.", exception.getMessage());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUsernameWithWhitespace() {
        assertInvalidUsername("new user", "El username no puede contener espacios.");
    }

    @Test
    void rejectsUsernameShorterThanFourCharacters() {
        assertInvalidUsername("abc", "El username debe tener entre 4 y 50 caracteres.");
    }

    @Test
    void rejectsUsernameLongerThanFiftyCharacters() {
        assertInvalidUsername("a".repeat(51), "El username debe tener entre 4 y 50 caracteres.");
    }

    @Test
    void changesRoleAndRecordsUpdateAuditThenExpiresSessions() {
        User target = user(TARGET_ID, "seller", Role.VENDEDOR, true);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        userService.changeRole(TARGET_ID, new EditUserRequest(Role.COMPRAS), ADMIN_ID);

        verify(target).changeRole(Role.COMPRAS, administrator, LocalDateTime.of(2026, 11, 26, 12, 0));
        verify(userRepository).save(target);
        verify(userSessionManager).expireSessions(TARGET_ID);
    }

    @Test
    void administratorCannotRemoveOwnAdministratorRole() {
        assertThrows(UserManagementException.class,
                () -> userService.changeRole(ADMIN_ID, new EditUserRequest(Role.VENDEDOR), ADMIN_ID));
        verify(userRepository, never()).save(administrator);
    }

    @Test
    void activatesAndRecordsUpdateAudit() {
        User target = user(TARGET_ID, "inactive", Role.VENDEDOR, false);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        userService.changeEnabled(TARGET_ID, true, ADMIN_ID);

        verify(target).changeEnabled(true, administrator, LocalDateTime.of(2026, 11, 26, 12, 0));
        verify(userRepository).save(target);
    }

    @Test
    void deactivatesUserAndExpiresItsSessions() {
        User target = user(TARGET_ID, "seller", Role.VENDEDOR, true);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        userService.changeEnabled(TARGET_ID, false, ADMIN_ID);

        verify(target).changeEnabled(false, administrator, LocalDateTime.of(2026, 11, 26, 12, 0));
        verify(userRepository).save(target);
        verify(userSessionManager).expireSessions(TARGET_ID);
    }

    @Test
    void administratorCannotDeactivateThemselves() {
        assertThrows(UserManagementException.class,
                () -> userService.changeEnabled(ADMIN_ID, false, ADMIN_ID));
        verify(userRepository, never()).save(administrator);
    }

    @Test
    void resetsAnotherUsersPasswordWithBcryptAndUpdateAudit() {
        User target = user(TARGET_ID, "seller", Role.VENDEDOR, true);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("new-password");

        userService.resetPassword(TARGET_ID, request, ADMIN_ID);

        ArgumentCaptor<String> passwordHash = ArgumentCaptor.forClass(String.class);
        verify(target).resetPassword(passwordHash.capture(), org.mockito.ArgumentMatchers.eq(administrator),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 11, 26, 12, 0)));
        assertTrue(passwordEncoder.matches("new-password", passwordHash.getValue()));
        verify(userRepository).save(target);
        verify(userSessionManager).expireSessions(TARGET_ID);
    }

    @Test
    void resetPasswordMustTargetAnotherUser() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("new-password");

        assertThrows(UserManagementException.class, () -> userService.resetPassword(ADMIN_ID, request, ADMIN_ID));
        verify(userRepository, never()).save(administrator);
    }

    @Test
    void rejectsNonAdministratorEvenWhenServiceIsCalledDirectly() {
        User seller = user(ADMIN_ID, "seller", Role.VENDEDOR, true);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(seller));

        assertThrows(UserManagementException.class, () -> userService.listUsers(ADMIN_ID));
    }

    @Test
    void bootstrapCreatesOnlyFirstAdministratorFromConfiguredCredentials() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(userService.initializeFirstAdministrator("first.admin", "configured-password"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        assertEquals(Role.ADMINISTRADOR, savedUser.getValue().getRole());
        assertTrue(passwordEncoder.matches("configured-password", savedUser.getValue().getPasswordHash()));
        assertNotNull(savedUser.getValue().getCreatedAt());
        assertTrue(savedUser.getValue().isEnabled());
    }

    private void assertInvalidUsername(String username, String expectedMessage) {
        UserManagementException exception = assertThrows(UserManagementException.class,
                () -> userService.createUser(createRequest(username, "password", Role.VENDEDOR), ADMIN_ID));
        assertEquals(expectedMessage, exception.getMessage());
        verify(userRepository, never()).saveAndFlush(any());
    }

    private static CreateUserRequest createRequest(String username, String password, Role role) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role);
        return request;
    }

    private static User user(Long id, String username, Role role, boolean enabled) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getUsername()).thenReturn(username);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(user.isEnabled()).thenReturn(enabled);
        return user;
    }
}
