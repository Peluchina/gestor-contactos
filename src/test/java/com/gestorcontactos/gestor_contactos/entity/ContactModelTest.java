package com.gestorcontactos.gestor_contactos.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ContactModelTest {

    @Test
    void contactKeepsMultiplePhonesEmailsAndBothCommercialTypes() {
        User administrator = new User("admin", "hash", Role.ADMINISTRADOR, true, LocalDateTime.now());
        Contact contact = new Contact(PersonType.NATURAL, DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), administrator);
        ContactPhone mobile = new ContactPhone(PhoneType.CELULAR, "", "987654321");
        ContactPhone landline = new ContactPhone(PhoneType.FIJO, "01", "4567890");
        ContactEmail primaryEmail = new ContactEmail("first@example.com");
        ContactEmail secondaryEmail = new ContactEmail("second@example.com");

        contact.addPhone(mobile);
        contact.addPhone(landline);
        contact.addEmail(primaryEmail);
        contact.addEmail(secondaryEmail);
        contact.addCommercialType(CommercialType.CLIENTE);
        contact.addCommercialType(CommercialType.PROVEEDOR);

        assertEquals(2, contact.getPhones().size());
        assertEquals(2, contact.getEmails().size());
        assertEquals(contact, mobile.getContact());
        assertEquals(contact, landline.getContact());
        assertEquals(contact, primaryEmail.getContact());
        assertEquals(contact, secondaryEmail.getContact());
        assertEquals(2, contact.getCommercialTypes().size());
        assertEquals(ContactStatus.ACTIVO, contact.getStatus());
        assertNull(contact.getDeletedAt());
        assertNull(contact.getDeletedBy());
    }
}
