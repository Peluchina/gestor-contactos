package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.PhoneType;

public record CreateContactPhoneRequest(PhoneType phoneType, String areaCode, String phoneNumber) {
}
