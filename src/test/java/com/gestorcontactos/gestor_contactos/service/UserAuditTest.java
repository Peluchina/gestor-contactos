package com.gestorcontactos.gestor_contactos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserAuditTest {

    @Test
    void createAndUpdateOperationsKeepTheirAuthenticatedAuditUsers() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 26, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 11, 26, 11, 0);
        User administrator = new User("admin", "hash", Role.ADMINISTRADOR, true, createdAt);
        User user = new User("seller", "hash", Role.VENDEDOR, true, createdAt, administrator);

        assertEquals(administrator, user.getCreatedBy());

        user.changeRole(Role.COMPRAS, administrator, updatedAt);

        assertEquals(Role.COMPRAS, user.getRole());
        assertEquals(administrator, user.getUpdatedBy());
        assertEquals(updatedAt, user.getUpdatedAt());

        user.changeEnabled(false, administrator, updatedAt.plusMinutes(1));
        assertEquals(administrator, user.getUpdatedBy());
        assertEquals(updatedAt.plusMinutes(1), user.getUpdatedAt());

        user.resetPassword("new-hash", administrator, updatedAt.plusMinutes(2));
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals(administrator, user.getUpdatedBy());
        assertEquals(updatedAt.plusMinutes(2), user.getUpdatedAt());
    }
}
