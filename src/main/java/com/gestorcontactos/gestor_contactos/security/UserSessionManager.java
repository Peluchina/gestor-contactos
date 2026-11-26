package com.gestorcontactos.gestor_contactos.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class UserSessionManager {

    private final SessionRegistry sessionRegistry;

    public UserSessionManager(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void expireSessions(Long userId) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(CustomUserDetails.class::isInstance)
                .map(CustomUserDetails.class::cast)
                .filter(userDetails -> Objects.equals(userDetails.getId(), userId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }
}
