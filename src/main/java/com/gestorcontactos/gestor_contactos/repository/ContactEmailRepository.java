package com.gestorcontactos.gestor_contactos.repository;

import com.gestorcontactos.gestor_contactos.entity.ContactEmail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactEmailRepository extends JpaRepository<ContactEmail, Long> {

    List<ContactEmail> findAllByContact_IdOrderByIdAsc(Long contactId);
}
