package com.gestorcontactos.gestor_contactos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.Contact;
import com.gestorcontactos.gestor_contactos.entity.ContactEmail;
import com.gestorcontactos.gestor_contactos.entity.ContactPhone;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import com.gestorcontactos.gestor_contactos.entity.PhoneType;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ContactPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    void persistsContactChildrenCommercialTypesAndSoftDeleteAudit() {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deletedAt = now.withNano(0);
        User administrator = new User("contact-test-" + uniqueSuffix, "test-hash", Role.ADMINISTRADOR, true, now);
        entityManager.persist(administrator);

        int documentDigits = Math.floorMod(uniqueSuffix.hashCode(), 90_000_000) + 10_000_000;
        Contact contact = new Contact(PersonType.NATURAL, "Persona de prueba", "Persistencia", DocumentType.DNI,
                Integer.toString(documentDigits), "Av. Prueba 123", now, administrator);
        contact.addPhone(new ContactPhone(PhoneType.CELULAR, "", "987654321"));
        contact.addPhone(new ContactPhone(PhoneType.FIJO, "01", "4567890"));
        contact.addEmail(new ContactEmail("first-" + uniqueSuffix + "@example.com"));
        contact.addEmail(new ContactEmail("second-" + uniqueSuffix + "@example.com"));
        contact.addCommercialType(CommercialType.CLIENTE);
        contact.addCommercialType(CommercialType.PROVEEDOR);

        contactRepository.saveAndFlush(contact);
        Long contactId = contact.getId();
        entityManager.clear();

        Contact persisted = contactRepository.findById(contactId).orElseThrow();
        assertEquals(administrator.getId(), persisted.getCreatedBy().getId());
        assertEquals(2, persisted.getPhones().size());
        assertEquals(2, persisted.getEmails().size());
        assertTrue(persisted.getCommercialTypes().contains(CommercialType.CLIENTE));
        assertTrue(persisted.getCommercialTypes().contains(CommercialType.PROVEEDOR));
        assertTrue(contactRepository.findAllNotDeletedByCommercialType(CommercialType.CLIENTE)
                .stream().anyMatch(found -> found.getId().equals(contactId)));

        persisted.setDeletedAt(deletedAt);
        persisted.setDeletedBy(administrator);
        contactRepository.saveAndFlush(persisted);
        entityManager.clear();

        Contact logicallyDeleted = contactRepository.findById(contactId).orElseThrow();
        assertEquals(deletedAt, logicallyDeleted.getDeletedAt());
        assertEquals(administrator.getId(), logicallyDeleted.getDeletedBy().getId());
        assertFalse(contactRepository.findByDocumentTypeAndDocumentNumberAndDeletedAtIsNull(
                DocumentType.DNI, Integer.toString(documentDigits)).isPresent());
        assertTrue(contactRepository.findByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, Integer.toString(documentDigits)).isPresent());
    }
}
