package com.gestorcontactos.gestor_contactos.service;

import com.gestorcontactos.gestor_contactos.dto.CreateUserRequest;
import com.gestorcontactos.gestor_contactos.dto.EditUserRequest;
import com.gestorcontactos.gestor_contactos.dto.ResetPasswordRequest;
import com.gestorcontactos.gestor_contactos.dto.UserListItem;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.UserConflictException;
import com.gestorcontactos.gestor_contactos.exception.UserManagementException;
import com.gestorcontactos.gestor_contactos.exception.UserNotFoundException;
import com.gestorcontactos.gestor_contactos.repository.UserRepository;
import com.gestorcontactos.gestor_contactos.security.UserSessionManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String USERNAME_REQUIRED = "El username es obligatorio.";
    private static final String USERNAME_LENGTH = "El username debe tener entre 4 y 50 caracteres.";
    private static final String USERNAME_SPACES = "El username no puede contener espacios.";
    private static final String USERNAME_DUPLICATE = "Ese username ya está registrado.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionManager userSessionManager;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            UserSessionManager userSessionManager, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSessionManager = userSessionManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    @Transactional(readOnly = true)
    public List<UserListItem> listUsers(Long actorId) {
        requireAdministrator(actorId);
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public UserListItem createUser(CreateUserRequest request, Long actorId) {
        User actor = requireAdministrator(actorId);
        validateUsername(request.getUsername());
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new UserManagementException("La contraseña es obligatoria.");
        }
        if (request.getRole() == null) {
            throw new UserManagementException("Selecciona un rol válido.");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new UserConflictException(USERNAME_DUPLICATE);
        }

        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()), request.getRole(),
                true, now(), actor);
        try {
            return toListItem(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new UserConflictException(USERNAME_DUPLICATE);
        }
    }

    @Transactional(readOnly = true)
    public UserListItem getUser(Long userId, Long actorId) {
        requireAdministrator(actorId);
        return toListItem(findUser(userId));
    }

    @Transactional
    public void changeRole(Long userId, EditUserRequest request, Long actorId) {
        User actor = requireAdministrator(actorId);
        if (request.getRole() == null) {
            throw new UserManagementException("Selecciona un rol válido.");
        }
        User target = findUser(userId);
        if (actor.getId().equals(target.getId()) && request.getRole() != Role.ADMINISTRADOR) {
            throw new UserManagementException("No puedes quitarte tu propio rol de administrador.");
        }
        if (target.getRole() != request.getRole()) {
            target.changeRole(request.getRole(), actor, now());
            userRepository.save(target);
            userSessionManager.expireSessions(target.getId());
        }
    }

    @Transactional
    public void changeEnabled(Long userId, boolean enabled, Long actorId) {
        User actor = requireAdministrator(actorId);
        User target = findUser(userId);
        if (!enabled && actor.getId().equals(target.getId())) {
            throw new UserManagementException("No puedes desactivar tu propia cuenta.");
        }
        if (target.isEnabled() != enabled) {
            target.changeEnabled(enabled, actor, now());
            userRepository.save(target);
            if (!enabled) {
                userSessionManager.expireSessions(target.getId());
            }
        }
    }

    @Transactional
    public void resetPassword(Long userId, ResetPasswordRequest request, Long actorId) {
        User actor = requireAdministrator(actorId);
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new UserManagementException("La nueva contraseña es obligatoria.");
        }
        User target = findUser(userId);
        if (actor.getId().equals(target.getId())) {
            throw new UserManagementException("El restablecimiento debe realizarse para otro usuario.");
        }
        target.resetPassword(passwordEncoder.encode(request.getPassword()), actor, now());
        userRepository.save(target);
        userSessionManager.expireSessions(target.getId());
    }

    @Transactional
    public boolean initializeFirstAdministrator(String username, String password) {
        if (userRepository.count() > 0) {
            return false;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("No hay usuarios registrados. Configura INITIAL_ADMIN_USERNAME e "
                    + "INITIAL_ADMIN_PASSWORD para crear el primer administrador.");
        }
        validateUsername(username);
        User administrator = new User(username, passwordEncoder.encode(password), Role.ADMINISTRADOR, true, now());
        userRepository.saveAndFlush(administrator);
        return true;
    }

    private User requireAdministrator(Long actorId) {
        User actor = findUser(actorId);
        if (!actor.isEnabled() || actor.getRole() != Role.ADMINISTRADOR) {
            throw new UserManagementException("No tienes permiso para administrar usuarios.");
        }
        return actor;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("No se encontró el usuario solicitado."));
    }

    private UserListItem toListItem(User user) {
        return new UserListItem(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getCreatedBy() == null ? null : user.getCreatedBy().getUsername(),
                user.getUpdatedAt(),
                user.getUpdatedBy() == null ? null : user.getUpdatedBy().getUsername());
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UserManagementException(USERNAME_REQUIRED);
        }
        if (username.length() < 4 || username.length() > 50) {
            throw new UserManagementException(USERNAME_LENGTH);
        }
        if (username.codePoints().anyMatch(codePoint ->
                Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint))) {
            throw new UserManagementException(USERNAME_SPACES);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(ZoneId.of("America/Lima")));
    }
}
