package com.gestorcontactos.gestor_contactos.validation;

import com.gestorcontactos.gestor_contactos.dto.CreateContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateLegalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateNaturalContactRequest;
import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PhoneType;
import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ContactValidator {

    private static final Pattern PERSON_NAME = Pattern.compile("(?U)^[\\p{L}]+(?:\\s+[\\p{L}]+)*$");
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile("^[0-9]+$");
    private static final Pattern LEGAL_NAME = Pattern.compile("(?U)^[\\p{L}\\p{N}\\s.,&'’()/\\-]+$");
    private static final Pattern ADDRESS = Pattern.compile("(?U)^[\\p{L}\\p{N}\\s#/,.-]+$");
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                    + "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    public CreateNaturalContactRequest normalizeAndValidate(CreateNaturalContactRequest request) {
        if (request == null) {
            throw new ContactBusinessException("Los datos del contacto son obligatorios.");
        }
        CreateNaturalContactRequest normalized = new CreateNaturalContactRequest(
                normalizeSpaces(request.firstNames()),
                normalizeSpaces(request.lastNames()),
                request.documentType(),
                request.documentNumber(),
                normalizeSpaces(request.address()),
                request.phones(),
                request.emails(),
                request.commercialTypes());

        validateName(normalized.firstNames(), "Los nombres");
        validateName(normalized.lastNames(), "Los apellidos");
        if (normalized.documentType() != DocumentType.DNI
                && normalized.documentType() != DocumentType.CARNET_EXTRANJERIA) {
            throw new ContactBusinessException("Una persona natural debe tener DNI o Carnet de Extranjería.");
        }
        validateDocument(normalized.documentType(), normalized.documentNumber());
        validateAddress(normalized.address());
        validateCollections(normalized.phones(), normalized.emails(), normalized.commercialTypes());
        return normalized;
    }

    public CreateLegalContactRequest normalizeAndValidate(CreateLegalContactRequest request) {
        if (request == null) {
            throw new ContactBusinessException("Los datos del contacto son obligatorios.");
        }
        CreateLegalContactRequest normalized = new CreateLegalContactRequest(
                request.legalName(),
                request.documentType(),
                request.documentNumber(),
                normalizeSpaces(request.address()),
                request.phones(),
                request.emails(),
                request.commercialTypes());

        validateLegalName(normalized.legalName());
        if (normalized.documentType() != DocumentType.RUC) {
            throw new ContactBusinessException("Una persona jurídica debe tener RUC.");
        }
        validateDocument(normalized.documentType(), normalized.documentNumber());
        validateAddress(normalized.address());
        validateCollections(normalized.phones(), normalized.emails(), normalized.commercialTypes());
        return normalized;
    }

    private void validateName(String name, String field) {
        if (name == null || name.isBlank() || name.length() > 100 || !PERSON_NAME.matcher(name).matches()) {
            throw new ContactBusinessException(field + " deben contener únicamente letras y espacios.");
        }
    }

    private void validateLegalName(String legalName) {
        if (legalName == null || legalName.isBlank() || legalName.length() < 2 || legalName.length() > 200
                || !LEGAL_NAME.matcher(legalName).matches()) {
            throw new ContactBusinessException("La razón social debe tener entre 2 y 200 caracteres válidos.");
        }
    }

    private void validateDocument(DocumentType documentType, String documentNumber) {
        if (documentNumber == null || !DOCUMENT_NUMBER.matcher(documentNumber).matches()) {
            throw new ContactBusinessException(documentMessage(documentType));
        }
        int expectedLength = documentType == DocumentType.DNI ? 8 : 11;
        if (documentNumber.length() != expectedLength) {
            throw new ContactBusinessException(documentMessage(documentType));
        }
    }

    private String documentMessage(DocumentType documentType) {
        if (documentType == DocumentType.DNI) {
            return "El DNI debe contener exactamente 8 dígitos.";
        }
        if (documentType == DocumentType.CARNET_EXTRANJERIA) {
            return "El Carnet de Extranjería debe contener exactamente 11 dígitos.";
        }
        return "El RUC debe contener exactamente 11 dígitos.";
    }

    private void validateAddress(String address) {
        if (address == null || address.length() < 5 || address.length() > 200 || !ADDRESS.matcher(address).matches()) {
            throw new ContactBusinessException("La dirección debe tener entre 5 y 200 caracteres válidos.");
        }
    }

    private void validateCollections(List<CreateContactPhoneRequest> phones,
            List<CreateContactEmailRequest> emails, Set<CommercialType> commercialTypes) {
        validatePhones(phones);
        validateEmails(emails);
        if (commercialTypes == null || commercialTypes.isEmpty() || commercialTypes.stream().anyMatch(Objects::isNull)) {
            throw new ContactBusinessException("Debe asignar al menos un tipo comercial.");
        }
    }

    private void validatePhones(List<CreateContactPhoneRequest> phones) {
        if (phones == null || phones.isEmpty()) {
            throw new ContactBusinessException("Debe registrar al menos un teléfono.");
        }
        Set<String> uniquePhones = new HashSet<>();
        for (CreateContactPhoneRequest phone : phones) {
            if (phone == null || phone.phoneType() == null || phone.phoneNumber() == null) {
                throw new ContactBusinessException("Los datos del teléfono son obligatorios.");
            }
            String areaCode = phone.areaCode() == null ? "" : phone.areaCode();
            if (phone.phoneType() == PhoneType.CELULAR) {
                if (!areaCode.isEmpty() || !phone.phoneNumber().matches("9[0-9]{8}")) {
                    throw new ContactBusinessException("El celular debe contener 9 dígitos y comenzar con 9.");
                }
            } else if (phone.phoneType() == PhoneType.FIJO) {
                if (!areaCode.matches("[0-9]{1,5}") || !phone.phoneNumber().matches("[0-9]{1,9}")) {
                    throw new ContactBusinessException(
                            "El teléfono fijo debe separar un código de área y un número local válidos.");
                }
            }
            String key = phone.phoneType() + "|" + areaCode + "|" + phone.phoneNumber();
            if (!uniquePhones.add(key)) {
                throw new ContactBusinessException("No se permiten teléfonos duplicados en el contacto.");
            }
        }
    }

    private void validateEmails(List<CreateContactEmailRequest> emails) {
        if (emails == null || emails.isEmpty()) {
            throw new ContactBusinessException("Debe registrar al menos un correo electrónico.");
        }
        Set<String> uniqueEmails = new HashSet<>();
        for (CreateContactEmailRequest emailRequest : emails) {
            if (emailRequest == null || emailRequest.email() == null) {
                throw new ContactBusinessException("El correo electrónico es obligatorio.");
            }
            String email = emailRequest.email();
            if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
                throw new ContactBusinessException("El correo electrónico no tiene un formato válido.");
            }
            if (!uniqueEmails.add(email.toLowerCase(Locale.ROOT))) {
                throw new ContactBusinessException("No se permiten correos electrónicos duplicados en el contacto.");
            }
        }
    }

    private String normalizeSpaces(String value) {
        return value == null ? null : value.strip().replaceAll("(?U)\\s+", " ");
    }
}
