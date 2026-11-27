package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.PhoneType;

public record ContactPhoneRequest(PhoneType phoneType, String areaCode, String phoneNumber) {
}
