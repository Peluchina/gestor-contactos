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
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    private ContactService contactService;
    private User administrator;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactRepository, new ContactValidator());
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
        for (Method method : ContactService.class.getDeclaredMethods()) {
            assertFalse(method.getName().startsWith("update") || method.getName().startsWith("edit"));
        }
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
}
