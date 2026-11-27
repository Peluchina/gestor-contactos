package com.gestorcontactos.gestor_contactos.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.gestorcontactos.gestor_contactos.config.SecurityConfig;
import com.gestorcontactos.gestor_contactos.controller.DashboardController;
import com.gestorcontactos.gestor_contactos.controller.LoginController;
import com.gestorcontactos.gestor_contactos.controller.UserAdminController;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.UserNotFoundException;
import com.gestorcontactos.gestor_contactos.exception.UserConflictException;
import com.gestorcontactos.gestor_contactos.service.UserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.List;

@WebMvcTest(controllers = {LoginController.class, DashboardController.class, UserAdminController.class})
@Import(SecurityConfig.class)
class SecurityAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Test
    void validCredentialsCreateSessionAndOpenProtectedPage() throws Exception {
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(details("admin", "correct-password", Role.ADMINISTRADOR, true));

        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        mockMvc.perform(get("/dashboard").session((org.springframework.mock.web.MockHttpSession)
                        result.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void incorrectPasswordUsesGenericFailurePage() throws Exception {
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(details("admin", "correct-password", Role.ADMINISTRADOR, true));

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usuario o contraseña incorrectos")));
    }

    @Test
    void nonexistentUserUsesSameGenericFailurePage() throws Exception {
        when(userDetailsService.loadUserByUsername("missing"))
                .thenThrow(new UsernameNotFoundException("Usuario o contraseña incorrectos"));

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "missing")
                        .param("password", "any-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        when(userDetailsService.loadUserByUsername("disabled"))
                .thenReturn(details("disabled", "correct-password", Role.VENDEDOR, false));

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "disabled")
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unauthenticatedRequestToProtectedPageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void onlyAdministratorCanAccessAdministrativeRoutes() throws Exception {
        mockMvc.perform(get("/usuarios").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        details("seller", "hash", Role.VENDEDOR, true))))
                .andExpect(status().isForbidden());

        when(userService.listUsers(null)).thenReturn(List.of());
        mockMvc.perform(get("/usuarios").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        details("admin", "hash", Role.ADMINISTRADOR, true))))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"));
    }

    @Test
    void missingAdministrativeUserReturns404() throws Exception {
        when(userService.getUser(999L, null)).thenThrow(new UserNotFoundException("internal user detail"));

        mockMvc.perform(get("/usuarios/999/editar")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(details("admin", "hash", Role.ADMINISTRADOR, true))))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void duplicateUsernameConflictReturns409() throws Exception {
        when(userService.createUser(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new UserConflictException("internal duplicate details"));

        mockMvc.perform(post("/usuarios").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(details("admin", "hash", Role.ADMINISTRADOR, true)))
                        .param("username", "existing-user")
                        .param("password", "safe-password")
                        .param("role", "VENDEDOR"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"));
    }

    @Test
    void administratorCanOpenUserCreationFormWithoutEchoingPasswordFields() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        details("admin", "hash", Role.ADMINISTRADOR, true))))
                .andExpect(status().isOk())
                .andExpect(view().name("users/create"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password_hash"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"password\"")));
    }

    @Test
    void csrfIsRequiredAndLogoutInvalidatesAuthentication() throws Exception {
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(details("admin", "correct-password", Role.ADMINISTRADOR, true));
        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "correct-password"))
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/logout").session(loginSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/logout").with(csrf()).session(loginSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void expiredRegisteredSessionCannotContinueUsingProtectedPages() throws Exception {
        when(userDetailsService.loadUserByUsername("session-owner"))
                .thenReturn(details("session-owner", "correct-password", Role.VENDEDOR, true, 901L));
        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "session-owner")
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession userSession = (MockHttpSession) login.getRequest().getSession(false);

        new UserSessionManager(sessionRegistry).expireSessions(901L);

        mockMvc.perform(get("/dashboard").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void passwordEncoderStoresOnlyBcryptHash() {
        String rawPassword = "safe-password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User("hashed-user", encodedPassword, Role.VENDEDOR, true, LocalDateTime.now());

        org.junit.jupiter.api.Assertions.assertNotEquals(rawPassword, user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(encodedPassword.startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    private CustomUserDetails details(String username, String rawPassword, Role role, boolean enabled) {
        return details(username, rawPassword, role, enabled, null);
    }

    private CustomUserDetails details(String username, String rawPassword, Role role, boolean enabled, Long id) {
        return new CustomUserDetails(new User(username, passwordEncoder.encode(rawPassword), role, enabled,
                LocalDateTime.now())) {
            @Override
            public Long getId() {
                return id;
            }
        };
    }
}
