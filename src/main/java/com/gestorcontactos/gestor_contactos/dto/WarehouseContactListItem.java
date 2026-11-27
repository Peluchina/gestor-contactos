package com.gestorcontactos.gestor_contactos.dto;

import com.gestorcontactos.gestor_contactos.entity.PersonType;

public record WarehouseContactListItem(Long id, PersonType personType, String displayName, String address) {
}
