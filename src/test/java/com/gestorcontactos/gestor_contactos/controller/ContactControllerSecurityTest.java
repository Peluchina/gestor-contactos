package com.gestorcontactos.gestor_contactos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.gestorcontactos.gestor_contactos.config.SecurityConfig;
import com.gestorcontactos.gestor_contactos.dto.CreateLegalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateNaturalContactRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactPhoneRequest;
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
import com.gestorcontactos.gestor_contactos.security.CustomUserDetails;
import com.gestorcontactos.gestor_contactos.security.CustomUserDetailsService;
import com.gestorcontactos.gestor_contactos.service.ContactService;
import com.gestorcontactos.gestor_contactos.service.ContactPhotoStorageService;
import com.gestorcontactos.gestor_contactos.service.UserService;
import com.gestorcontactos.gestor_contactos.validation.ContactValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentCaptor.forClass;

@WebMvcTest(controllers = {ContactController.class, DashboardController.class})
@Import(SecurityConfig.class)
class ContactControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private ContactPhotoStorageService photoStorageService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void unauthenticatedUserCannotOpenContactPages() throws Exception {
        mockMvc.perform(get("/contactos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void administratorCanCreateNaturalAndLegalContacts() throws Exception {
        User admin = actor("admin", Role.ADMINISTRADOR);
        mockNaturalCreation(admin, Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR));
        mockLegalCreation(admin, Set.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR));

