package com.gestorcontactos.gestor_contactos.validation;

import com.gestorcontactos.gestor_contactos.exception.ContactBusinessException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ContactPhotoValidator {

    public static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 800;

    public ValidatedPhoto validateAndProcess(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ContactBusinessException("Seleccione una fotografía válida.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ContactBusinessException("La fotografía no debe superar los 2 MB.");
        }

        String extension = allowedExtension(file.getOriginalFilename());
        try {
            byte[] input = file.getBytes();
            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
                if (stream == null) {
                    throw invalidImage();
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) {
                    throw invalidImage();
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream, true, true);
                    String detectedFormat = normalizeFormat(reader.getFormatName());
                    if (!extensionMatches(extension, detectedFormat)) {
                        throw invalidImage();
                    }
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width < 1 || height < 1) {
                        throw invalidImage();
                    }

                    double scale = Math.min(1d, Math.min((double) MAX_DIMENSION / width,
                            (double) MAX_DIMENSION / height));
                    int targetWidth = Math.max(1, (int) Math.round(width * scale));
                    int targetHeight = Math.max(1, (int) Math.round(height * scale));
                    int subsampling = Math.max(1, (int) Math.floor(1d / scale));
                    ImageReadParam readParam = reader.getDefaultReadParam();
                    if (subsampling > 1) {
                        readParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
                    }
                    BufferedImage decoded = reader.read(0, readParam);
                    if (decoded == null) {
                        throw invalidImage();
                    }

                    String outputFormat = detectedFormat.equals("webp") ? "png" : detectedFormat;
                    BufferedImage resized = render(decoded, targetWidth, targetHeight, outputFormat.equals("png"));
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!ImageIO.write(resized, outputFormat, output)) {
                        throw invalidImage();
                    }
                    return new ValidatedPhoto(output.toByteArray(), outputFormat);
                } finally {
                    reader.dispose();
                }
            }
        } catch (ContactBusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalidImage();
        }
    }

    private BufferedImage render(BufferedImage source, int width, int height, boolean preserveAlpha) {
        BufferedImage target = new BufferedImage(width, height,
                preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (!preserveAlpha) {
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private String allowedExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ContactBusinessException("El formato de la fotografía no está permitido.");
        }
        String filename = originalFilename.replace('\\', '/');
        int lastSlash = filename.lastIndexOf('/');
        String basename = filename.substring(lastSlash + 1);
        int lastDot = basename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == basename.length() - 1) {
            throw new ContactBusinessException("El formato de la fotografía no está permitido.");
        }
        String extension = basename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals("jpg") && !extension.equals("jpeg")
                && !extension.equals("png") && !extension.equals("webp")) {
            throw new ContactBusinessException("El formato de la fotografía no está permitido.");
        }
        return extension;
    }

    private String normalizeFormat(String format) {
        String normalized = format.toLowerCase(Locale.ROOT);
        return normalized.equals("jpg") ? "jpeg" : normalized;
    }

    private boolean extensionMatches(String extension, String detectedFormat) {
        return switch (detectedFormat) {
            case "jpeg" -> extension.equals("jpg") || extension.equals("jpeg");
            case "png" -> extension.equals("png");
            case "webp" -> extension.equals("webp");
            default -> false;
        };
    }

    private ContactBusinessException invalidImage() {
        return new ContactBusinessException("El archivo no es una imagen válida o está dañado.");
    }

    public record ValidatedPhoto(byte[] content, String format) {
        public String extension() {
            return format.equals("jpeg") ? "jpg" : format;
        }
    }
}
