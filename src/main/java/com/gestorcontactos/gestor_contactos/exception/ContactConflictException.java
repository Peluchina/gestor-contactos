package com.gestorcontactos.gestor_contactos.exception;

public class ContactConflictException extends RuntimeException {

    public ContactConflictException(String message) {
        super(message);
    }
}
