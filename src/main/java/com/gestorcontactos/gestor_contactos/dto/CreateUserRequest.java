package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "El username es obligatorio.")
    @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres.")
    @Pattern(regexp = "(?U)^[^\\s\\p{Z}]+$", message = "El username no puede contener espacios.")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    @NotNull(message = "Selecciona un rol válido.")
    private Role role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
