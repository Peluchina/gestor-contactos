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
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = {LoginController.class, DashboardController.class})
@Import(SecurityConfig.class)
class SecurityAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

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

        mockMvc.perform(get("/usuarios").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                        details("admin", "hash", Role.ADMINISTRADOR, true))))
                .andExpect(status().isNotFound());
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
    void passwordEncoderStoresOnlyBcryptHash() {
        String rawPassword = "safe-password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User("hashed-user", encodedPassword, Role.VENDEDOR, true, LocalDateTime.now());

        org.junit.jupiter.api.Assertions.assertNotEquals(rawPassword, user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(encodedPassword.startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    private CustomUserDetails details(String username, String rawPassword, Role role, boolean enabled) {
        return new CustomUserDetails(new User(username, passwordEncoder.encode(rawPassword), role, enabled,
                LocalDateTime.now()));
    }
}
