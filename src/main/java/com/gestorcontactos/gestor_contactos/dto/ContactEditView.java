package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.ContactStatus;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import java.util.List;
import java.util.Set;

public record ContactEditView(Long id, PersonType personType, String firstNames, String lastNames,
        String legalName, DocumentType documentType, String documentNumber, String address,
        ContactStatus status, Set<CommercialType> commercialTypes,
        List<ContactPhoneEditView> phones, List<ContactEmailEditView> emails) {
}
