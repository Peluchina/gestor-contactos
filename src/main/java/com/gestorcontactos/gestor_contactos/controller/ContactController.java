package com.gestorcontactos.gestor_contactos.controller;

import com.gestorcontactos.gestor_contactos.dto.ContactDetailsView;
import com.gestorcontactos.gestor_contactos.dto.ContactAssociationLookupView;
import com.gestorcontactos.gestor_contactos.dto.ContactEditView;
import com.gestorcontactos.gestor_contactos.dto.ContactEmailEditView;
import com.gestorcontactos.gestor_contactos.dto.ContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.ContactIdentityLookupView;
import com.gestorcontactos.gestor_contactos.dto.ContactListItem;
import com.gestorcontactos.gestor_contactos.dto.ContactPhoneEditView;
import com.gestorcontactos.gestor_contactos.dto.ContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.ContactPhoneView;
import com.gestorcontactos.gestor_contactos.dto.CreateContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateLegalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateNaturalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.EditContactRequest;
import com.gestorcontactos.gestor_contactos.dto.WarehouseContactDetailsView;
import com.gestorcontactos.gestor_contactos.dto.WarehouseContactListItem;
import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.Contact;
import com.gestorcontactos.gestor_contactos.entity.ContactStatus;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import com.gestorcontactos.gestor_contactos.entity.PhoneType;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import com.gestorcontactos.gestor_contactos.exception.ContactNotFoundException;
import com.gestorcontactos.gestor_contactos.security.CustomUserDetails;
import com.gestorcontactos.gestor_contactos.service.ContactService;
import com.gestorcontactos.gestor_contactos.service.ContactPhotoStorageService;
import com.gestorcontactos.gestor_contactos.service.UserService;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    private final ContactService contactService;
    private final ContactPhotoStorageService photoStorageService;
    private final UserService userService;

    public ContactController(ContactService contactService, ContactPhotoStorageService photoStorageService,
            UserService userService) {
        this.contactService = contactService;
        this.photoStorageService = photoStorageService;
        this.userService = userService;
    }

    @GetMapping("/contactos")
    public String listContacts(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User actor = authenticatedUser(currentUser);
        if (actor.getRole() == Role.ALMACENERO) {
            List<WarehouseContactListItem> contacts = warehouseContacts();
            model.addAttribute("contacts", contacts);
            model.addAttribute("restrictedView", true);
        } else {
            List<Contact> contacts = contactsForRole(actor.getRole());
            model.addAttribute("contacts", contacts.stream().map(this::toListItem).toList());
            model.addAttribute("restrictedView", false);
        }
        model.addAttribute("canSearch", actor.getRole() != Role.ALMACENERO);
        boolean canCreateContacts = actor.getRole() != Role.ALMACENERO;
        model.addAttribute("canCreateNatural", canCreateContacts);
        model.addAttribute("canCreateLegal", canCreateContacts);
        model.addAttribute("canViewDeletedContacts", actor.getRole() == Role.ADMINISTRADOR);
        return "contacts/list";
    }

    @GetMapping("/contactos/eliminados")
    public String listDeletedContacts(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        authenticatedUser(currentUser);
        model.addAttribute("contacts", contactService.findAllDeleted().stream().map(this::toListItem).toList());
        return "contacts/deleted-list";
    }

    @GetMapping("/contactos/nuevo/natural")
    public String naturalContactForm(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User actor = authenticatedUser(currentUser);
        model.addAttribute("form", emptyNaturalRequest(actor.getRole()));
        addAllowedCommercialTypes(model, actor.getRole());
        return "contacts/create-natural";
    }

    @PostMapping("/contactos/natural")
    public String createNaturalContact(@ModelAttribute("form") CreateNaturalContactRequest form,
            BindingResult bindingResult, @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Model model, RedirectAttributes redirectAttributes) {
        User actor = authenticatedUser(currentUser);
        if (bindingResult.hasErrors()) {
            addAllowedCommercialTypes(model, actor.getRole());
            return "contacts/create-natural";
        }
        try {
            Contact contact = photo == null ? contactService.createNaturalContact(form, actor)
                    : contactService.createNaturalContact(form, actor, photo);
            redirectAttributes.addFlashAttribute("success", "Contacto creado correctamente.");
            return "redirect:/contactos/" + contact.getId();
        } catch (ContactBusinessException exception) {
            bindingResult.reject("contactBusiness", exception.getMessage());
            addAllowedCommercialTypes(model, actor.getRole());
            return "contacts/create-natural";
        }
    }

    @GetMapping("/contactos/nuevo/juridica")
    public String legalContactForm(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User actor = authenticatedUser(currentUser);
        model.addAttribute("form", emptyLegalRequest(actor.getRole()));
        addAllowedCommercialTypes(model, actor.getRole());
        return "contacts/create-legal";
    }

    @PostMapping("/contactos/juridica")
    public String createLegalContact(@ModelAttribute("form") CreateLegalContactRequest form,
            BindingResult bindingResult, @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Model model, RedirectAttributes redirectAttributes) {
        User actor = authenticatedUser(currentUser);
        if (bindingResult.hasErrors()) {
            addAllowedCommercialTypes(model, actor.getRole());
            return "contacts/create-legal";
        }
        try {
            Contact contact = photo == null ? contactService.createLegalContact(form, actor)
                    : contactService.createLegalContact(form, actor, photo);
            redirectAttributes.addFlashAttribute("success", "Contacto creado correctamente.");
            return "redirect:/contactos/" + contact.getId();
        } catch (ContactBusinessException exception) {
            bindingResult.reject("contactBusiness", exception.getMessage());
            addAllowedCommercialTypes(model, actor.getRole());
            return "contacts/create-legal";
        }
    }

    @GetMapping("/contactos/buscar")
    public String findByDocument(@RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) String documentNumber,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model) {
        User actor = authenticatedUser(currentUser);
        model.addAttribute("crossRelationContact", false);
        model.addAttribute("searchedDocumentType", documentType);
        model.addAttribute("searchedDocumentNumber", documentNumber);
        if (documentType == null || documentNumber == null || documentNumber.isBlank()) {
            return "contacts/search-result";
        }
        Optional<Contact> result = contactService.findByIdentity(documentType, documentNumber);
        if (result.isEmpty()) {
            model.addAttribute("resultMessage", "No se encontró un contacto con el documento indicado.");
            return "contacts/search-result";
        }

        Contact contact = result.get();
        if (contact.getDeletedAt() != null) {
            model.addAttribute("resultMessage",
                    "La identidad ya está registrada y no se puede crear un contacto duplicado.");
            return "contacts/search-result";
        }
        if (actor.getRole() == Role.ADMINISTRADOR) {
            model.addAttribute("contact", toDetailsView(contact));
            model.addAttribute("restrictedView", false);
            model.addAttribute("canViewDetails", true);
        } else {
            CommercialType actorType = commercialTypeForRole(actor.getRole());
            CommercialType oppositeType = oppositeCommercialType(actorType);
            boolean active = contact.getStatus() == ContactStatus.ACTIVO;
            boolean withinRoleScope = active && actorType != null
                    && contact.getCommercialTypes().contains(actorType);
            boolean canAssociateCrossRelation = active && actorType != null && oppositeType != null
                    && !contact.getCommercialTypes().contains(actorType)
                    && contact.getCommercialTypes().contains(oppositeType);
            if (!withinRoleScope) {
                if (canAssociateCrossRelation) {
                    model.addAttribute("contact", toAssociationLookupView(contact));
                    model.addAttribute("restrictedView", true);
                    model.addAttribute("canViewDetails", false);
                    model.addAttribute("crossRelationContact", true);
                    addRelationActions(model, actor.getRole(), contact);
                    return "contacts/search-result";
                }
                model.addAttribute("resultMessage",
                        "La identidad ya está registrada y no se puede crear un contacto duplicado.");
                return "contacts/search-result";
            }
            model.addAttribute("contact", toIdentityLookupView(contact));
            model.addAttribute("restrictedView", true);
            model.addAttribute("canViewDetails", true);
        }
        addRelationActions(model, actor.getRole(), contact);
        return "contacts/search-result";
    }

    @GetMapping("/contactos/{id}")
    public String contactDetails(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User actor = authenticatedUser(currentUser);
        Contact contact = contactService.findById(id)
                .orElseThrow(() -> new ContactNotFoundException("No se encontró el contacto indicado."));
        boolean deleted = contact.getDeletedAt() != null;
        if (deleted && actor.getRole() != Role.ADMINISTRADOR) {
            throw new AccessDeniedException("No tiene permiso para consultar este contacto.");
        }
        requireReadPermission(actor.getRole(), contact);
        if (actor.getRole() == Role.ALMACENERO) {
            model.addAttribute("contact", toWarehouseDetails(contact));
            model.addAttribute("restrictedView", true);
        } else {
            model.addAttribute("contact", toDetailsView(contact));
            model.addAttribute("restrictedView", false);
            model.addAttribute("deletedContact", deleted);
            model.addAttribute("canEdit", !deleted);
            model.addAttribute("canChangeStatus", !deleted && actor.getRole() == Role.ADMINISTRADOR);
            model.addAttribute("canDelete", !deleted && actor.getRole() == Role.ADMINISTRADOR);
            model.addAttribute("canRecover", deleted && actor.getRole() == Role.ADMINISTRADOR);
            model.addAttribute("photoUrl", !deleted && contact.getPhotoPath() != null
                    ? "/contactos/" + id + "/foto" : null);
            model.addAttribute("showPhotoMissing", !deleted && contact.getPhotoPath() == null);
            model.addAttribute("isAdministrator", actor.getRole() == Role.ADMINISTRADOR);
            if (actor.getRole() == Role.ADMINISTRADOR) {
                model.addAttribute("createdAt", contact.getCreatedAt());
                model.addAttribute("createdBy", contact.getCreatedBy().getUsername());
                model.addAttribute("updatedAt", contact.getUpdatedAt());
                model.addAttribute("updatedBy", contact.getUpdatedBy() == null
                        ? null : contact.getUpdatedBy().getUsername());
                model.addAttribute("deletedAt", contact.getDeletedAt());
                model.addAttribute("deletedBy", contact.getDeletedBy() == null
                        ? null : contact.getDeletedBy().getUsername());
            }
            if (!deleted) {
                addRelationActions(model, actor.getRole(), contact);
            } else {
                model.addAttribute("canAddClient", false);
                model.addAttribute("canAddSupplier", false);
            }
        }
        return "contacts/detail";
    }

    @GetMapping("/contactos/{id}/editar")
    public String editContactForm(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User actor = authenticatedUser(currentUser);
        return populateEditModel(id, actor, model, null, null, null);
    }

    @PostMapping("/contactos/{id}/editar")
    public String updateContact(@PathVariable Long id,
            @ModelAttribute("form") EditContactRequest form, BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails currentUser, Model model,
            RedirectAttributes redirectAttributes) {
        User actor = authenticatedUser(currentUser);
        if (bindingResult.hasErrors()) {
            return populateEditModel(id, actor, model, form, null, null);
        }
        try {
            contactService.updateEditableData(id, form, actor);
            redirectAttributes.addFlashAttribute("success", "Datos del contacto actualizados correctamente.");
            return "redirect:/contactos/" + id + "/editar";
        } catch (ContactBusinessException exception) {
            bindingResult.reject("contactEdit", exception.getMessage());
            try {
                return populateEditModel(id, actor, model, form, null, null);
            } catch (ContactBusinessException missingContact) {
                redirectAttributes.addFlashAttribute("error", exception.getMessage());
                return "redirect:/contactos";
            }
        }
    }

    @PostMapping("/contactos/{id}/telefonos")
    public String addPhone(@PathVariable Long id, @ModelAttribute("phoneForm") ContactPhoneRequest form,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.addPhone(id, form, actor), "Teléfono agregado correctamente.");
    }

    @PostMapping("/contactos/{id}/telefonos/{phoneId}/editar")
    public String updatePhone(@PathVariable Long id, @PathVariable Long phoneId,
            @ModelAttribute("phoneForm") ContactPhoneRequest form,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.updatePhone(id, phoneId, form, actor), "Teléfono actualizado correctamente.");
    }

    @PostMapping("/contactos/{id}/telefonos/{phoneId}/eliminar")
    public String removePhone(@PathVariable Long id, @PathVariable Long phoneId,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.removePhone(id, phoneId, actor), "Teléfono eliminado correctamente.");
    }

    @PostMapping("/contactos/{id}/correos")
    public String addEmail(@PathVariable Long id, @ModelAttribute("emailForm") ContactEmailRequest form,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.addEmail(id, form, actor), "Correo agregado correctamente.");
    }

    @PostMapping("/contactos/{id}/correos/{emailId}/editar")
    public String updateEmail(@PathVariable Long id, @PathVariable Long emailId,
            @ModelAttribute("emailForm") ContactEmailRequest form,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.updateEmail(id, emailId, form, actor), "Correo actualizado correctamente.");
    }

    @PostMapping("/contactos/{id}/correos/{emailId}/eliminar")
    public String removeEmail(@PathVariable Long id, @PathVariable Long emailId,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.removeEmail(id, emailId, actor), "Correo eliminado correctamente.");
    }

    @GetMapping("/contactos/{id}/foto")
    public ResponseEntity<Resource> contactPhoto(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) throws IOException {
        User actor = authenticatedUser(currentUser);
        String photoPath = contactService.getPhotoPathForViewing(id, actor);
        if (photoPath == null) {
            return ResponseEntity.notFound().build();
        }
        Resource photo = photoStorageService.loadResource(photoPath);
        return ResponseEntity.ok()
                .contentType(photoStorageService.mediaType(photoPath))
                .cacheControl(CacheControl.noCache())
                .body(photo);
    }

    @PostMapping("/contactos/{id}/foto")
    public String replacePhoto(@PathVariable Long id,
            @RequestParam("photo") MultipartFile photo,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.replacePhoto(id, photo, actor), "Fotografía actualizada correctamente.");
    }

    @PostMapping("/contactos/{id}/foto/eliminar")
    public String removePhoto(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return applyContactChange(id, currentUser, redirectAttributes,
                actor -> contactService.removePhoto(id, actor), "Fotografía eliminada correctamente.");
    }

    @PostMapping("/contactos/{id}/estado")
    public String changeContactStatus(@PathVariable Long id, @RequestParam("status") String status,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        try {
            contactService.changeStatus(id, status, authenticatedUser(currentUser));
            redirectAttributes.addFlashAttribute("success", "Estado del contacto actualizado correctamente.");
        } catch (ContactBusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/contactos/" + id;
    }

    @PostMapping("/contactos/{id}/eliminar")
    public String deleteContact(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        try {
            contactService.softDelete(id, authenticatedUser(currentUser));
            redirectAttributes.addFlashAttribute("success", "Contacto eliminado correctamente.");
        } catch (ContactBusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/contactos";
    }

    @PostMapping("/contactos/{id}/recuperar")
    public String restoreContact(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        try {
            contactService.restore(id, authenticatedUser(currentUser));
            redirectAttributes.addFlashAttribute("success", "Contacto recuperado correctamente como INACTIVO.");
        } catch (ContactBusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/contactos/eliminados";
    }

    @PostMapping("/contactos/{id}/relaciones/cliente")
    public String addClientRelation(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return addCommercialRelation(id, CommercialType.CLIENTE, currentUser, redirectAttributes);
    }

    @PostMapping("/contactos/{id}/relaciones/proveedor")
    public String addSupplierRelation(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        return addCommercialRelation(id, CommercialType.PROVEEDOR, currentUser, redirectAttributes);
    }

    private String addCommercialRelation(Long id, CommercialType commercialType,
            CustomUserDetails currentUser, RedirectAttributes redirectAttributes) {
        try {
            contactService.addCommercialType(id, commercialType, authenticatedUser(currentUser));
            redirectAttributes.addFlashAttribute("success", "Relación comercial agregada correctamente.");
        } catch (ContactBusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/contactos";
    }

    private String applyContactChange(Long contactId, CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes, ContactChange operation, String successMessage) {
        try {
            operation.apply(authenticatedUser(currentUser));
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (ContactBusinessException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/contactos/" + contactId + "/editar";
    }

    private String populateEditModel(Long id, User actor, Model model,
            EditContactRequest form, ContactPhoneRequest phoneForm, ContactEmailRequest emailForm) {
        Contact contact = contactService.getContactForEditing(id, actor);
        model.addAttribute("contact", toEditView(contact));
        model.addAttribute("photoUrl", contact.getPhotoPath() == null ? null : "/contactos/" + id + "/foto");
        model.addAttribute("form", form == null ? new EditContactRequest(contact.getAddress()) : form);
        model.addAttribute("phoneForm", phoneForm == null
                ? new ContactPhoneRequest(PhoneType.CELULAR, "", "") : phoneForm);
        model.addAttribute("emailForm", emailForm == null ? new ContactEmailRequest("") : emailForm);
        return "contacts/edit";
    }

    private User authenticatedUser(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Se requiere autenticación.");
        }
        return userService.findByUsername(currentUser.getUsername())
                .filter(User::isEnabled)
                .orElseThrow(() -> new AccessDeniedException("La sesión no corresponde a un usuario habilitado."));
    }

    private List<Contact> contactsForRole(Role role) {
        return switch (role) {
            case ADMINISTRADOR -> contactService.findAllNotDeleted();
            case VENDEDOR -> activeContactsFor(CommercialType.CLIENTE);
            case COMPRAS -> activeContactsFor(CommercialType.PROVEEDOR);
            case ALMACENERO -> List.of();
        };
    }

    private List<Contact> activeContactsFor(CommercialType commercialType) {
        return contactService.findAllNotDeletedByCommercialType(commercialType).stream()
                .filter(contact -> contact.getStatus() == ContactStatus.ACTIVO)
                .toList();
    }

    private List<WarehouseContactListItem> warehouseContacts() {
        Map<Long, Contact> contacts = new LinkedHashMap<>();
        Stream.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR)
                .flatMap(type -> contactService.findAllNotDeletedByCommercialType(type).stream())
                .filter(contact -> contact.getStatus() == ContactStatus.ACTIVO)
                .forEach(contact -> contacts.putIfAbsent(contact.getId(), contact));
        return contacts.values().stream().map(this::toWarehouseListItem).toList();
    }

    private void requireReadPermission(Role role, Contact contact) {
        if (role == Role.ADMINISTRADOR) {
            return;
        }
        if (contact.getStatus() != ContactStatus.ACTIVO) {
            throw new AccessDeniedException("No tiene permiso para consultar este contacto.");
        }
        boolean allowed = switch (role) {
            case VENDEDOR -> contact.getCommercialTypes().contains(CommercialType.CLIENTE);
            case COMPRAS -> contact.getCommercialTypes().contains(CommercialType.PROVEEDOR);
            case ALMACENERO -> !contact.getCommercialTypes().isEmpty();
            case ADMINISTRADOR -> true;
        };
        if (!allowed) {
            throw new AccessDeniedException("No tiene permiso para consultar este contacto.");
        }
    }

    private void addRelationActions(Model model, Role role, Contact contact) {
        Set<CommercialType> existingTypes = contact.getCommercialTypes();
        model.addAttribute("canAddClient", role == Role.ADMINISTRADOR || role == Role.VENDEDOR);
        model.addAttribute("canAddSupplier", role == Role.ADMINISTRADOR || role == Role.COMPRAS);
        model.addAttribute("clientAlreadyAssigned", existingTypes.contains(CommercialType.CLIENTE));
        model.addAttribute("supplierAlreadyAssigned", existingTypes.contains(CommercialType.PROVEEDOR));
    }

    private ContactListItem toListItem(Contact contact) {
        return new ContactListItem(contact.getId(), contact.getPersonType(), displayName(contact),
                contact.getDocumentType(), contact.getDocumentNumber(), contact.getAddress(), contact.getStatus(),
                Set.copyOf(contact.getCommercialTypes()));
    }

    private WarehouseContactListItem toWarehouseListItem(Contact contact) {
        return new WarehouseContactListItem(contact.getId(), contact.getPersonType(), displayName(contact),
                contact.getAddress());
    }

    private ContactDetailsView toDetailsView(Contact contact) {
        List<ContactPhoneView> phones = contact.getPhones().stream()
                .map(phone -> new ContactPhoneView(phone.getPhoneType(), phone.getAreaCode(), phone.getPhoneNumber()))
                .toList();
        List<String> emails = contact.getEmails().stream().map(email -> email.getEmail()).toList();
        return new ContactDetailsView(contact.getId(), contact.getPersonType(), displayName(contact),
                contact.getDocumentType(), contact.getDocumentNumber(), contact.getAddress(), contact.getStatus(),
                Set.copyOf(contact.getCommercialTypes()), phones, emails);
    }

    private ContactEditView toEditView(Contact contact) {
        List<ContactPhoneEditView> phones = contact.getPhones().stream()
                .map(phone -> new ContactPhoneEditView(phone.getId(), phone.getPhoneType(),
                        phone.getAreaCode(), phone.getPhoneNumber()))
                .toList();
        List<ContactEmailEditView> emails = contact.getEmails().stream()
                .map(email -> new ContactEmailEditView(email.getId(), email.getEmail()))
                .toList();
        return new ContactEditView(contact.getId(), contact.getPersonType(), contact.getFirstNames(),
                contact.getLastNames(), contact.getLegalName(), contact.getDocumentType(),
                contact.getDocumentNumber(), contact.getAddress(), contact.getStatus(),
                Set.copyOf(contact.getCommercialTypes()), phones, emails);
    }

    private WarehouseContactDetailsView toWarehouseDetails(Contact contact) {
        return new WarehouseContactDetailsView(contact.getId(), contact.getPersonType(), displayName(contact),
                contact.getAddress());
    }

    private ContactIdentityLookupView toIdentityLookupView(Contact contact) {
        return new ContactIdentityLookupView(contact.getId(), contact.getPersonType(), displayName(contact),
                contact.getDocumentType(), contact.getDocumentNumber(), contact.getStatus(),
                Set.copyOf(contact.getCommercialTypes()));
    }

    private ContactAssociationLookupView toAssociationLookupView(Contact contact) {
        return new ContactAssociationLookupView(contact.getId(), contact.getPersonType(), displayName(contact));
    }

    private CommercialType commercialTypeForRole(Role role) {
        return switch (role) {
            case VENDEDOR -> CommercialType.CLIENTE;
            case COMPRAS -> CommercialType.PROVEEDOR;
            case ADMINISTRADOR, ALMACENERO -> null;
        };
    }

    private CommercialType oppositeCommercialType(CommercialType commercialType) {
        if (commercialType == null) {
            return null;
        }
        return commercialType == CommercialType.CLIENTE ? CommercialType.PROVEEDOR : CommercialType.CLIENTE;
    }

    private String displayName(Contact contact) {
        return contact.getPersonType() == PersonType.NATURAL
                ? contact.getFirstNames() + " " + contact.getLastNames()
                : contact.getLegalName();
    }

    private void addAllowedCommercialTypes(Model model, Role role) {
        List<CommercialType> types = switch (role) {
            case ADMINISTRADOR -> Arrays.asList(CommercialType.values());
            case VENDEDOR -> List.of(CommercialType.CLIENTE);
            case COMPRAS -> List.of(CommercialType.PROVEEDOR);
            case ALMACENERO -> List.of();
        };
        model.addAttribute("commercialTypes", types);
    }

    private CreateNaturalContactRequest emptyNaturalRequest(Role role) {
        return new CreateNaturalContactRequest("", "", DocumentType.DNI, "", "",
                List.of(new CreateContactPhoneRequest(PhoneType.CELULAR, "", "")),
                List.of(new CreateContactEmailRequest("")), initialCommercialTypes(role, CommercialType.CLIENTE));
    }

    private CreateLegalContactRequest emptyLegalRequest(Role role) {
        return new CreateLegalContactRequest("", DocumentType.RUC, "", "",
                List.of(new CreateContactPhoneRequest(PhoneType.CELULAR, "", "")),
                List.of(new CreateContactEmailRequest("")), initialCommercialTypes(role, CommercialType.PROVEEDOR));
    }

    private Set<CommercialType> initialCommercialTypes(Role role, CommercialType administratorDefault) {
        return switch (role) {
            case ADMINISTRADOR -> Set.of(administratorDefault);
            case VENDEDOR -> Set.of(CommercialType.CLIENTE);
            case COMPRAS -> Set.of(CommercialType.PROVEEDOR);
            case ALMACENERO -> Set.of();
        };
    }

    @FunctionalInterface
    private interface ContactChange {
        Contact apply(User actor);
    }
}
