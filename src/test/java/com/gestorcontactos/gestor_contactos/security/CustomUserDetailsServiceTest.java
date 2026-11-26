package com.gestorcontactos.gestor_contactos.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.service.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadsRoleAndEnabledStatusFromUser() {
        User user = new User("seller", "bcrypt-hash", Role.VENDEDOR, false, LocalDateTime.now());
        when(userService.findByUsername("seller")).thenReturn(Optional.of(user));

        CustomUserDetails details = (CustomUserDetails) userDetailsService.loadUserByUsername("seller");

        assertEquals("seller", details.getUsername());
        assertEquals("bcrypt-hash", details.getPassword());
        assertEquals("ROLE_VENDEDOR", details.getAuthorities().iterator().next().getAuthority());
        org.junit.jupiter.api.Assertions.assertFalse(details.isEnabled());
        verify(userService).findByUsername("seller");
    }

    @Test
    void missingUserProducesGenericInternalMessage() {
        when(userService.findByUsername("missing")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing"));

        assertEquals("Usuario o contraseña incorrectos", exception.getMessage());
    }
}
