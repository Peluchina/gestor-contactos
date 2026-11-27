package com.gestorcontactos.gestor_contactos.repository;

import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.Contact;
import com.gestorcontactos.gestor_contactos.entity.ContactStatus;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findAllByDeletedAtIsNull();

    List<Contact> findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();

    List<Contact> findAllByStatusAndDeletedAtIsNull(ContactStatus status);

    Optional<Contact> findByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

    Optional<Contact> findByDocumentTypeAndDocumentNumberAndDeletedAtIsNull(
            DocumentType documentType, String documentNumber);

    @Query("select distinct contact from Contact contact "
            + "join contact.commercialTypes commercialType "
            + "where commercialType = :commercialType and contact.deletedAt is null")
    List<Contact> findAllNotDeletedByCommercialType(@Param("commercialType") CommercialType commercialType);
}
