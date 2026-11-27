package com.gestorcontactos.gestor_contactos.service;

import com.gestorcontactos.gestor_contactos.dto.CreateContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateLegalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateNaturalContactRequest;
import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.Contact;
import com.gestorcontactos.gestor_contactos.entity.ContactEmail;
import com.gestorcontactos.gestor_contactos.entity.ContactPhone;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import com.gestorcontactos.gestor_contactos.repository.ContactRepository;
import com.gestorcontactos.gestor_contactos.validation.ContactValidator;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactValidator contactValidator;

    public ContactService(ContactRepository contactRepository, ContactValidator contactValidator) {
        this.contactRepository = contactRepository;
        this.contactValidator = contactValidator;
    }

    @Transactional
    public Contact createNaturalContact(CreateNaturalContactRequest request, User createdBy) {
        CreateNaturalContactRequest valid = contactValidator.normalizeAndValidate(request);
        validateCreatorAndPermissions(createdBy, valid.commercialTypes());
        ensureIdentityIsNew(valid.documentType(), valid.documentNumber());

        Contact contact = new Contact(PersonType.NATURAL, valid.firstNames(), valid.lastNames(),
                valid.documentType(), valid.documentNumber(), valid.address(), LocalDateTime.now(), createdBy);
        addChildrenAndCommercialTypes(contact, valid.phones(), valid.emails(), valid.commercialTypes());
        return save(contact);
    }

    @Transactional
    public Contact createLegalContact(CreateLegalContactRequest request, User createdBy) {
        CreateLegalContactRequest valid = contactValidator.normalizeAndValidate(request);
        validateCreatorAndPermissions(createdBy, valid.commercialTypes());
        ensureIdentityIsNew(valid.documentType(), valid.documentNumber());

        Contact contact = new Contact(PersonType.JURIDICA, valid.legalName(), valid.documentType(),
                valid.documentNumber(), valid.address(), LocalDateTime.now(), createdBy);
        addChildrenAndCommercialTypes(contact, valid.phones(), valid.emails(), valid.commercialTypes());
        return save(contact);
    }

    @Transactional
    public Contact addCommercialType(Long contactId, CommercialType commercialType, User actor) {
        validatePermissionToAddCommercialType(actor, commercialType);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactBusinessException("No se encontró el contacto indicado."));
        if (contact.getDeletedAt() != null) {
            throw new ContactBusinessException("No se puede agregar una relación comercial a un contacto eliminado.");
        }
        if (contact.getCommercialTypes().contains(commercialType)) {
            throw new ContactBusinessException("El contacto ya tiene asignada esa relación comercial.");
        }

        contact.addCommercialType(commercialType);
        contact.setUpdatedBy(actor);
        contact.setUpdatedAt(LocalDateTime.now());
        try {
            return contactRepository.saveAndFlush(contact);
        } catch (DataIntegrityViolationException exception) {
            throw new ContactBusinessException(
                    "No se pudo agregar la relación comercial por un conflicto con los datos registrados.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Contact> findById(Long id) {
        return contactRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Contact> findByIdentity(DocumentType documentType, String documentNumber) {
        return contactRepository.findByDocumentTypeAndDocumentNumber(documentType, documentNumber);
    }

    @Transactional(readOnly = true)
    public List<Contact> findAllNotDeleted() {
        return contactRepository.findAllByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public List<Contact> findAllNotDeletedByCommercialType(CommercialType commercialType) {
        return contactRepository.findAllNotDeletedByCommercialType(commercialType);
    }

    private void ensureIdentityIsNew(DocumentType documentType, String documentNumber) {
        if (contactRepository.findByDocumentTypeAndDocumentNumber(documentType, documentNumber).isPresent()) {
            throw new ContactBusinessException("Ya existe un contacto con el documento indicado.");
        }
    }

    private void validatePermissionToAddCommercialType(User actor, CommercialType commercialType) {
        if (actor == null || actor.getRole() == null || !actor.isEnabled()) {
            throw new ContactBusinessException("Se requiere un usuario autenticado para modificar el contacto.");
        }
        if (commercialType == null) {
            throw new ContactBusinessException("Debe indicar la relación comercial que desea agregar.");
        }
        boolean allowed = switch (actor.getRole()) {
            case ADMINISTRADOR -> true;
            case VENDEDOR -> commercialType == CommercialType.CLIENTE;
            case COMPRAS -> commercialType == CommercialType.PROVEEDOR;
            case ALMACENERO -> false;
        };
        if (!allowed) {
            throw new ContactBusinessException("No tiene permisos para agregar la relación comercial indicada.");
        }
    }

    private void validateCreatorAndPermissions(User createdBy, java.util.Set<CommercialType> commercialTypes) {
        if (createdBy == null || createdBy.getRole() == null || !createdBy.isEnabled()) {
            throw new ContactBusinessException("Se requiere un usuario autenticado para crear el contacto.");
        }
        boolean allowed = switch (createdBy.getRole()) {
            case ADMINISTRADOR -> true;
            case VENDEDOR -> commercialTypes.stream().allMatch(type -> type == CommercialType.CLIENTE);
            case COMPRAS -> commercialTypes.stream().allMatch(type -> type == CommercialType.PROVEEDOR);
            case ALMACENERO -> false;
        };
        if (!allowed) {
            throw new ContactBusinessException("No tiene permisos para asignar los tipos comerciales indicados.");
        }
    }

    private void addChildrenAndCommercialTypes(Contact contact, List<CreateContactPhoneRequest> phones,
            List<CreateContactEmailRequest> emails, java.util.Set<CommercialType> commercialTypes) {
        for (CreateContactPhoneRequest phone : phones) {
            String areaCode = phone.areaCode() == null ? "" : phone.areaCode();
            contact.addPhone(new ContactPhone(phone.phoneType(), areaCode, phone.phoneNumber()));
        }
        for (CreateContactEmailRequest email : emails) {
            contact.addEmail(new ContactEmail(email.email().toLowerCase(Locale.ROOT)));
        }
        commercialTypes.forEach(contact::addCommercialType);
    }

    private Contact save(Contact contact) {
        try {
            return contactRepository.saveAndFlush(contact);
        } catch (DataIntegrityViolationException exception) {
            throw new ContactBusinessException(
                    "No se pudo crear el contacto porque existe un conflicto con los datos registrados.");
        }
    }
}
