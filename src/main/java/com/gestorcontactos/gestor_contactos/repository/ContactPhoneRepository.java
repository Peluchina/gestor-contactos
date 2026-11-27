package com.gestorcontactos.gestor_contactos.repository;

import com.gestorcontactos.gestor_contactos.entity.ContactPhone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactPhoneRepository extends JpaRepository<ContactPhone, Long> {

    List<ContactPhone> findAllByContact_IdOrderByIdAsc(Long contactId);
}
