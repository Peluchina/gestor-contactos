package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.Role;
import java.time.LocalDateTime;

public record UserListItem(
        Long id,
        String username,
        Role role,
        boolean enabled,
        LocalDateTime createdAt,
        String createdByUsername,
        LocalDateTime updatedAt,
        String updatedByUsername) {
}
