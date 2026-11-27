package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import java.util.List;
import java.util.Set;

public record CreateLegalContactRequest(
        String legalName,
        DocumentType documentType,
        String documentNumber,
        String address,
        List<CreateContactPhoneRequest> phones,
        List<CreateContactEmailRequest> emails,
        Set<CommercialType> commercialTypes) {
}
