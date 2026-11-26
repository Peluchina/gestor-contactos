package com.gestorcontactos.gestor_contactos.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

class UserSessionManagerTest {

    @Test
    void expiresSessionsForMatchingUserId() {
        User user = new User("seller", "hash", Role.VENDEDOR, true, LocalDateTime.now());
        CustomUserDetails principal = new CustomUserDetails(user) {
            @Override
            public Long getId() {
                return 42L;
            }
        };
        SessionInformation session = mock(SessionInformation.class);
        SessionRegistry registry = mock(SessionRegistry.class);
        when(registry.getAllPrincipals()).thenReturn(List.of(principal));
        when(registry.getAllSessions(principal, false)).thenReturn(List.of(session));

        new UserSessionManager(registry).expireSessions(42L);

        verify(session).expireNow();
    }
}
