package com.gestorcontactos.gestor_contactos.dto;

import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequest {

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
