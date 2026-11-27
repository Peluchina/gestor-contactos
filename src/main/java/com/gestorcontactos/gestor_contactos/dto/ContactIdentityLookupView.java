package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.ContactStatus;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import java.util.Set;

public record ContactIdentityLookupView(Long id, PersonType personType, String displayName,
        DocumentType documentType, String documentNumber, ContactStatus status,
        Set<CommercialType> commercialTypes) {
}
