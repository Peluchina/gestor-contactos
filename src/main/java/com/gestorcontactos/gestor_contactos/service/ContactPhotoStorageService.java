package com.gestorcontactos.gestor_contactos.service;

import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import com.gestorcontactos.gestor_contactos.validation.ContactPhotoValidator;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContactPhotoStorageService {

    private static final String DATABASE_PATH_PREFIX = "uploads/contacts/";
    private final ContactPhotoValidator validator;
    private final Path contactsDirectory;

    @Autowired
    public ContactPhotoStorageService(ContactPhotoValidator validator,
            @Value("${app.uploads.directory:uploads}") String uploadsDirectory) {
        this(validator, Path.of(uploadsDirectory));
    }

    public ContactPhotoStorageService(ContactPhotoValidator validator, Path uploadsDirectory) {
        this.validator = validator;
        this.contactsDirectory = uploadsDirectory.toAbsolutePath().normalize().resolve("contacts").normalize();
    }

    public String store(MultipartFile file) {
        ContactPhotoValidator.ValidatedPhoto photo = validator.validateAndProcess(file);
        String filename = UUID.randomUUID() + "." + photo.extension();
        Path target = contactsDirectory.resolve(filename).normalize();
        if (!target.startsWith(contactsDirectory)) {
            throw new ContactBusinessException("No se pudo almacenar la fotografía.");
        }

        Path temporary = null;
        try {
            Files.createDirectories(contactsDirectory);
            temporary = Files.createTempFile(contactsDirectory, ".photo-", ".tmp");
            Files.write(temporary, photo.content());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            return DATABASE_PATH_PREFIX + filename;
        } catch (IOException exception) {
            throw new ContactBusinessException("No se pudo almacenar la fotografía.");
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The temporary file is inside the ignored upload directory.
                }
            }
        }
    }

    public Resource loadResource(String photoPath) {
        Path storedPath = resolveStoredPath(photoPath);
        if (!Files.isRegularFile(storedPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new ContactBusinessException("No se encontró la fotografía.");
        }
        return new FileSystemResource(storedPath);
    }

    public MediaType mediaType(String photoPath) {
        Path storedPath = resolveStoredPath(photoPath);
        String filename = storedPath.getFileName().toString();
        return filename.endsWith(".jpg") ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;
    }

    public void delete(String photoPath) {
        if (photoPath == null) {
            return;
        }
        Path storedPath = resolveStoredPath(photoPath);
        try {
            Files.deleteIfExists(storedPath);
        } catch (IOException exception) {
            throw new ContactBusinessException("No se pudo eliminar la fotografía almacenada.");
        }
    }

    private Path resolveStoredPath(String photoPath) {
        if (photoPath == null || !photoPath.matches("uploads/contacts/[0-9a-fA-F-]{36}\\.(jpg|png)")) {
            throw new ContactBusinessException("La referencia de la fotografía no es válida.");
        }
        String filename = photoPath.substring(DATABASE_PATH_PREFIX.length());
        Path resolved = contactsDirectory.resolve(filename).normalize();
        if (!resolved.startsWith(contactsDirectory)) {
            throw new ContactBusinessException("La referencia de la fotografía no es válida.");
        }
        return resolved;
    }
}
