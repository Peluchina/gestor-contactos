package com.gestorcontactos.gestor_contactos.service;

import com.gestorcontactos.gestor_contactos.dto.CreateContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateLegalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateNaturalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.ContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.ContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.EditContactRequest;
import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.Contact;
import com.gestorcontactos.gestor_contactos.entity.ContactEmail;
import com.gestorcontactos.gestor_contactos.entity.ContactPhone;
import com.gestorcontactos.gestor_contactos.entity.ContactStatus;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import com.gestorcontactos.gestor_contactos.exception.ContactConflictException;
import com.gestorcontactos.gestor_contactos.exception.ContactNotFoundException;
import com.gestorcontactos.gestor_contactos.repository.ContactRepository;
import com.gestorcontactos.gestor_contactos.validation.ContactValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private static final ZoneId AUDIT_ZONE = ZoneId.of("America/Lima");

    private final ContactRepository contactRepository;
    private final ContactValidator contactValidator;
    private final ContactPhotoStorageService photoStorageService;
    private final Clock clock;

    public ContactService(ContactRepository contactRepository, ContactValidator contactValidator,
            ContactPhotoStorageService photoStorageService, Clock clock) {
        this.contactRepository = contactRepository;
        this.contactValidator = contactValidator;
        this.photoStorageService = photoStorageService;
        this.clock = clock;
    }

    @Transactional
    public Contact createNaturalContact(CreateNaturalContactRequest request, User createdBy) {
        return createNaturalContact(request, createdBy, null);
    }

    @Transactional
    public Contact createNaturalContact(CreateNaturalContactRequest request, User createdBy, MultipartFile photo) {
        String photoPath = isPhotoNotProvided(photo) ? null : photoStorageService.store(photo);
        if (photoPath != null) {
            scheduleDeleteOnRollback(photoPath);
        }
        try {
            CreateNaturalContactRequest valid = contactValidator.normalizeAndValidate(request);
            validateCreatorAndPermissions(createdBy, valid.commercialTypes());
            ensureIdentityIsNew(valid.documentType(), valid.documentNumber());

            Contact contact = new Contact(PersonType.NATURAL, valid.firstNames(), valid.lastNames(),
                    valid.documentType(), valid.documentNumber(), valid.address(), now(), createdBy);
            contact.setPhotoPath(photoPath);
            addChildrenAndCommercialTypes(contact, valid.phones(), valid.emails(), valid.commercialTypes());
            return save(contact);
        } catch (RuntimeException exception) {
            if (photoPath != null) {
                deleteImmediately(photoPath);
            }
            throw exception;
        }
    }

    @Transactional
    public Contact createLegalContact(CreateLegalContactRequest request, User createdBy) {
        return createLegalContact(request, createdBy, null);
    }

    @Transactional
    public Contact createLegalContact(CreateLegalContactRequest request, User createdBy, MultipartFile photo) {
        String photoPath = isPhotoNotProvided(photo) ? null : photoStorageService.store(photo);
        if (photoPath != null) {
            scheduleDeleteOnRollback(photoPath);
        }
        try {
            CreateLegalContactRequest valid = contactValidator.normalizeAndValidate(request);
            validateCreatorAndPermissions(createdBy, valid.commercialTypes());
            ensureIdentityIsNew(valid.documentType(), valid.documentNumber());

            Contact contact = new Contact(PersonType.JURIDICA, valid.legalName(), valid.documentType(),
                    valid.documentNumber(), valid.address(), now(), createdBy);
            contact.setPhotoPath(photoPath);
            addChildrenAndCommercialTypes(contact, valid.phones(), valid.emails(), valid.commercialTypes());
            return save(contact);
        } catch (RuntimeException exception) {
            if (photoPath != null) {
                deleteImmediately(photoPath);
            }
            throw exception;
        }
    }

    @Transactional
    public Contact addCommercialType(Long contactId, CommercialType commercialType, User actor) {
        validatePermissionToAddCommercialType(actor, commercialType);

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException("No se encontró el contacto indicado."));
        if (contact.getDeletedAt() != null) {
            throw new ContactBusinessException("No se puede agregar una relación comercial a un contacto eliminado.");
        }
        if (contact.getCommercialTypes().contains(commercialType)) {
            throw new ContactConflictException("El contacto ya tiene asignada esa relación comercial.");
        }

        contact.addCommercialType(commercialType);
        contact.setUpdatedBy(actor);
        contact.setUpdatedAt(now());
        try {
            return contactRepository.saveAndFlush(contact);
        } catch (DataIntegrityViolationException exception) {
            throw new ContactConflictException(
                    "No se pudo agregar la relación comercial por un conflicto con los datos registrados.");
        }
    }

    @Transactional
    public Contact changeStatus(Long contactId, String requestedStatus, User actor) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException("No se encontró el contacto indicado."));
        if (contact.getDeletedAt() != null) {
            throw new ContactBusinessException("No se puede cambiar el estado de un contacto eliminado.");
        }
        if (actor == null || actor.getRole() != com.gestorcontactos.gestor_contactos.entity.Role.ADMINISTRADOR
                || !actor.isEnabled()) {
            throw new AccessDeniedException("Solo un administrador habilitado puede cambiar el estado del contacto.");
        }
        if (requestedStatus == null) {
            throw new ContactBusinessException("El estado solicitado no es válido.");
        }

        ContactStatus newStatus;
        try {
            newStatus = ContactStatus.valueOf(requestedStatus);
        } catch (IllegalArgumentException exception) {
            throw new ContactBusinessException("El estado solicitado no es válido.");
        }
        if (contact.getStatus() == newStatus) {
            throw new ContactBusinessException("El contacto ya se encuentra en el estado solicitado.");
        }

        contact.setStatus(newStatus);
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact softDelete(Long contactId, User actor) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException("No se encontró el contacto indicado."));
        if (contact.getDeletedAt() != null) {
            throw new ContactBusinessException("El contacto ya está eliminado.");
        }
        requireAdministrator(actor);

        LocalDateTime now = now();
        contact.setDeletedAt(now);
        contact.setDeletedBy(actor);
        return saveModified(contact, actor, now);
    }

    @Transactional
    public Contact restore(Long contactId, User actor) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException("No se encontró el contacto indicado."));
        if (contact.getDeletedAt() == null) {
            throw new ContactBusinessException("El contacto no está eliminado.");
        }
        requireAdministrator(actor);

        LocalDateTime now = now();
        contact.setDeletedAt(null);
        contact.setDeletedBy(null);
        contact.setStatus(ContactStatus.INACTIVO);
        return saveModified(contact, actor, now);
    }

    private void requireAdministrator(User actor) {
        if (actor == null || actor.getRole() != Role.ADMINISTRADOR || !actor.isEnabled()) {
            throw new AccessDeniedException("Solo un administrador habilitado puede realizar esta operación.");
        }
    }

    @Transactional(readOnly = true)
    public Contact getContactForEditing(Long contactId, User actor) {
        return loadEditableContact(contactId, actor);
    }

    @Transactional
    public Contact updateEditableData(Long contactId, EditContactRequest request, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        if (request == null) {
            throw new ContactBusinessException("Los datos de edición son obligatorios.");
        }
        contact.setAddress(contactValidator.normalizeAndValidateAddress(request.address()));
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact addPhone(Long contactId, ContactPhoneRequest request, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        ContactPhoneRequest valid = contactValidator.normalizeAndValidatePhone(request);
        if (contact.getPhones().stream().anyMatch(phone -> samePhone(phone, valid))) {
            throw new ContactBusinessException("No se permiten teléfonos duplicados en el contacto.");
        }
        contact.addPhone(new ContactPhone(valid.phoneType(), valid.areaCode(), valid.phoneNumber()));
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact updatePhone(Long contactId, Long phoneId, ContactPhoneRequest request, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        ContactPhone phone = findPhone(contact, phoneId);
        ContactPhoneRequest valid = contactValidator.normalizeAndValidatePhone(request);
        if (contact.getPhones().stream()
                .filter(existing -> !Objects.equals(existing.getId(), phoneId))
                .anyMatch(existing -> samePhone(existing, valid))) {
            throw new ContactBusinessException("No se permiten teléfonos duplicados en el contacto.");
        }
        phone.setPhoneType(valid.phoneType());
        phone.setAreaCode(valid.areaCode());
        phone.setPhoneNumber(valid.phoneNumber());
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact removePhone(Long contactId, Long phoneId, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        ContactPhone phone = findPhone(contact, phoneId);
        if (contact.getPhones().size() <= 1) {
            throw new ContactBusinessException("El contacto debe conservar al menos un teléfono.");
        }
        contact.removePhone(phone);
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact addEmail(Long contactId, ContactEmailRequest request, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        String validEmail = contactValidator.normalizeAndValidateEmail(request);
        if (contact.getEmails().stream().anyMatch(email -> email.getEmail().equalsIgnoreCase(validEmail))) {
            throw new ContactBusinessException("No se permiten correos electrónicos duplicados en el contacto.");
        }
        contact.addEmail(new ContactEmail(validEmail));
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact updateEmail(Long contactId, Long emailId, ContactEmailRequest request, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        ContactEmail email = findEmail(contact, emailId);
        String validEmail = contactValidator.normalizeAndValidateEmail(request);
        if (contact.getEmails().stream()
                .filter(existing -> !Objects.equals(existing.getId(), emailId))
                .anyMatch(existing -> existing.getEmail().equalsIgnoreCase(validEmail))) {
            throw new ContactBusinessException("No se permiten correos electrónicos duplicados en el contacto.");
        }
        email.setEmail(validEmail);
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact removeEmail(Long contactId, Long emailId, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        ContactEmail email = findEmail(contact, emailId);
        if (contact.getEmails().size() <= 1) {
            throw new ContactBusinessException("El contacto debe conservar al menos un correo electrónico.");
        }
        contact.removeEmail(email);
        return saveModified(contact, actor);
    }

    @Transactional
    public Contact replacePhoto(Long contactId, MultipartFile photo, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        String newPath = photoStorageService.store(photo);
        scheduleDeleteOnRollback(newPath);
        String oldPath = contact.getPhotoPath();
        User oldUpdatedBy = contact.getUpdatedBy();
        LocalDateTime oldUpdatedAt = contact.getUpdatedAt();
        contact.setPhotoPath(newPath);
        try {
            Contact updated = saveModified(contact, actor);
            scheduleDeleteAfterCommit(oldPath);
            return updated;
        } catch (RuntimeException exception) {
            contact.setPhotoPath(oldPath);
            contact.setUpdatedBy(oldUpdatedBy);
            contact.setUpdatedAt(oldUpdatedAt);
            deleteImmediately(newPath);
            throw exception;
        }
    }

    @Transactional
    public Contact removePhoto(Long contactId, User actor) {
        Contact contact = loadEditableContact(contactId, actor);
        String oldPath = contact.getPhotoPath();
        User oldUpdatedBy = contact.getUpdatedBy();
        LocalDateTime oldUpdatedAt = contact.getUpdatedAt();
        contact.setPhotoPath(null);
        Contact updated;
        try {
            updated = saveModified(contact, actor);
        } catch (RuntimeException exception) {
            contact.setPhotoPath(oldPath);
            contact.setUpdatedBy(oldUpdatedBy);
            contact.setUpdatedAt(oldUpdatedAt);
            throw exception;
        }
        scheduleDeleteAfterCommit(oldPath);
        return updated;
    }

    @Transactional(readOnly = true)
    public String getPhotoPathForViewing(Long contactId, User actor) {
        return loadEditableContact(contactId, actor).getPhotoPath();
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
    public List<Contact> findAllDeleted() {
        return contactRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Contact> findAllNotDeletedByCommercialType(CommercialType commercialType) {
        return contactRepository.findAllNotDeletedByCommercialType(commercialType);
    }

    private void ensureIdentityIsNew(DocumentType documentType, String documentNumber) {
        if (contactRepository.findByDocumentTypeAndDocumentNumber(documentType, documentNumber).isPresent()) {
            throw new ContactConflictException("Ya existe un contacto con el documento indicado.");
        }
    }

    private Contact loadEditableContact(Long contactId, User actor) {
        if (actor == null || actor.getRole() == null || !actor.isEnabled()) {
            throw new AccessDeniedException("Se requiere un usuario autenticado para editar el contacto.");
        }
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException("No se encontró el contacto indicado."));
        if (contact.getDeletedAt() != null) {
            throw new ContactBusinessException("No se puede modificar un contacto eliminado.");
        }

        boolean allowed = switch (actor.getRole()) {
            case ADMINISTRADOR -> true;
            case VENDEDOR -> contact.getStatus() == com.gestorcontactos.gestor_contactos.entity.ContactStatus.ACTIVO
                    && contact.getCommercialTypes().contains(CommercialType.CLIENTE);
            case COMPRAS -> contact.getStatus() == com.gestorcontactos.gestor_contactos.entity.ContactStatus.ACTIVO
                    && contact.getCommercialTypes().contains(CommercialType.PROVEEDOR);
            case ALMACENERO -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException("No tiene permisos para editar este contacto.");
        }
        return contact;
    }

    private ContactPhone findPhone(Contact contact, Long phoneId) {
        return contact.getPhones().stream()
                .filter(phone -> Objects.equals(phone.getId(), phoneId))
                .findFirst()
                .orElseThrow(() -> new ContactBusinessException("No se encontró el teléfono indicado."));
    }

    private ContactEmail findEmail(Contact contact, Long emailId) {
        return contact.getEmails().stream()
                .filter(email -> Objects.equals(email.getId(), emailId))
                .findFirst()
                .orElseThrow(() -> new ContactBusinessException("No se encontró el correo indicado."));
    }

    private boolean samePhone(ContactPhone existing, ContactPhoneRequest request) {
        return existing.getPhoneType() == request.phoneType()
                && Objects.equals(existing.getAreaCode(), request.areaCode())
                && Objects.equals(existing.getPhoneNumber(), request.phoneNumber());
    }

    private Contact saveModified(Contact contact, User actor) {
        return saveModified(contact, actor, now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(AUDIT_ZONE));
    }

    private Contact saveModified(Contact contact, User actor, LocalDateTime updatedAt) {
        contact.setUpdatedBy(actor);
        contact.setUpdatedAt(updatedAt);
        try {
            return contactRepository.saveAndFlush(contact);
        } catch (DataIntegrityViolationException exception) {
            throw new ContactConflictException(
                    "No se pudo guardar la modificación porque algunos datos ya están registrados.");
        }
    }

    private boolean isPhotoNotProvided(MultipartFile photo) {
        return photo == null || (photo.isEmpty()
                && (photo.getOriginalFilename() == null || photo.getOriginalFilename().isBlank()));
    }

    private void scheduleDeleteAfterCommit(String photoPath) {
        if (photoPath == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    photoStorageService.delete(photoPath);
                }
            });
        } else {
            photoStorageService.delete(photoPath);
        }
    }

    private void scheduleDeleteOnRollback(String photoPath) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        photoStorageService.delete(photoPath);
                    }
                }
            });
        }
    }

    private void deleteImmediately(String photoPath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            photoStorageService.delete(photoPath);
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
            throw new ContactConflictException(
                    "No se pudo crear el contacto porque existe un conflicto con los datos registrados.");
        }
    }
}
