package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.ContactStatus;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import java.util.List;
import java.util.Set;

public record ContactDetailsView(Long id, PersonType personType, String displayName,
        DocumentType documentType, String documentNumber, String address,
        ContactStatus status, Set<CommercialType> commercialTypes,
        List<ContactPhoneView> phones, List<String> emails) {
}