        assertCreationFormDefaults("/contactos/nuevo/natural", admin, Set.of(CommercialType.CLIENTE),
                List.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR));
        assertCreationFormDefaults("/contactos/nuevo/juridica", admin, Set.of(CommercialType.PROVEEDOR),
                List.of(CommercialType.CLIENTE, CommercialType.PROVEEDOR));

        mockMvc.perform(post("/contactos/natural").with(user(new CustomUserDetails(admin))).with(csrf())
                        .params(validNaturalParameters()).param("commercialTypes", "CLIENTE", "PROVEEDOR"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/contactos/juridica").with(user(new CustomUserDetails(admin))).with(csrf())
                        .params(validLegalParameters()).param("commercialTypes", "CLIENTE", "PROVEEDOR"))
                .andExpect(status().is3xxRedirection());

        verify(contactService).createNaturalContact(any(CreateNaturalContactRequest.class), eq(admin));
        verify(contactService).createLegalContact(any(CreateLegalContactRequest.class), eq(admin));
    }

    @Test
    void sellerCanCreateNaturalAndLegalClients() throws Exception {
        User seller = actor("seller", Role.VENDEDOR);
        mockNaturalCreation(seller, Set.of(CommercialType.CLIENTE));
        mockLegalCreation(seller, Set.of(CommercialType.CLIENTE));

        assertCreationFormDefaults("/contactos/nuevo/natural", seller, Set.of(CommercialType.CLIENTE),
                List.of(CommercialType.CLIENTE));
        assertCreationFormDefaults("/contactos/nuevo/juridica", seller, Set.of(CommercialType.CLIENTE),
                List.of(CommercialType.CLIENTE));

        mockMvc.perform(post("/contactos/natural").with(user(new CustomUserDetails(seller))).with(csrf())
                        .params(validNaturalParameters()).param("commercialTypes", "CLIENTE"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/contactos/juridica").with(user(new CustomUserDetails(seller))).with(csrf())
                        .params(validLegalParameters()).param("commercialTypes", "CLIENTE"))
                .andExpect(status().is3xxRedirection());

        verify(contactService).createNaturalContact(any(CreateNaturalContactRequest.class), eq(seller));
        verify(contactService).createLegalContact(any(CreateLegalContactRequest.class), eq(seller));
    }

    @Test
    void naturalContactFormSubmitsMultiplePhonesAndEmails() throws Exception {
        User seller = actor("seller-multiple-values", Role.VENDEDOR);
        mockNaturalCreation(seller, Set.of(CommercialType.CLIENTE));

        mockMvc.perform(post("/contactos/natural").with(user(new CustomUserDetails(seller))).with(csrf())
                        .params(validNaturalParameters())
                        .param("phones[1].phoneType", "FIJO")
                        .param("phones[1].areaCode", "01")
                        .param("phones[1].phoneNumber", "4567890")
                        .param("emails[1].email", "second@example.com")
                        .param("commercialTypes", "CLIENTE"))
                .andExpect(status().is3xxRedirection());

        var requestCaptor = forClass(CreateNaturalContactRequest.class);
        verify(contactService).createNaturalContact(requestCaptor.capture(), eq(seller));
        assertEquals(2, requestCaptor.getValue().phones().size());
        assertEquals(2, requestCaptor.getValue().emails().size());
        assertEquals("second@example.com", requestCaptor.getValue().emails().get(1).email());
    }

    @Test
    void purchasingCanCreateNaturalAndLegalSuppliers() throws Exception {
        User buyer = actor("buyer", Role.COMPRAS);
        mockNaturalCreation(buyer, Set.of(CommercialType.PROVEEDOR));
        mockLegalCreation(buyer, Set.of(CommercialType.PROVEEDOR));

        assertCreationFormDefaults("/contactos/nuevo/natural", buyer, Set.of(CommercialType.PROVEEDOR),
                List.of(CommercialType.PROVEEDOR));
        assertCreationFormDefaults("/contactos/nuevo/juridica", buyer, Set.of(CommercialType.PROVEEDOR),
                List.of(CommercialType.PROVEEDOR));

        mockMvc.perform(post("/contactos/natural").with(user(new CustomUserDetails(buyer))).with(csrf())
                        .params(validNaturalParameters()).param("commercialTypes", "PROVEEDOR"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/contactos/juridica").with(user(new CustomUserDetails(buyer))).with(csrf())
                        .params(validLegalParameters()).param("commercialTypes", "PROVEEDOR"))
                .andExpect(status().is3xxRedirection());

        verify(contactService).createNaturalContact(any(CreateNaturalContactRequest.class), eq(buyer));
        verify(contactService).createLegalContact(any(CreateLegalContactRequest.class), eq(buyer));
    }

    @Test
    void dashboardAndContactListShowBothCreationOptionsForSellerAndPurchasingRoles() throws Exception {
        User seller = actor("seller-create-options", Role.VENDEDOR);
        mockNaturalCreation(seller, Set.of(CommercialType.CLIENTE));
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear cliente natural")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear cliente jurídico")));
        mockMvc.perform(get("/contactos").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear cliente natural")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear cliente jurídico")));

        User buyer = actor("buyer-create-options", Role.COMPRAS);
        mockNaturalCreation(buyer, Set.of(CommercialType.PROVEEDOR));
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(buyer))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear proveedor natural")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear proveedor jurídico")));
        mockMvc.perform(get("/contactos").with(user(new CustomUserDetails(buyer))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear proveedor natural")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear proveedor jurídico")));
    }

    @Test
    void commercialRoleRestrictionsRemainEnforcedByTheContactServiceForBothPersonTypes() {
        ContactService roleEnforcingService = new ContactService(
                org.mockito.Mockito.mock(ContactRepository.class), new ContactValidator(),
                org.mockito.Mockito.mock(ContactPhotoStorageService.class),
                java.time.Clock.system(java.time.ZoneId.of("America/Lima")));
        User seller = actor("seller-role-guard", Role.VENDEDOR);
        User buyer = actor("buyer-role-guard", Role.COMPRAS);

        assertThrows(ContactBusinessException.class,
                () -> roleEnforcingService.createNaturalContact(naturalRequest(Set.of(CommercialType.PROVEEDOR)), seller));
        assertThrows(ContactBusinessException.class,
                () -> roleEnforcingService.createLegalContact(legalRequest(Set.of(CommercialType.PROVEEDOR)), seller));
        assertThrows(ContactBusinessException.class,
                () -> roleEnforcingService.createNaturalContact(naturalRequest(Set.of(CommercialType.CLIENTE)), buyer));
        assertThrows(ContactBusinessException.class,
                () -> roleEnforcingService.createLegalContact(legalRequest(Set.of(CommercialType.CLIENTE)), buyer));
    }

    @Test
    void warehouseRoleCannotOpenCreationFormsOrPostCreation() throws Exception {
        User warehouse = actor("warehouse", Role.ALMACENERO);

        mockMvc.perform(get("/contactos/nuevo/natural").with(user(new CustomUserDetails(warehouse))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/contactos/nuevo/juridica").with(user(new CustomUserDetails(warehouse))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/contactos/natural").with(user(new CustomUserDetails(warehouse))).with(csrf())
                        .params(validNaturalParameters()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/contactos/juridica").with(user(new CustomUserDetails(warehouse))).with(csrf())
                        .params(validLegalParameters()))
                .andExpect(status().isForbidden());

        verify(contactService, never()).createNaturalContact(any(CreateNaturalContactRequest.class), any(User.class));
        verify(contactService, never()).createLegalContact(any(CreateLegalContactRequest.class), any(User.class));
    }

    @Test
    void sellerCanAddClientButCannotAddSupplier() throws Exception {
        User seller = actor("seller", Role.VENDEDOR);
        mockMvc.perform(post("/contactos/41/relaciones/cliente").with(user(new CustomUserDetails(seller))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos"));
        mockMvc.perform(post("/contactos/41/relaciones/proveedor")
                        .with(user(new CustomUserDetails(seller))).with(csrf()))
                .andExpect(status().isForbidden());

        verify(contactService).addCommercialType(41L, CommercialType.CLIENTE, seller);
        verify(contactService, never()).addCommercialType(41L, CommercialType.PROVEEDOR, seller);
    }

    @Test
    void purchasingCanAddSupplierButCannotAddClient() throws Exception {
        User buyer = actor("buyer", Role.COMPRAS);
        mockMvc.perform(post("/contactos/42/relaciones/proveedor").with(user(new CustomUserDetails(buyer))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos"));
        mockMvc.perform(post("/contactos/42/relaciones/cliente")
                        .with(user(new CustomUserDetails(buyer))).with(csrf()))
                .andExpect(status().isForbidden());

        verify(contactService).addCommercialType(42L, CommercialType.PROVEEDOR, buyer);
        verify(contactService, never()).addCommercialType(42L, CommercialType.CLIENTE, buyer);
    }

    @Test
    void administratorCanAddEitherCommercialRelation() throws Exception {
        User admin = actor("admin", Role.ADMINISTRADOR);

        mockMvc.perform(post("/contactos/44/relaciones/cliente").with(user(new CustomUserDetails(admin))).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/contactos/44/relaciones/proveedor")
                        .with(user(new CustomUserDetails(admin))).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(contactService).addCommercialType(44L, CommercialType.CLIENTE, admin);
        verify(contactService).addCommercialType(44L, CommercialType.PROVEEDOR, admin);
    }

    @Test
    void warehouseRoleCannotAddEitherCommercialRelation() throws Exception {
        User warehouse = actor("warehouse", Role.ALMACENERO);
        mockMvc.perform(post("/contactos/43/relaciones/cliente")
                        .with(user(new CustomUserDetails(warehouse))).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/contactos/43/relaciones/proveedor")
                        .with(user(new CustomUserDetails(warehouse))).with(csrf()))
                .andExpect(status().isForbidden());

        verify(contactService, never()).addCommercialType(eq(43L), any(CommercialType.class), any(User.class));
    }

    @Test
    void contactFormsIncludeCsrfAndPostRejectsMissingToken() throws Exception {
        User seller = actor("seller", Role.VENDEDOR);

        mockMvc.perform(get("/contactos/nuevo/natural").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/create-natural"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("multipart/form-data")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("phones[0].phoneNumber")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("emails[0].email")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("contact-collections.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(post("/contactos/natural").with(user(new CustomUserDetails(seller)))
                        .params(validNaturalParameters()).param("commercialTypes", "CLIENTE"))
                .andExpect(status().isForbidden());
        verify(contactService, never()).createNaturalContact(any(CreateNaturalContactRequest.class), any(User.class));
    }

    @Test
    void contactEditFormIsProtectedAndRendersIdentityAsReadonlyWithCsrf() throws Exception {
        User seller = actor("seller-edit", Role.VENDEDOR);
        Contact contact = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), seller);
        contact.addCommercialType(CommercialType.CLIENTE);
        contact.addPhone(new com.gestorcontactos.gestor_contactos.entity.ContactPhone(
                com.gestorcontactos.gestor_contactos.entity.PhoneType.CELULAR, "", "987654321"));
        contact.addEmail(new com.gestorcontactos.gestor_contactos.entity.ContactEmail("maria@example.com"));
        when(contactService.getContactForEditing(51L, seller)).thenReturn(contact);

        mockMvc.perform(get("/contactos/51/editar").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/edit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("readonly")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("987654321")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("maria@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(get("/contactos/51/editar").with(user(new CustomUserDetails(actor("warehouse-edit", Role.ALMACENERO)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void editPostRequiresCsrfAndIgnoresIdentityFieldsSentByClient() throws Exception {
        User seller = actor("seller-update", Role.VENDEDOR);
        Contact contact = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), seller);
        contact.addCommercialType(CommercialType.CLIENTE);
        when(contactService.getContactForEditing(52L, seller)).thenReturn(contact);

        mockMvc.perform(post("/contactos/52/editar").with(user(new CustomUserDetails(seller)))
                        .param("address", "Jr. Nueva 456")
                        .param("firstNames", "Manipulado")
                        .param("documentNumber", "87654321"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/contactos/52/editar").with(user(new CustomUserDetails(seller))).with(csrf())
                        .param("address", "Jr. Nueva 456")
                        .param("firstNames", "Manipulado")
                        .param("documentNumber", "87654321"))
                .andExpect(status().is3xxRedirection());

        var requestCaptor = forClass(EditContactRequest.class);
        verify(contactService).updateEditableData(eq(52L), requestCaptor.capture(), eq(seller));
        assertEquals(new EditContactRequest("Jr. Nueva 456"), requestCaptor.getValue());
    }

    @Test
    void warehouseCannotPostContactEditsEvenWithCsrf() throws Exception {
        User warehouse = actor("warehouse-update", Role.ALMACENERO);
        mockMvc.perform(post("/contactos/53/telefonos").with(user(new CustomUserDetails(warehouse))).with(csrf())
                        .param("phoneType", "CELULAR").param("areaCode", "").param("phoneNumber", "987654321"))
                .andExpect(status().isForbidden());
        verify(contactService, never()).addPhone(eq(53L), any(), any(User.class));
    }

    @Test
    void photoEndpointsRequireCsrfAndRejectWarehouseRole() throws Exception {
        User administrator = actor("admin-photo", Role.ADMINISTRADOR);
        User warehouse = actor("warehouse-photo", Role.ALMACENERO);
        org.springframework.mock.web.MockMultipartFile photo = new org.springframework.mock.web.MockMultipartFile(
                "photo", "valid.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/contactos/61/foto").file(photo)
                        .with(user(new CustomUserDetails(administrator))))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/contactos/61/foto").file(photo)
                        .with(user(new CustomUserDetails(warehouse))).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/contactos/61/foto").file(photo)
                        .with(user(new CustomUserDetails(administrator))).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(contactService).replacePhoto(eq(61L), any(org.springframework.web.multipart.MultipartFile.class),
                eq(administrator));
        verify(contactService, never()).replacePhoto(eq(61L), any(), eq(warehouse));
    }

    @Test
    void photoResourceRequiresAuthenticationAndUsesStoredImageReference() throws Exception {
        User seller = actor("seller-photo-view", Role.VENDEDOR);
        String photoPath = "uploads/contacts/00000000-0000-0000-0000-000000000001.png";
        when(contactService.getPhotoPathForViewing(64L, seller)).thenReturn(photoPath);
        when(photoStorageService.loadResource(photoPath))
                .thenReturn(new org.springframework.core.io.ByteArrayResource(new byte[] {1, 2, 3}));
        when(photoStorageService.mediaType(photoPath)).thenReturn(org.springframework.http.MediaType.IMAGE_PNG);

        mockMvc.perform(get("/contactos/64/foto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/contactos/64/foto").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void contactDetailShowsPhotoOnlyToRolesWithFullContactView() throws Exception {
        User seller = actor("seller-detail-photo", Role.VENDEDOR);
        Contact contact = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), seller);
        contact.setPhotoPath("uploads/contacts/00000000-0000-0000-0000-000000000002.jpg");
        contact.addCommercialType(CommercialType.CLIENTE);
        when(contactService.findById(65L)).thenReturn(Optional.of(contact));

        mockMvc.perform(get("/contactos/65").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/contactos/65/foto")));

        User warehouse = actor("warehouse-detail-photo", Role.ALMACENERO);
        mockMvc.perform(get("/contactos/65").with(user(new CustomUserDetails(warehouse))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<img"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("foto"))));
    }

    @Test
    void authenticatedActorCanCreateContactWithOptionalPhoto() throws Exception {
        User admin = actor("admin-create-photo", Role.ADMINISTRADOR);
        Contact created = org.mockito.Mockito.mock(Contact.class);
        when(created.getId()).thenReturn(62L);
        when(contactService.createNaturalContact(any(CreateNaturalContactRequest.class), eq(admin),
                any(org.springframework.web.multipart.MultipartFile.class))).thenReturn(created);
        org.springframework.mock.web.MockMultipartFile photo = new org.springframework.mock.web.MockMultipartFile(
                "photo", "image.png", "image/png", new byte[] {1, 2, 3});
        org.springframework.mock.web.MockMultipartFile form = new org.springframework.mock.web.MockMultipartFile(
                "form", "", "text/plain", "".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/contactos/natural").file(photo).file(form).with(csrf())
                        .with(user(new CustomUserDetails(admin)))
                        .param("firstNames", "María").param("lastNames", "Pérez")
                        .param("documentType", "DNI").param("documentNumber", "12345678")
                        .param("address", "Av. Perú 123")
                        .param("phones[0].phoneType", "CELULAR").param("phones[0].areaCode", "")
                        .param("phones[0].phoneNumber", "987654321")
                        .param("emails[0].email", "maria@example.com")
                        .param("commercialTypes", "CLIENTE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos/62"));

        verify(contactService).createNaturalContact(any(CreateNaturalContactRequest.class), eq(admin),
                any(org.springframework.web.multipart.MultipartFile.class));
    }

    @Test
    void statusEndpointRequiresCsrfAndOnlyAdministratorCanUseIt() throws Exception {
        User admin = actor("admin-status", Role.ADMINISTRADOR);
        User seller = actor("seller-status", Role.VENDEDOR);
        User purchasing = actor("purchasing-status", Role.COMPRAS);
        User warehouse = actor("warehouse-status", Role.ALMACENERO);

        mockMvc.perform(post("/contactos/71/estado").with(user(new CustomUserDetails(admin)))
                        .param("status", "INACTIVO"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/contactos/71/estado").with(user(new CustomUserDetails(admin))).with(csrf())
                        .param("status", "INACTIVO").param("updatedBy", "999")
                        .param("updatedAt", "2099-01-01T00:00:00").param("role", "ADMINISTRADOR")
                        .param("documentNumber", "87654321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos/71"));

        for (User denied : List.of(seller, purchasing, warehouse)) {
            mockMvc.perform(post("/contactos/71/estado").with(user(new CustomUserDetails(denied))).with(csrf())
                            .param("status", "INACTIVO"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/contactos/71/estado").with(user(new CustomUserDetails(denied))).with(csrf())
                            .param("status", "ACTIVO"))
                    .andExpect(status().isForbidden());
        }

        verify(contactService).changeStatus(71L, "INACTIVO", admin);
        verify(contactService, never()).changeStatus(eq(71L), any(String.class), eq(seller));
        verify(contactService, never()).changeStatus(eq(71L), any(String.class), eq(purchasing));
        verify(contactService, never()).changeStatus(eq(71L), any(String.class), eq(warehouse));
    }

    @Test
    void onlyAdministratorSeesTheStatusTransitionForm() throws Exception {
        User admin = actor("admin-status-view", Role.ADMINISTRADOR);
        User seller = actor("seller-status-view", Role.VENDEDOR);
        Contact contact = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), admin);
        contact.addCommercialType(CommercialType.CLIENTE);
        when(contactService.findById(72L)).thenReturn(Optional.of(contact));
        when(contactService.findById(73L)).thenReturn(Optional.of(contact));

        mockMvc.perform(get("/contactos/72").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"status\" value=\"INACTIVO\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Estado:")));

        contact.setStatus(ContactStatus.INACTIVO);
        mockMvc.perform(get("/contactos/73").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"status\" value=\"ACTIVO\"")));

        contact.setStatus(ContactStatus.ACTIVO);
        mockMvc.perform(get("/contactos/72").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"status\""))));
    }

    @Test
    void formCannotSpoofTheCreatorOrRole() throws Exception {
        User seller = actor("seller", Role.VENDEDOR);
        mockNaturalCreation(seller, Set.of(CommercialType.CLIENTE));

        mockMvc.perform(post("/contactos/natural").with(user(new CustomUserDetails(seller))).with(csrf())
                        .params(validNaturalParameters())
                        .param("commercialTypes", "CLIENTE")
                        .param("createdBy", "999")
                        .param("role", "ADMINISTRADOR"))
                .andExpect(status().is3xxRedirection());

        verify(contactService).createNaturalContact(any(CreateNaturalContactRequest.class), eq(seller));
    }

    @Test
    void warehouseListDoesNotRenderDocumentPhonesOrEmails() throws Exception {
        User warehouse = actor("warehouse", Role.ALMACENERO);
        Contact contact = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), warehouse);
        contact.addCommercialType(CommercialType.CLIENTE);
        when(contactService.findAllNotDeletedByCommercialType(CommercialType.CLIENTE)).thenReturn(java.util.List.of(contact));
        when(contactService.findAllNotDeletedByCommercialType(CommercialType.PROVEEDOR)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/contactos").with(user(new CustomUserDetails(warehouse))))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("María Pérez")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Av. Perú 123")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ACTIVO")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("12345678"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Documento"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Teléfonos"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Correos"))));
    }

    @Test
    void warehouseDetailContainsOnlyNameAndAddress() throws Exception {
        User warehouse = actor("warehouse-detail-private", Role.ALMACENERO);
        Contact contact = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), warehouse);
        contact.addCommercialType(CommercialType.CLIENTE);
        contact.addPhone(new com.gestorcontactos.gestor_contactos.entity.ContactPhone(
                com.gestorcontactos.gestor_contactos.entity.PhoneType.CELULAR, "", "987654321"));
        contact.addEmail(new com.gestorcontactos.gestor_contactos.entity.ContactEmail("private@example.com"));
        contact.setPhotoPath("uploads/contacts/private.jpg");
        when(contactService.findById(87L)).thenReturn(Optional.of(contact));

        mockMvc.perform(get("/contactos/87").with(user(new CustomUserDetails(warehouse))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("María Pérez")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Av. Perú 123")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("12345678"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("987654321"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private@example.com"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Auditoría"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<img"))));
    }

    @Test
    void roleSpecificContactListUsesOnlyAllowedCommercialType() throws Exception {
        User seller = actor("seller", Role.VENDEDOR);
        when(contactService.findAllNotDeletedByCommercialType(CommercialType.CLIENTE)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/contactos").with(user(new CustomUserDetails(seller))))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/list"));

        verify(contactService).findAllNotDeletedByCommercialType(CommercialType.CLIENTE);
        verify(contactService, never()).findAllNotDeletedByCommercialType(CommercialType.PROVEEDOR);
    }

    @Test
    void administratorCanDeleteAndRestoreWithCsrfAndAuthenticatedActor() throws Exception {
        User admin = actor("admin-delete", Role.ADMINISTRADOR);

        mockMvc.perform(post("/contactos/81/eliminar").with(user(new CustomUserDetails(admin))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos"));
        mockMvc.perform(post("/contactos/81/recuperar").with(user(new CustomUserDetails(admin))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos/eliminados"));

        verify(contactService).softDelete(81L, admin);
        verify(contactService).restore(81L, admin);
    }

    @Test
    void deleteAndRestoreRequireCsrfToken() throws Exception {
        User admin = actor("admin-csrf", Role.ADMINISTRADOR);

        mockMvc.perform(post("/contactos/82/eliminar").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/contactos/82/recuperar").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isForbidden());

        verify(contactService, never()).softDelete(eq(82L), any(User.class));
        verify(contactService, never()).restore(eq(82L), any(User.class));
    }

    @Test
    void onlyAdministratorCanDeleteRestoreAndViewDeletedContacts() throws Exception {
        for (Role role : List.of(Role.VENDEDOR, Role.COMPRAS, Role.ALMACENERO)) {
            User actor = actor("role-" + role, role);
            mockMvc.perform(post("/contactos/83/eliminar").with(user(new CustomUserDetails(actor))).with(csrf()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/contactos/83/recuperar").with(user(new CustomUserDetails(actor))).with(csrf()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/contactos/eliminados").with(user(new CustomUserDetails(actor))))
                    .andExpect(status().isForbidden());
        }
        verify(contactService, never()).softDelete(eq(83L), any(User.class));
        verify(contactService, never()).restore(eq(83L), any(User.class));
        verify(contactService, never()).findAllDeleted();
    }

    @Test
    void administratorCanSeeDeletedListAndRestoreForm() throws Exception {
        User admin = actor("admin-deleted-list", Role.ADMINISTRADOR);
        Contact deleted = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), admin);
        setEntityId(deleted, 85L);
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactService.findAllDeleted()).thenReturn(List.of(deleted));

        mockMvc.perform(get("/contactos/eliminados").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/deleted-list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("María Pérez")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ELIMINADO (lógico)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void administratorCanViewDeletedContactAndOnlyRestoreActionIsAvailable() throws Exception {
        User admin = actor("admin-deleted-detail", Role.ADMINISTRADOR);
        Contact deleted = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.of(2026, 11, 27, 14, 30), admin);
        setEntityId(deleted, 84L);
        deleted.setDeletedAt(LocalDateTime.of(2026, 11, 27, 14, 30));
        deleted.setDeletedBy(admin);
        when(contactService.findById(84L)).thenReturn(Optional.of(deleted));

        mockMvc.perform(get("/contactos/84").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Contacto eliminado lógicamente.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Auditoría")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("27/11/2026 14:30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-deleted-detail")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/contactos/84/recuperar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/contactos/84/eliminar"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/contactos/84/editar"))));
    }

    @Test
    void sellerCanSearchClientsAndOnlyMinimalDetailsOfSupplierForCrossRelation() throws Exception {
        User seller = actor("seller-search", Role.VENDEDOR);
        Contact client = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), seller);
        client.addCommercialType(CommercialType.CLIENTE);
        setEntityId(client, 86L);
        when(contactService.findByIdentity(DocumentType.DNI, "12345678")).thenReturn(Optional.of(client));

        mockMvc.perform(get("/contactos/buscar").with(user(new CustomUserDetails(seller)))
                        .param("documentType", "DNI").param("documentNumber", "12345678"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("María Pérez")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/contactos/86")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"12345678\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/contactos/86/relaciones/cliente"))));

        Contact supplier = new Contact(PersonType.NATURAL, "Persona privada", "Proveedor", DocumentType.DNI,
                "87654321", "Av. Privada 1", LocalDateTime.now(), seller);
        supplier.addCommercialType(CommercialType.PROVEEDOR);
        when(contactService.findByIdentity(DocumentType.DNI, "87654321")).thenReturn(Optional.of(supplier));
        mockMvc.perform(get("/contactos/buscar").with(user(new CustomUserDetails(seller)))
                        .param("documentType", "DNI").param("documentNumber", "87654321"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Persona privada Proveedor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Agregar como cliente")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Documento:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Relaciones:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Av. Privada 1"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("87654321"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/contactos/87654321"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void sellerCanSubmitCrossRelationForExistingSupplierWithoutCreatingAnotherContact() throws Exception {
        User seller = actor("seller-cross-relation", Role.VENDEDOR);
        Contact supplier = new Contact(PersonType.NATURAL, "Persona privada", "Proveedor", DocumentType.DNI,
                "87654321", "Av. Privada 1", LocalDateTime.now(), seller);
        supplier.addCommercialType(CommercialType.PROVEEDOR);
        setEntityId(supplier, 87L);
        when(userService.findByUsername(seller.getUsername())).thenReturn(Optional.of(seller));
        when(contactService.addCommercialType(87L, CommercialType.CLIENTE, seller)).thenReturn(supplier);

        mockMvc.perform(post("/contactos/87/relaciones/cliente")
                        .with(user(new CustomUserDetails(seller))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos"));

        verify(contactService).addCommercialType(87L, CommercialType.CLIENTE, seller);
        verify(contactService, never()).createNaturalContact(any(CreateNaturalContactRequest.class), eq(seller));
    }

    @Test
    void purchasingCanSearchAndSubmitCrossRelationForExistingClientWithMinimalDetails() throws Exception {
        User buyer = actor("buyer-cross-relation", Role.COMPRAS);
        Contact client = new Contact(PersonType.JURIDICA, "Empresa confidencial S.A.C.", DocumentType.RUC,
                "20123456789", "Av. Reservada 123", LocalDateTime.now(), buyer);
        client.addCommercialType(CommercialType.CLIENTE);
        setEntityId(client, 88L);
        when(userService.findByUsername(buyer.getUsername())).thenReturn(Optional.of(buyer));
        when(contactService.findByIdentity(DocumentType.RUC, "20123456789")).thenReturn(Optional.of(client));
        when(contactService.addCommercialType(88L, CommercialType.PROVEEDOR, buyer)).thenReturn(client);

        mockMvc.perform(get("/contactos/buscar").with(user(new CustomUserDetails(buyer)))
                        .param("documentType", "RUC").param("documentNumber", "20123456789"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Empresa confidencial S.A.C.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Agregar como proveedor")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Documento:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Relaciones:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Av. Reservada 123"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("20123456789"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Consultar detalle"))));

        mockMvc.perform(post("/contactos/88/relaciones/proveedor")
                        .with(user(new CustomUserDetails(buyer))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contactos"));

        verify(contactService).addCommercialType(88L, CommercialType.PROVEEDOR, buyer);
        verify(contactService, never()).createLegalContact(any(CreateLegalContactRequest.class), eq(buyer));
    }

    @Test
    void deletedContactSearchDoesNotOfferCrossRelation() throws Exception {
        User seller = actor("seller-deleted-cross", Role.VENDEDOR);
        Contact deletedSupplier = new Contact(PersonType.NATURAL, "Nombre reservado", "Proveedor", DocumentType.DNI,
                "87654321", "Av. Privada 1", LocalDateTime.now(), seller);
        deletedSupplier.addCommercialType(CommercialType.PROVEEDOR);
        deletedSupplier.setDeletedAt(LocalDateTime.now());
        when(userService.findByUsername(seller.getUsername())).thenReturn(Optional.of(seller));
        when(contactService.findByIdentity(DocumentType.DNI, "87654321")).thenReturn(Optional.of(deletedSupplier));

        mockMvc.perform(get("/contactos/buscar").with(user(new CustomUserDetails(seller)))
                        .param("documentType", "DNI").param("documentNumber", "87654321"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "La identidad ya está registrada y no se puede crear un contacto duplicado.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Nombre reservado"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Agregar como cliente"))));
    }

    @Test
    void purchasingSearchDoesNotOfferAlreadyAssignedSupplierRelation() throws Exception {
        User buyer = actor("buyer-existing-supplier", Role.COMPRAS);
        Contact supplier = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), buyer);
        supplier.addCommercialType(CommercialType.PROVEEDOR);
        setEntityId(supplier, 89L);
        when(userService.findByUsername(buyer.getUsername())).thenReturn(Optional.of(buyer));
        when(contactService.findByIdentity(DocumentType.DNI, "12345678")).thenReturn(Optional.of(supplier));

        mockMvc.perform(get("/contactos/buscar").with(user(new CustomUserDetails(buyer)))
                        .param("documentType", "DNI").param("documentNumber", "12345678"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("María Pérez")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/contactos/89/relaciones/proveedor"))));

        verify(contactService, never()).addCommercialType(89L, CommercialType.PROVEEDOR, buyer);
    }

    private void mockNaturalCreation(User actor, Set<CommercialType> commercialTypes) {
        when(userService.findByUsername(actor.getUsername())).thenReturn(Optional.of(actor));
        Contact created = org.mockito.Mockito.mock(Contact.class);
        when(created.getId()).thenReturn(100L);
        when(contactService.createNaturalContact(any(CreateNaturalContactRequest.class), eq(actor)))
                .thenAnswer(invocation -> {
                    CreateNaturalContactRequest request = invocation.getArgument(0);
                    assertEquals(commercialTypes, request.commercialTypes());
                    return created;
                });
    }

    private void assertCreationFormDefaults(String path, User actor, Set<CommercialType> expectedDefaults,
            List<CommercialType> expectedOptions) throws Exception {
        var result = mockMvc.perform(get(path).with(user(new CustomUserDetails(actor))))
                .andExpect(status().isOk())
                .andReturn();
        Object form = result.getModelAndView().getModel().get("form");
        Set<CommercialType> actualDefaults;
        if (form instanceof CreateNaturalContactRequest naturalForm) {
            actualDefaults = naturalForm.commercialTypes();
        } else {
            actualDefaults = ((CreateLegalContactRequest) form).commercialTypes();
        }

        assertEquals(expectedDefaults, actualDefaults);
        assertEquals(expectedOptions, result.getModelAndView().getModel().get("commercialTypes"));
    }

    private CreateNaturalContactRequest naturalRequest(Set<CommercialType> commercialTypes) {
        return new CreateNaturalContactRequest("María", "Pérez", DocumentType.DNI, "12345678", "Av. Perú 123",
                List.of(new CreateContactPhoneRequest(PhoneType.CELULAR, "", "987654321")),
                List.of(new CreateContactEmailRequest("maria@example.com")), commercialTypes);
    }

    private CreateLegalContactRequest legalRequest(Set<CommercialType> commercialTypes) {
        return new CreateLegalContactRequest("Empresa S.A.C.", DocumentType.RUC, "20123456789", "Av. Perú 123",
                List.of(new CreateContactPhoneRequest(PhoneType.CELULAR, "", "987654321")),
                List.of(new CreateContactEmailRequest("empresa@example.com")), commercialTypes);
    }

    private void mockLegalCreation(User actor, Set<CommercialType> commercialTypes) {
        when(userService.findByUsername(actor.getUsername())).thenReturn(Optional.of(actor));
        Contact created = org.mockito.Mockito.mock(Contact.class);
        when(created.getId()).thenReturn(101L);
        when(contactService.createLegalContact(any(CreateLegalContactRequest.class), eq(actor)))
                .thenAnswer(invocation -> {
                    CreateLegalContactRequest request = invocation.getArgument(0);
                    assertEquals(commercialTypes, request.commercialTypes());
                    return created;
                });
    }

    private User actor(String username, Role role) {
        User actor = new User(username, "hash", role, true, LocalDateTime.now());
        when(userService.findByUsername(username)).thenReturn(Optional.of(actor));
        return actor;
    }

    private void setEntityId(Contact entity, Long id) {
        try {
            var field = Contact.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to prepare persisted contact fixture", exception);
        }
    }

    private org.springframework.util.MultiValueMap<String, String> validNaturalParameters() {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("firstNames", "María");
        params.add("lastNames", "Pérez");
        params.add("documentType", "DNI");
        params.add("documentNumber", "12345678");
        params.add("address", "Av. Perú 123");
        params.add("phones[0].phoneType", "CELULAR");
        params.add("phones[0].areaCode", "");
        params.add("phones[0].phoneNumber", "987654321");
        params.add("emails[0].email", "maria@example.com");
        return params;
    }

    private org.springframework.util.MultiValueMap<String, String> validLegalParameters() {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("legalName", "Empresa 123 S.A.C.");
        params.add("documentType", "RUC");
        params.add("documentNumber", "20123456789");
        params.add("address", "Av. Perú 123");
        params.add("phones[0].phoneType", "CELULAR");
        params.add("phones[0].areaCode", "");
        params.add("phones[0].phoneNumber", "987654321");
        params.add("emails[0].email", "empresa@example.com");
        return params;
    }
}
