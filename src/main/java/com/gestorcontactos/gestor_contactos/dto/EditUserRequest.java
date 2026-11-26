package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.Role;
import jakarta.validation.constraints.NotNull;

public class EditUserRequest {

    @NotNull(message = "Selecciona un rol válido.")
    private Role role;

    public EditUserRequest() {
    }

    public EditUserRequest(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
