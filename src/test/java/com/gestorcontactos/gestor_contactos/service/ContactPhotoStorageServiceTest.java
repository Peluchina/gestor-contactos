package com.gestorcontactos.gestor_contactos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestorcontactos.gestor_contactos.dto.CreateContactEmailRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateContactPhoneRequest;
import com.gestorcontactos.gestor_contactos.dto.CreateNaturalContactRequest;
import com.gestorcontactos.gestor_contactos.entity.CommercialType;
import com.gestorcontactos.gestor_contactos.entity.Contact;
import com.gestorcontactos.gestor_contactos.entity.DocumentType;
import com.gestorcontactos.gestor_contactos.entity.PersonType;
import com.gestorcontactos.gestor_contactos.entity.PhoneType;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import com.gestorcontactos.gestor_contactos.repository.ContactRepository;
import com.gestorcontactos.gestor_contactos.validation.ContactPhotoValidator;
import com.gestorcontactos.gestor_contactos.validation.ContactValidator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ContactPhotoStorageServiceTest {

    private static final String WEBP_1X1 = "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA";

    @TempDir
    Path tempDirectory;

    @Mock
    private ContactRepository contactRepository;

    private ContactPhotoValidator photoValidator;
    private ContactPhotoStorageService storageService;
    private ContactService contactService;
    private User administrator;

    @BeforeEach
    void setUp() {
        photoValidator = new ContactPhotoValidator();
        storageService = new ContactPhotoStorageService(photoValidator, tempDirectory);
        contactService = new ContactService(contactRepository, new ContactValidator(), storageService);
        administrator = user("admin", Role.ADMINISTRADOR);
    }

    @Test
    void acceptsJpegPngAndWebpAndStoresOnlyGeneratedNames() throws Exception {
        String jpegPath = storageService.store(upload("../../evil.jpg", "application/pdf", image("jpeg", 20, 10)));
        String jpegLongExtensionPath = storageService.store(
                upload("photo.JPEG", "image/jpeg", image("jpeg", 20, 10)));
        String pngPath = storageService.store(upload("C:\\fakepath\\photo.png", "text/plain", image("png", 20, 10)));
        String webpPath = storageService.store(upload("photo.webp", "image/jpeg", java.util.Base64.getDecoder().decode(WEBP_1X1)));

        assertTrue(jpegPath.matches("uploads/contacts/[0-9a-f-]{36}\\.jpg"));
        assertTrue(pngPath.matches("uploads/contacts/[0-9a-f-]{36}\\.png"));
        assertTrue(jpegLongExtensionPath.matches("uploads/contacts/[0-9a-f-]{36}\\.jpg"));
        assertTrue(webpPath.matches("uploads/contacts/[0-9a-f-]{36}\\.png"));
        assertTrue(Files.isRegularFile(resolve(jpegPath)));
        assertTrue(Files.isRegularFile(resolve(pngPath)));
        assertTrue(Files.isRegularFile(resolve(webpPath)));
        assertEquals("image/png", storageService.mediaType(webpPath).toString());
        assertNotNull(storageService.loadResource(webpPath));
    }

    @Test
    void acceptsImagesAtOrBelowTwoMegabytesAndRejectsLargerFiles() throws Exception {
        byte[] valid = image("png", 1, 1);
        assertTrue(storageService.loadResource(storageService.store(upload("tiny.png", "image/png", valid))).exists());

        byte[] oversized = new byte[(int) ContactPhotoValidator.MAX_FILE_SIZE + 1];
        assertThrows(ContactBusinessException.class,
                () -> storageService.store(upload("large.png", "image/png", oversized)));
    }

    @Test
    void rejectsUnsupportedEmptyCorruptAndMismatchedContent() throws Exception {
        assertThrows(ContactBusinessException.class,
                () -> storageService.store(upload("photo.gif", "image/gif", image("png", 1, 1))));
        assertThrows(ContactBusinessException.class,
                () -> storageService.store(upload("renamed-executable.exe", "image/png", image("png", 1, 1))));
        assertThrows(ContactBusinessException.class,
                () -> storageService.store(upload("renamed-executable.jpg", "image/jpeg",
                        new byte[] {'M', 'Z', 0, 0, 0, 0})));
        assertThrows(ContactBusinessException.class,
                () -> storageService.store(upload("photo.jpg", "image/jpeg", "%PDF-1.7".getBytes())));
        assertThrows(ContactBusinessException.class,
                () -> storageService.store(upload("photo.png", "image/png", new byte[0])));
        assertThrows(ContactBusinessException.class,
                () -> storageService.store(upload("photo.jpg", "image/jpeg", new byte[] {1, 2, 3, 4})));
    }

    @Test
    void resizesLargeImagesProportionallyWithoutExceedingEightHundredPixels() throws Exception {
        String path = storageService.store(upload("large.png", "image/png", image("png", 1600, 400)));
        BufferedImage stored = javax.imageio.ImageIO.read(storageService.loadResource(path).getInputStream());

        assertEquals(800, stored.getWidth());
        assertEquals(200, stored.getHeight());
    }

    @Test
    void replacementDeletesOldFileOnlyAfterNewImageWasAcceptedAndSaved() throws Exception {
        Contact contact = contactWithType(CommercialType.CLIENTE);
        String oldPath = storageService.store(upload("old.jpg", "image/jpeg", image("jpeg", 12, 12)));
        contact.setPhotoPath(oldPath);
        when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(contactRepository.saveAndFlush(contact)).thenReturn(contact);

        Contact updated = contactService.replacePhoto(1L,
                upload("../new.png", "application/octet-stream", image("png", 8, 5)), administrator);

        assertEquals(contact, updated);
        assertFalse(Files.exists(resolve(oldPath)));
        assertTrue(Files.exists(resolve(contact.getPhotoPath())));
        assertEquals(administrator, contact.getUpdatedBy());
        assertNotNull(contact.getUpdatedAt());
        verify(contactRepository).saveAndFlush(contact);
    }

    @Test
    void invalidReplacementKeepsTheExistingReferenceAndFile() throws Exception {
        Contact contact = contactWithType(CommercialType.CLIENTE);
        String oldPath = storageService.store(upload("old.jpg", "image/jpeg", image("jpeg", 12, 12)));
        contact.setPhotoPath(oldPath);
        when(contactRepository.findById(2L)).thenReturn(Optional.of(contact));

        assertThrows(ContactBusinessException.class,
                () -> contactService.replacePhoto(2L, upload("bad.jpg", "image/jpeg", new byte[] {1, 2, 3}), administrator));

        assertEquals(oldPath, contact.getPhotoPath());
        assertTrue(Files.exists(resolve(oldPath)));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void databaseFailureDuringReplacementRestoresOldReferenceAndKeepsOldFile() throws Exception {
        Contact contact = contactWithType(CommercialType.CLIENTE);
        String oldPath = storageService.store(upload("old.jpg", "image/jpeg", image("jpeg", 12, 12)));
        contact.setPhotoPath(oldPath);
        when(contactRepository.findById(21L)).thenReturn(Optional.of(contact));
        when(contactRepository.saveAndFlush(contact))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("database failure"));

        assertThrows(ContactBusinessException.class, () -> contactService.replacePhoto(21L,
                upload("new.png", "image/png", image("png", 8, 5)), administrator));

        assertEquals(oldPath, contact.getPhotoPath());
        assertTrue(Files.exists(resolve(oldPath)));
        assertNull(contact.getUpdatedBy());
        try (var paths = Files.list(tempDirectory.resolve("contacts"))) {
            assertEquals(1, paths.count());
        }
    }

    @Test
    void removalClearsReferenceAndDeletesFileEvenWhenItWasAlreadyMissing() throws Exception {
        Contact contact = contactWithType(CommercialType.CLIENTE);
        String oldPath = storageService.store(upload("old.png", "image/png", image("png", 10, 10)));
        contact.setPhotoPath(oldPath);
        when(contactRepository.findById(3L)).thenReturn(Optional.of(contact));
        when(contactRepository.saveAndFlush(contact)).thenReturn(contact);
        contactService.removePhoto(3L, administrator);
        assertEquals(null, contact.getPhotoPath());
        assertFalse(Files.exists(resolve(oldPath)));

        String missingPath = "uploads/contacts/00000000-0000-0000-0000-000000000000.jpg";
        contact.setPhotoPath(missingPath);
        when(contactRepository.findById(4L)).thenReturn(Optional.of(contact));
        when(contactRepository.saveAndFlush(contact)).thenReturn(contact);
        contactService.removePhoto(4L, administrator);
        assertEquals(null, contact.getPhotoPath());
    }

    @Test
    void onlyAllowedRolesAndContactScopesCanManagePhotos() throws Exception {
        Contact supplier = contactWithType(CommercialType.PROVEEDOR);
        when(contactRepository.findById(5L)).thenReturn(Optional.of(supplier));
        assertThrows(AccessDeniedException.class,
                () -> contactService.getPhotoPathForViewing(5L, user("seller", Role.VENDEDOR)));
        assertThrows(AccessDeniedException.class,
                () -> contactService.getPhotoPathForViewing(5L, user("warehouse", Role.ALMACENERO)));
        assertThrows(AccessDeniedException.class, () -> contactService.getPhotoPathForViewing(5L, null));
        assertThrows(AccessDeniedException.class, () -> contactService.getPhotoPathForViewing(5L,
                new User("disabled", "hash", Role.ADMINISTRADOR, false, LocalDateTime.now())));
        verify(contactRepository, never()).saveAndFlush(any(Contact.class));
    }

    @Test
    void sellerAndPurchasingCanReplacePhotosOnlyForTheirActiveCommercialType() throws Exception {
        User seller = user("seller", Role.VENDEDOR);
        User purchasing = user("purchasing", Role.COMPRAS);
        Contact client = contactWithType(CommercialType.CLIENTE);
        Contact supplier = contactWithType(CommercialType.PROVEEDOR);
        when(contactRepository.findById(6L)).thenReturn(Optional.of(client));
        when(contactRepository.findById(7L)).thenReturn(Optional.of(supplier));
        when(contactRepository.saveAndFlush(client)).thenReturn(client);
        when(contactRepository.saveAndFlush(supplier)).thenReturn(supplier);

        contactService.replacePhoto(6L, upload("client.png", "image/png", image("png", 5, 5)), seller);
        contactService.replacePhoto(7L, upload("supplier.png", "image/png", image("png", 5, 5)), purchasing);

        assertEquals(seller, client.getUpdatedBy());
        assertEquals(purchasing, supplier.getUpdatedBy());
        assertNotNull(client.getUpdatedAt());
        assertNotNull(supplier.getUpdatedAt());
    }

    @Test
    void administratorMayManageInactiveNonDeletedContactButOtherRolesMayNot() throws Exception {
        Contact inactiveClient = contactWithType(CommercialType.CLIENTE);
        inactiveClient.setStatus(com.gestorcontactos.gestor_contactos.entity.ContactStatus.INACTIVO);
        when(contactRepository.findById(8L)).thenReturn(Optional.of(inactiveClient));
        when(contactRepository.saveAndFlush(inactiveClient)).thenReturn(inactiveClient);

        assertThrows(AccessDeniedException.class, () -> contactService.replacePhoto(8L,
                upload("client.png", "image/png", image("png", 5, 5)), user("seller", Role.VENDEDOR)));
        contactService.replacePhoto(8L, upload("admin.png", "image/png", image("png", 5, 5)), administrator);
        assertNotNull(inactiveClient.getPhotoPath());

        Contact deleted = contactWithType(CommercialType.CLIENTE);
        deleted.setDeletedAt(LocalDateTime.now());
        when(contactRepository.findById(9L)).thenReturn(Optional.of(deleted));
        assertThrows(ContactBusinessException.class, () -> contactService.replacePhoto(9L,
                upload("deleted.png", "image/png", image("png", 5, 5)), administrator));
    }

    @Test
    void creationStoresPhotoReferenceOnContactAndUsesNoOriginalPath() throws Exception {
        when(contactRepository.findByDocumentTypeAndDocumentNumber(DocumentType.DNI, "12345678"))
                .thenReturn(Optional.empty());
        when(contactRepository.saveAndFlush(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contact created = contactService.createNaturalContact(new CreateNaturalContactRequest("María", "Pérez",
                DocumentType.DNI, "12345678", "Av. Perú 123",
                List.of(new CreateContactPhoneRequest(PhoneType.CELULAR, "", "987654321")),
                List.of(new CreateContactEmailRequest("maria@example.com")), Set.of(CommercialType.CLIENTE)),
                administrator, upload("../../private-name.jpeg", "image/jpeg", image("jpeg", 10, 10)));

        assertTrue(created.getPhotoPath().matches("uploads/contacts/[0-9a-f-]{36}\\.jpg"));
        assertTrue(Files.exists(resolve(created.getPhotoPath())));
    }

    private MockMultipartFile upload(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("photo", filename, contentType, bytes);
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height,
                format.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(javax.imageio.ImageIO.write(image, format, output));
        return output.toByteArray();
    }

    private Path resolve(String databasePath) {
        return tempDirectory.resolve(databasePath.substring("uploads/".length()));
    }

    private Contact contactWithType(CommercialType commercialType) {
        Contact contact = new Contact(PersonType.NATURAL, "María", "Pérez", DocumentType.DNI, "12345678",
                "Av. Perú 123", LocalDateTime.now(), administrator);
        contact.addCommercialType(commercialType);
        return contact;
    }

    private User user(String username, Role role) {
        return new User(username, "encoded-hash", role, true, LocalDateTime.now());
    }
}
