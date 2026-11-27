package com.gestorcontactos.gestor_contactos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestorcontactos.gestor_contactos.dto.CreateContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateLegalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateNaturalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.ContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.ContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.EditContactRequest;
import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.Contact;
import com.gestorcontactos.gestor_contactos.entity.ContactStatus;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import com.gestorcontactos.gestor_contactos.entity.PhoneType;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import com.gestorcontactos.gestor_contactos.repository.ContactRepository;
import com.gestorcontactos.gestor_contactos.validation.ContactValidator;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private ContactPhotoStorageService photoStorageService;

    private ContactService contactService;
    private User administrator;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactRepository, new ContactValidator(), photoStorageService);
        administrator = user(Role.ADMINISTRADOR);
    }

    @Test
    void createsValidNaturalPersonAndNormalizesNamesAddressAndEmail() {
        stubSave();
        allowNewIdentity(DocumentType.DNI, "12345678");
        Contact contact = contactService.createNaturalContact(naturalRequest(
                "  María   José ", " Núñez  Ríos ", DocumentType.DNI, "12345678", " Av. Perú   123 ",
                List.of(mobile()), List.of(email("MARIA@example.com")), Set.of(CommercialType.CLIENTE)), administrator);

        assertEquals(PersonType.NATURAL, contact.getPersonType());
        assertEquals("María José", contact.getFirstNames());
        assertEquals("Núñez Ríos", contact.getLastNames());
        assertEquals("Av. Perú 123", contact.getAddress());
        assertEquals("maria@example.com", contact.getEmails().get(0).getEmail());
        assertNotNull(contact.getCreatedAt());
        verify(contactRepository).saveAndFlush(contact);
    }

    @Test
    void createsValidLegalPerson() {
        stubSave();
        allowNewIdentity(DocumentType.RUC, "20123456789");
        Contact contact = contactService.createLegalContact(legalRequest("Comercial Los Andes S.A.C.",
                DocumentType.RUC, "20123456789", "Jr. Los Álamos 245", List.of(mobile()),
                List.of(email("info@example.com")), Set.of(CommercialType.PROVEEDOR)), administrator);

        assertEquals(PersonType.JURIDICA, contact.getPersonType());
        assertEquals("Comercial Los Andes S.A.C.", contact.getLegalName());
        assertEquals(DocumentType.RUC, contact.getDocumentType());
        assertEquals(ContactStatus.ACTIVO, contact.getStatus());
    }

    @Test
    void rejectsInvalidDni() {
        assertInvalidNatural(DocumentType.DNI, "1234567", "El DNI debe contener exactamente 8 dígitos.");
    }

    @Test
    void rejectsInvalidForeignerCard() {
        assertInvalidNatural(DocumentType.CARNET_EXTRANJERIA, "1234567890",
                "El Carnet de Extranjería debe contener exactamente 11 dígitos.");
    }

    @Test
    void rejectsInvalidRuc() {
        assertThrows(ContactBusinessException.class, () -> contactService.createLegalContact(legalRequest(
                "Empresa 123 S.A.C.", DocumentType.RUC, "1234ABCD", "Av. Perú 123", List.of(mobile()),
                List.of(email("valid@example.com")), Set.of(CommercialType.CLIENTE)), administrator));
    }

    @Test
    void rejectsNamesContainingNumbers() {
        assertInvalidNaturalName("María123", "Pérez");
    }

    @Test
    void rejectsNamesContainingSymbols() {
        assertInvalidNaturalName("Juan@", "Pérez");
    }

    @Test
    void rejectsBlankLegalName() {
        ContactBusinessException exception = assertThrows(ContactBusinessException.class,
                () -> contactService.createLegalContact(legalRequest("   ", DocumentType.RUC, "20123456789",
                        "Av. Perú 123", List.of(mobile()), List.of(email("valid@example.com")),
                        Set.of(CommercialType.CLIENTE)), administrator));
        assertEquals("La razón social debe tener entre 2 y 200 caracteres válidos.", exception.getMessage());
    }

    @Test
    void rejectsInvalidAddress() {
        assertThrows(ContactBusinessException.class, () -> contactService.createNaturalContact(naturalRequest(
                "María", "Pérez", DocumentType.DNI, "12345678", "<script>", List.of(mobile()),
                List.of(email("valid@example.com")), Set.of(CommercialType.CLIENTE)), administrator));
    }

    @Test
    void rejectsContactWithoutPhones() {
        ContactBusinessException exception = assertThrows(ContactBusinessException.class,
                () -> contactService.createNaturalContact(naturalRequest("María", "Pérez", DocumentType.DNI,
                        "12345678", "Av. Perú 123", List.of(), List.of(email("valid@example.com")),
                        Set.of(CommercialType.CLIENTE)), administrator));
        assertEquals("Debe registrar al menos un teléfono.", exception.getMessage());
    }

    @Test
    void rejectsContactWithoutEmails() {
        ContactBusinessException exception = assertThrows(ContactBusinessException.class,
                () -> contactService.createNaturalContact(naturalRequest("María", "Pérez", DocumentType.DNI,
                        "12345678", "Av. Perú 123", List.of(mobile()), List.of(),
                        Set.of(CommercialType.CLIENTE)), administrator));
        assertEquals("Debe registrar al menos un correo electrónico.", exception.getMessage());
    }

    @Test
    void rejectsInvalidCellPhone() {
        assertThrows(ContactBusinessException.class, () -> contactService.createNaturalContact(naturalRequest(
                "María", "Pérez", DocumentType.DNI, "12345678", "Av. Perú 123",
                List.of(new CreateContactPhoneRequest(PhoneType.CELULAR, "", "98765432")),
                List.of(email("valid@example.com")), Set.of(CommercialType.CLIENTE)), administrator));
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(ContactBusinessException.class, () -> contactService.createNaturalContact(naturalRequest(
                "María", "Pérez", DocumentType.DNI, "12345678", "Av. Perú 123", List.of(mobile()),
                List.of(email("no-es-un-correo")), Set.of(CommercialType.CLIENTE)), administrator));
    }

    @Test
    void rejectsDuplicatePhoneWithinContact() {
        assertThrows(ContactBusinessException.class, () -> contactService.createNaturalContact(naturalRequest(
                "María", "Pérez", DocumentType.DNI, "12345678", "Av. Perú 123",
                List.of(mobile(), mobile()), List.of(email("valid@example.com")),
                Set.of(CommercialType.CLIENTE)), administrator));
    }

    @Test
    void rejectsDuplicateEmailsIgnoringCase() {
        assertThrows(ContactBusinessException.class, () -> contactService.createNaturalContact(naturalRequest(
                "María", "Pérez", DocumentType.DNI, "12345678", "Av. Perú 123", List.of(mobile()),
                List.of(email("valid@example.com"), email("VALID@example.com")),
                Set.of(CommercialType.CLIENTE)), administrator));
    }

    @Test
    void normalizesEmailsToLowercase() {
        stubSave();
        allowNewIdentity(DocumentType.DNI, "12345678");
        Contact contact = contactService.createNaturalContact(naturalRequest("María", "Pérez", DocumentType.DNI,
                "12345678", "Av. Perú 123", List.of(mobile()), List.of(email("MARIA.PEREZ@EXAMPLE.COM")),
                Set.of(CommercialType.CLIENTE)), administrator);

        assertEquals("maria.perez@example.com", contact.getEmails().get(0).getEmail());
    }

    @Test
    void rejectsDuplicateIdentity() {
        when(contactRepository.findByDocumentTypeAndDocumentNumber(DocumentType.DNI, "12345678"))
                .thenReturn(Optional.of(existingContact(administrator)));

        ContactBusinessException exception = assertThrows(ContactBusinessException.class,
                () -> contactService.createNaturalContact(validNaturalRequest(), administrator));

        assertEquals("Ya existe un contacto con el documento indicado.", exception.getMessage());
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void rejectsIdentityAlreadyUsedBySoftDeletedContact() {
        Contact deleted = existingContact(administrator);
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findByDocumentTypeAndDocumentNumber(DocumentType.DNI, "12345678"))
                .thenReturn(Optional.of(deleted));

        assertThrows(ContactBusinessException.class,
                () -> contactService.createNaturalContact(validNaturalRequest(), administrator));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void translatesDatabaseIntegrityConflictsToBusinessException() {
        allowNewIdentity(DocumentType.DNI, "12345678");
        when(contactRepository.saveAndFlush(any(Contact.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("database detail"));

        ContactBusinessException exception = assertThrows(ContactBusinessException.class,
                () -> contactService.createNaturalContact(validNaturalRequest(), administrator));

        assertEquals("No se pudo crear el contacto porque existe un conflicto con los datos registrados.",
                exception.getMessage());
    }

    @Test
    void allowsBothCommercialTypesOnOneContactForAdministrator() {
        stubSave();
        allowNewIdentity(DocumentType.DNI, "12345678");
        Contact contact = contactService.createNaturalContact(naturalRequest("María", "Pérez", DocumentType.DNI,
                "12345678", "Av. Perú 123", List.of(mobile()), List.of(email("valid@example.com")),
                Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR)), administrator);

        assertEquals(Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR), contact.getCommercialTypes());
    }

    @Test
    void sellerAddsClientToExistingSupplierContact() {
        Long contactId = 21L;
        Contact contact = existingContactWithType(CommercialType.PROVEEDOR);
        prepareContactForUpdate(contactId, contact);

        Contact updated = contactService.addCommercialType(contactId, CommercialType.CLIENTE, user(Role.VENDEDOR));

        assertEquals(contact, updated);
        assertEquals(Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR), updated.getCommercialTypes());
        verify(contactRepository).saveAndFlush(contact);
    }

    @Test
    void purchasingAddsSupplierToExistingClientContact() {
        Long contactId = 22L;
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        prepareContactForUpdate(contactId, contact);

        Contact updated = contactService.addCommercialType(contactId, CommercialType.PROVEEDOR, user(Role.COMPRAS));

        assertEquals(contact, updated);
        assertEquals(Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR), updated.getCommercialTypes());
        verify(contactRepository).saveAndFlush(contact);
    }

    @Test
    void administratorCanAddClientOrSupplier() {
        Contact clientContact = existingContact();
        Contact supplierContact = existingContact();
        prepareContactForUpdate(23L, clientContact);
        prepareContactForUpdate(24L, supplierContact);

        assertEquals(clientContact, contactService.addCommercialType(23L, CommercialType.CLIENTE, administrator));
        assertEquals(supplierContact, contactService.addCommercialType(24L, CommercialType.PROVEEDOR, administrator));
        assertEquals(Set.of(CommercialType.CLIENTE), clientContact.getCommercialTypes());
        assertEquals(Set.of(CommercialType.PROVEEDOR), supplierContact.getCommercialTypes());
    }

    @Test
    void sellerCannotAddSupplier() {
        assertRoleCannotAdd(Role.VENDEDOR, CommercialType.PROVEEDOR);
    }

    @Test
    void purchasingCannotAddClient() {
        assertRoleCannotAdd(Role.COMPRAS, CommercialType.CLIENTE);
    }

    @Test
    void warehouseRoleCannotAddEitherCommercialType() {
        assertRoleCannotAdd(Role.ALMACENERO, CommercialType.CLIENTE);
        assertRoleCannotAdd(Role.ALMACENERO, CommercialType.PROVEEDOR);
    }

    @Test
    void rejectsCommercialTypeAlreadyAssigned() {
        Long contactId = 25L;
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        ContactBusinessException exception = assertThrows(ContactBusinessException.class,
                () -> contactService.addCommercialType(contactId, CommercialType.CLIENTE, administrator));

        assertEquals("El contacto ya tiene asignada esa relación comercial.", exception.getMessage());
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void doesNotModifySoftDeletedContact() {
        Long contactId = 26L;
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        contact.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        assertThrows(ContactBusinessException.class,
                () -> contactService.addCommercialType(contactId, CommercialType.PROVEEDOR, administrator));

        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void updatesAuditWithAuthenticatedActorAndKeepsIdentity() {
        Long contactId = 27L;
        Contact contact = existingContactWithType(CommercialType.PROVEEDOR);
        User seller = user(Role.VENDEDOR);
        prepareContactForUpdate(contactId, contact);
        LocalDateTime before = LocalDateTime.now();

        Contact updated = contactService.addCommercialType(contactId, CommercialType.CLIENTE, seller);

        assertEquals("12345678", updated.getDocumentNumber());
        assertEquals(DocumentType.DNI, updated.getDocumentType());
        assertEquals(seller, updated.getUpdatedBy());
        assertNotNull(updated.getUpdatedAt());
        assertFalse(updated.getUpdatedAt().isBefore(before));
        assertFalse(updated.getUpdatedAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void addsRelationToTheLoadedContactInsteadOfCreatingAnotherContact() {
        Long contactId = 28L;
        Contact existing = existingContactWithType(CommercialType.CLIENTE);
        prepareContactForUpdate(contactId, existing);

        Contact result = contactService.addCommercialType(contactId, CommercialType.PROVEEDOR, user(Role.COMPRAS));

        assertEquals(existing, result);
        verify(contactRepository).findById(contactId);
        verify(contactRepository).saveAndFlush(existing);
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void doesNotExposeIdentityMutationMethodsInContactServiceOrEntity() {
        Map<String, Class<?>> immutableFields = Map.of(
                "firstNames", String.class,
                "lastNames", String.class,
                "legalName", String.class,
                "documentType", DocumentType.class,
                "documentNumber", String.class,
                "personType", PersonType.class);

        for (var entry : immutableFields.entrySet()) {
            String field = entry.getKey();
            String setterName = "set" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            assertThrows(NoSuchMethodException.class,
                    () -> Contact.class.getMethod(setterName, entry.getValue()));
        }
        assertEquals(1, EditContactRequest.class.getRecordComponents().length);
        assertEquals("address", EditContactRequest.class.getRecordComponents()[0].getName());
    }

    @Test
    void administratorCanInactivateAndReactivateContactWithoutChangingOtherData() {
        Contact active = existingContactWithType(CommercialType.CLIENTE);
        active.addPhone(new com.gestorcontactos.gestor_contactos.entity.ContactPhone(
                PhoneType.CELULAR, "", "987654321"));
        active.addEmail(new com.gestorcontactos.gestor_contactos.entity.ContactEmail("contact@example.com"));
        active.setPhotoPath("uploads/contacts/00000000-0000-0000-0000-000000000001.jpg");
        Contact inactive = existingContactWithType(CommercialType.PROVEEDOR);
        inactive.setStatus(ContactStatus.INACTIVO);
        prepareContactForUpdate(101L, active);
        prepareContactForUpdate(102L, inactive);
        LocalDateTime before = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

        Contact madeInactive = contactService.changeStatus(101L, "INACTIVO", administrator);
        Contact madeActive = contactService.changeStatus(102L, "ACTIVO", administrator);

        assertEquals(ContactStatus.INACTIVO, madeInactive.getStatus());
        assertEquals(ContactStatus.ACTIVO, madeActive.getStatus());
        assertEquals("Existente", madeInactive.getFirstNames());
        assertEquals("Prueba", madeInactive.getLastNames());
        assertEquals(DocumentType.DNI, madeInactive.getDocumentType());
        assertEquals("12345678", madeInactive.getDocumentNumber());
        assertEquals("Av. Perú 123", madeInactive.getAddress());
        assertEquals("987654321", madeInactive.getPhones().get(0).getPhoneNumber());
        assertEquals("contact@example.com", madeInactive.getEmails().get(0).getEmail());
        assertEquals("uploads/contacts/00000000-0000-0000-0000-000000000001.jpg", madeInactive.getPhotoPath());
        assertEquals(Set.of(CommercialType.CLIENTE), madeInactive.getCommercialTypes());
        assertEquals(administrator, madeInactive.getUpdatedBy());
        assertNotNull(madeInactive.getUpdatedAt());
        assertFalse(madeInactive.getUpdatedAt().isBefore(before));
        assertFalse(madeInactive.getUpdatedAt().isAfter(LocalDateTime.now(java.time.ZoneId.of("America/Lima"))));
        verify(contactRepository).saveAndFlush(active);
        verify(contactRepository).saveAndFlush(inactive);
    }

    @Test
    void nonAdministratorsCannotActivateOrInactivateContacts() {
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        when(contactRepository.findById(103L)).thenReturn(Optional.of(contact));

        for (Role role : List.of(Role.VENDEDOR, Role.COMPRAS, Role.ALMACENERO)) {
            assertThrows(AccessDeniedException.class,
                    () -> contactService.changeStatus(103L, "INACTIVO", user(role)));
            assertThrows(AccessDeniedException.class,
                    () -> contactService.changeStatus(103L, "ACTIVO", user(role)));
        }
        assertThrows(AccessDeniedException.class, () -> contactService.changeStatus(103L, "INACTIVO", null));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void rejectsStatusChangeForMissingOrSoftDeletedContact() {
        Contact deleted = existingContactWithType(CommercialType.CLIENTE);
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findById(104L)).thenReturn(Optional.empty());
        when(contactRepository.findById(105L)).thenReturn(Optional.of(deleted));

        assertThrows(ContactBusinessException.class,
                () -> contactService.changeStatus(104L, "INACTIVO", administrator));
        assertThrows(ContactBusinessException.class,
                () -> contactService.changeStatus(105L, "INACTIVO", administrator));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void rejectsInvalidNullAndUnchangedStatuses() {
        Contact active = existingContactWithType(CommercialType.CLIENTE);
        when(contactRepository.findById(106L)).thenReturn(Optional.of(active));

        assertThrows(ContactBusinessException.class,
                () -> contactService.changeStatus(106L, "ELIMINADO", administrator));
        assertThrows(ContactBusinessException.class,
                () -> contactService.changeStatus(106L, null, administrator));
        assertThrows(ContactBusinessException.class,
                () -> contactService.changeStatus(106L, "ACTIVO", administrator));
        assertEquals(ContactStatus.ACTIVO, active.getStatus());
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void administratorSoftDeletesInPlaceAndPreservesContactData() {
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        contact.addCommercialType(CommercialType.PROVEEDOR);
        contact.addPhone(new com.gestorcontactos.gestor_contactos.entity.ContactPhone(
                PhoneType.CELULAR, "", "987654321"));
        contact.addEmail(new com.gestorcontactos.gestor_contactos.entity.ContactEmail("contact@example.com"));
        contact.setPhotoPath("uploads/contacts/contact.jpg");
        prepareContactForUpdate(107L, contact);
        LocalDateTime before = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

        Contact deleted = contactService.softDelete(107L, administrator);

        assertEquals(contact, deleted);
        assertNotNull(deleted.getDeletedAt());
        assertFalse(deleted.getDeletedAt().isBefore(before));
        assertFalse(deleted.getDeletedAt().isAfter(LocalDateTime.now(java.time.ZoneId.of("America/Lima"))));
        assertEquals(administrator, deleted.getDeletedBy());
        assertEquals(administrator, deleted.getUpdatedBy());
        assertNotNull(deleted.getUpdatedAt());
        assertEquals(ContactStatus.ACTIVO, deleted.getStatus());
        assertEquals("12345678", deleted.getDocumentNumber());
        assertEquals("Av. Perú 123", deleted.getAddress());
        assertEquals("987654321", deleted.getPhones().get(0).getPhoneNumber());
        assertEquals("contact@example.com", deleted.getEmails().get(0).getEmail());
        assertEquals("uploads/contacts/contact.jpg", deleted.getPhotoPath());
        assertEquals(Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR), deleted.getCommercialTypes());
        verify(contactRepository).saveAndFlush(contact);
        verify(contactRepository, never()).delete(any(Contact.class));
        verify(photoStorageService, never()).delete(any());
    }

    @Test
    void administratorRestoresDeletedContactAsInactiveAndPreservesData() {
        Contact contact = existingContactWithType(CommercialType.PROVEEDOR);
        contact.addPhone(new com.gestorcontactos.gestor_contactos.entity.ContactPhone(
                PhoneType.CELULAR, "", "987654321"));
        contact.addEmail(new com.gestorcontactos.gestor_contactos.entity.ContactEmail("contact@example.com"));
        contact.setPhotoPath("uploads/contacts/contact.jpg");
        contact.setDeletedAt(LocalDateTime.now().minusDays(2));
        contact.setDeletedBy(user(Role.ADMINISTRADOR));
        contact.setStatus(ContactStatus.ACTIVO);
        prepareContactForUpdate(108L, contact);

        Contact restored = contactService.restore(108L, administrator);

        assertEquals(contact, restored);
        assertEquals(null, restored.getDeletedAt());
        assertEquals(null, restored.getDeletedBy());
        assertEquals(ContactStatus.INACTIVO, restored.getStatus());
        assertEquals(administrator, restored.getUpdatedBy());
        assertNotNull(restored.getUpdatedAt());
        assertEquals("12345678", restored.getDocumentNumber());
        assertEquals("987654321", restored.getPhones().get(0).getPhoneNumber());
        assertEquals("contact@example.com", restored.getEmails().get(0).getEmail());
        assertEquals("uploads/contacts/contact.jpg", restored.getPhotoPath());
        assertEquals(Set.of(CommercialType.PROVEEDOR), restored.getCommercialTypes());
        verify(contactRepository).saveAndFlush(contact);
        verify(contactRepository, never()).delete(any(Contact.class));
        verify(photoStorageService, never()).delete(any());
    }

    @Test
    void onlyEnabledAdministratorCanSoftDeleteOrRestore() {
        Contact contact = existingContact();
        Contact deleted = existingContact();
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findById(109L)).thenReturn(Optional.of(contact));
        when(contactRepository.findById(113L)).thenReturn(Optional.of(deleted));
        for (Role role : List.of(Role.VENDEDOR, Role.COMPRAS, Role.ALMACENERO)) {
            assertThrows(AccessDeniedException.class, () -> contactService.softDelete(109L, user(role)));
            assertThrows(AccessDeniedException.class, () -> contactService.restore(113L, user(role)));
        }
        User disabledAdmin = new User("disabled", "hash", Role.ADMINISTRADOR, false, LocalDateTime.now());
        assertThrows(AccessDeniedException.class, () -> contactService.softDelete(109L, disabledAdmin));
        assertThrows(AccessDeniedException.class, () -> contactService.restore(113L, disabledAdmin));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void rejectsMissingAlreadyDeletedAndNotDeletedLifecycleOperations() {
        Contact deleted = existingContact();
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findById(110L)).thenReturn(Optional.empty());
        when(contactRepository.findById(111L)).thenReturn(Optional.of(deleted));
        when(contactRepository.findById(112L)).thenReturn(Optional.of(existingContact()));

        assertThrows(ContactBusinessException.class, () -> contactService.softDelete(110L, administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.softDelete(111L, administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.restore(112L, administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.restore(110L, administrator));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void delegatesDeletedContactListToRepository() {
        contactService.findAllDeleted();
        verify(contactRepository).findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();
    }

    @Test
    void administratorSellerAndPurchasingCanUpdateAddressWithinTheirContactScope() {
        Contact adminContact = existingContactWithType(CommercialType.PROVEEDOR);
        Contact sellerContact = existingContactWithType(CommercialType.CLIENTE);
        Contact buyerContact = existingContactWithType(CommercialType.PROVEEDOR);
        prepareContactForUpdate(31L, adminContact);
        prepareContactForUpdate(32L, sellerContact);
        prepareContactForUpdate(33L, buyerContact);

        contactService.updateEditableData(31L, new EditContactRequest("  Av. Nueva 456  "), administrator);
        contactService.updateEditableData(32L, new EditContactRequest("Jr. Cliente 123"), user(Role.VENDEDOR));
        contactService.updateEditableData(33L, new EditContactRequest("Jr. Proveedor 123"), user(Role.COMPRAS));

        assertEquals("Av. Nueva 456", adminContact.getAddress());
        assertEquals("Jr. Cliente 123", sellerContact.getAddress());
        assertEquals("Jr. Proveedor 123", buyerContact.getAddress());
    }

    @Test
    void warehouseAndRolesOutsideCommercialScopeCannotEdit() {
        Contact supplier = existingContactWithType(CommercialType.PROVEEDOR);
        Contact client = existingContactWithType(CommercialType.CLIENTE);
        when(contactRepository.findById(34L)).thenReturn(Optional.of(supplier));
        when(contactRepository.findById(35L)).thenReturn(Optional.of(client));

        assertThrows(AccessDeniedException.class, () -> contactService.updateEditableData(
                34L, new EditContactRequest("Av. Nueva 456"), user(Role.VENDEDOR)));
        assertThrows(AccessDeniedException.class, () -> contactService.updateEditableData(
                35L, new EditContactRequest("Av. Nueva 456"), user(Role.COMPRAS)));
        assertThrows(AccessDeniedException.class, () -> contactService.updateEditableData(
                35L, new EditContactRequest("Av. Nueva 456"), user(Role.ALMACENERO)));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void editRequestCannotChangeIdentityAndAuditUsesAuthenticatedActor() {
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        User seller = user(Role.VENDEDOR);
        prepareContactForUpdate(36L, contact);
        LocalDateTime before = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));

        contactService.updateEditableData(36L, new EditContactRequest("Av. Nueva 456"), seller);

        assertEquals("Existente", contact.getFirstNames());
        assertEquals("Prueba", contact.getLastNames());
        assertEquals(DocumentType.DNI, contact.getDocumentType());
        assertEquals("12345678", contact.getDocumentNumber());
        assertEquals(seller, contact.getUpdatedBy());
        assertFalse(contact.getUpdatedAt().isBefore(before));
        assertFalse(contact.getUpdatedAt().isAfter(LocalDateTime.now(java.time.ZoneId.of("America/Lima"))));
    }

    @Test
    void rejectsInvalidAddressAndDoesNotSave() {
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        when(contactRepository.findById(37L)).thenReturn(Optional.of(contact));

        assertThrows(ContactBusinessException.class, () -> contactService.updateEditableData(
                37L, new EditContactRequest("bad"), administrator));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void addsAndUpdatesPhoneAndRejectsDuplicates() {
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        var existingPhone = new com.gestorcontactos.gestor_contactos.entity.ContactPhone(
                PhoneType.CELULAR, "", "987654321");
        setEntityId(existingPhone, 1L);
        contact.addPhone(existingPhone);
        prepareContactForUpdate(38L, contact);

        contactService.addPhone(38L, new ContactPhoneRequest(PhoneType.FIJO, "01", "2345678"), administrator);
        assertEquals(2, contact.getPhones().size());
        assertEquals("01", contact.getPhones().get(1).getAreaCode());
        assertThrows(ContactBusinessException.class, () -> contactService.addPhone(38L,
                new ContactPhoneRequest(PhoneType.CELULAR, "", "987654321"), administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.addPhone(38L,
                new ContactPhoneRequest(PhoneType.CELULAR, "", "987"), administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.updatePhone(38L,
                contact.getPhones().get(1).getId(),
                new ContactPhoneRequest(PhoneType.CELULAR, "", "987654321"), administrator));
        contactService.updatePhone(38L, contact.getPhones().get(1).getId(),
                new ContactPhoneRequest(PhoneType.CELULAR, "", "912345678"), administrator);
        assertEquals("912345678", contact.getPhones().get(1).getPhoneNumber());
    }

    @Test
    void removesPhoneOnlyWhenAtLeastOneRemains() {
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        var first = new com.gestorcontactos.gestor_contactos.entity.ContactPhone(PhoneType.CELULAR, "", "987654321");
        var second = new com.gestorcontactos.gestor_contactos.entity.ContactPhone(PhoneType.CELULAR, "", "912345678");
        setEntityId(first, 1L);
        setEntityId(second, 2L);
        contact.addPhone(first);
        contact.addPhone(second);
        prepareContactForUpdate(39L, contact);

        contactService.removePhone(39L, first.getId(), administrator);
        assertEquals(1, contact.getPhones().size());
        assertThrows(ContactBusinessException.class, () -> contactService.removePhone(39L, second.getId(), administrator));
    }

    @Test
    void managesEmailsNormalizesAndRejectsDuplicatesAndLastRemoval() {
        Contact contact = existingContactWithType(CommercialType.CLIENTE);
        var first = new com.gestorcontactos.gestor_contactos.entity.ContactEmail("one@example.com");
        var second = new com.gestorcontactos.gestor_contactos.entity.ContactEmail("two@example.com");
        setEntityId(first, 1L);
        setEntityId(second, 2L);
        contact.addEmail(first);
        contact.addEmail(second);
        prepareContactForUpdate(40L, contact);

        contactService.addEmail(40L, new ContactEmailRequest("THREE@example.com"), administrator);
        assertEquals("three@example.com", contact.getEmails().get(2).getEmail());
        assertThrows(ContactBusinessException.class, () -> contactService.addEmail(40L,
                new ContactEmailRequest("ONE@example.com"), administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.addEmail(40L,
                new ContactEmailRequest("invalid"), administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.updateEmail(40L, second.getId(),
                new ContactEmailRequest("ONE@example.com"), administrator));
        contactService.updateEmail(40L, second.getId(), new ContactEmailRequest("UPDATED@example.com"), administrator);
        assertEquals("updated@example.com", second.getEmail());
        contactService.removeEmail(40L, first.getId(), administrator);
        contactService.removeEmail(40L, second.getId(), administrator);
        assertThrows(ContactBusinessException.class, () -> contactService.removeEmail(40L,
                contact.getEmails().get(0).getId(), administrator));
    }

    @Test
    void deletedOrMissingContactCannotBeEdited() {
        Contact deleted = existingContactWithType(CommercialType.CLIENTE);
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findById(41L)).thenReturn(Optional.of(deleted));
        when(contactRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ContactBusinessException.class, () -> contactService.updateEditableData(
                41L, new EditContactRequest("Av. Nueva 456"), administrator));
        assertThrows(ContactBusinessException.class, () -> contactService.updateEditableData(
                42L, new EditContactRequest("Av. Nueva 456"), administrator));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void sellerCannotCreateSupplierOrCombinedCommercialType() {
        User seller = user(Role.VENDEDOR);
        assertThrows(ContactBusinessException.class, () -> contactService.createNaturalContact(naturalRequest(
                "María", "Pérez", DocumentType.DNI, "12345678", "Av. Perú 123", List.of(mobile()),
                List.of(email("valid@example.com")), Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR)), seller));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void delegatesReadOperationsToRepositories() {
        contactService.findById(15L);
        contactService.findByIdentity(DocumentType.DNI, "12345678");
        contactService.findAllNotDeleted();
        contactService.findAllNotDeletedByCommercialType(CommercialType.CLIENTE);

        verify(contactRepository).findById(15L);
        verify(contactRepository).findByDocumentTypeAndDocumentNumber(DocumentType.DNI, "12345678");
        verify(contactRepository).findAllByDeletedAtIsNull();
        verify(contactRepository).findAllNotDeletedByCommercialType(CommercialType.CLIENTE);
    }

    private void assertInvalidNatural(DocumentType documentType, String documentNumber, String expectedMessage) {
        ContactBusinessException exception = assertThrows(ContactBusinessException.class,
                () -> contactService.createNaturalContact(naturalRequest("María", "Pérez", documentType,
                        documentNumber, "Av. Perú 123", List.of(mobile()), List.of(email("valid@example.com")),
                        Set.of(CommercialType.CLIENTE)), administrator));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void assertInvalidNaturalName(String firstNames, String lastNames) {
        assertThrows(ContactBusinessException.class, () -> contactService.createNaturalContact(naturalRequest(
                firstNames, lastNames, DocumentType.DNI, "12345678", "Av. Perú 123", List.of(mobile()),
                List.of(email("valid@example.com")), Set.of(CommercialType.CLIENTE)), administrator));
    }

    private CreateNaturalContactRequest validNaturalRequest() {
        return naturalRequest("María", "Pérez", DocumentType.DNI, "12345678", "Av. Perú 123",
                List.of(mobile()), List.of(email("valid@example.com")), Set.of(CommercialType.CLIENTE));
    }

    private CreateNaturalContactRequest naturalRequest(String firstNames, String lastNames,
            DocumentType documentType, String documentNumber, String address,
            List<CreateContactPhoneRequest> phones, List<CreateContactEmailRequest> emails,
            Set<CommercialType> commercialTypes) {
        return new CreateNaturalContactRequest(firstNames, lastNames, documentType, documentNumber, address,
                phones, emails, commercialTypes);
    }

    private CreateLegalContactRequest legalRequest(String legalName, DocumentType documentType,
            String documentNumber, String address, List<CreateContactPhoneRequest> phones,
            List<CreateContactEmailRequest> emails, Set<CommercialType> commercialTypes) {
        return new CreateLegalContactRequest(legalName, documentType, documentNumber, address,
                phones, emails, commercialTypes);
    }

    private CreateContactPhoneRequest mobile() {
        return new CreateContactPhoneRequest(PhoneType.CELULAR, "", "987654321");
    }

    private CreateContactEmailRequest email(String value) {
        return new CreateContactEmailRequest(value);
    }

    private void stubSave() {
        when(contactRepository.saveAndFlush(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void allowNewIdentity(DocumentType documentType, String documentNumber) {
        when(contactRepository.findByDocumentTypeAndDocumentNumber(documentType, documentNumber))
                .thenReturn(Optional.empty());
    }

    private void assertRoleCannotAdd(Role role, CommercialType commercialType) {
        assertThrows(ContactBusinessException.class,
                () -> contactService.addCommercialType(99L, commercialType, user(role)));
        verify(contactRepository, never()).findById(99L);
    }

    private User user(Role role) {
        return new User("test-user", "hash", role, true, LocalDateTime.now());
    }

    private Contact existingContact(User createdBy) {
        return new Contact(PersonType.NATURAL, "Existente", "Prueba", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), createdBy);
    }

    private Contact existingContact() {
        return existingContact(administrator);
    }

    private Contact existingContactWithType(CommercialType commercialType) {
        Contact contact = existingContact();
        contact.addCommercialType(commercialType);
        return contact;
    }

    private void prepareContactForUpdate(Long contactId, Contact contact) {
        when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
        when(contactRepository.saveAndFlush(contact)).thenReturn(contact);
    }

    private void setEntityId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to prepare persisted entity fixture", exception);
        }
    }
}
