package com.hotel.app.service.serviceimpl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Guarda las imagenes en Cloudinary si CLOUDINARY_URL esta configurada (produccion: el disco
 * de Cloud Run se borra en cada despliegue). Sin ella, en la carpeta local uploads/ (desarrollo).
 */
@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final String CLOUDINARY_FOLDER = "hotel-gotitas/categories";

    private final Path fileStorageLocation;
    private final Cloudinary cloudinary;

    public FileStorageServiceImpl(@Value("${cloudinary.url:}") String cloudinaryUrl) {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            this.cloudinary = new Cloudinary(cloudinaryUrl);
            this.cloudinary.config.secure = true;
            log.info("Imagenes: Cloudinary (cloud {})", cloudinary.config.cloudName);
            return;
        }
        this.cloudinary = null;
        log.warn("CLOUDINARY_URL no configurada: las imagenes se guardan en uploads/ y se pierden al redesplegar");
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String fileExtension = validate(file);
        String baseName = UUID.randomUUID().toString();
        return cloudinary != null ? uploadToCloudinary(file, baseName) : storeLocally(file, baseName + fileExtension);
    }

    private String uploadToCloudinary(MultipartFile file, String publicId) {
        try {
            cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", CLOUDINARY_FOLDER,
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", false));
            // Se entrega en el formato y calidad optimos para cada navegador, max 1600 px de ancho
            return cloudinary.url()
                    .transformation(new Transformation<>().width(1600).crop("limit").fetchFormat("auto").quality("auto"))
                    .generate(CLOUDINARY_FOLDER + "/" + publicId);
        } catch (Exception ex) {
            log.error("No se pudo subir la imagen a Cloudinary", ex);
            throw new BusinessException("No se pudo subir la imagen. Por favor, intente de nuevo.");
        }
    }

    private String storeLocally(MultipartFile file, String newFileName) {
        try {
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(newFileName)
                    .toUriString();
        } catch (IOException ex) {
            throw new BusinessException("No se pudo guardar el archivo. Por favor, intente de nuevo.");
        }
    }

    /** Devuelve la extension (en minusculas) si el archivo es una imagen valida. */
    private String validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("El archivo excede el tamaño máximo permitido de 5MB.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException("Tipo de archivo no permitido. Solo se aceptan imágenes (JPEG, PNG, WEBP).");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Prevent Path Traversal in the original file name
        if (originalFileName.contains("..") || originalFileName.contains("/") || originalFileName.contains("\\")) {
            throw new BusinessException("El nombre del archivo contiene caracteres inválidos.");
        }

        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        }

        // Final security check on extension
        if (!fileExtension.equals(".jpg") && !fileExtension.equals(".jpeg") &&
            !fileExtension.equals(".png") && !fileExtension.equals(".webp")) {
            throw new BusinessException("La extensión del archivo no coincide con un formato de imagen válido.");
        }
        return fileExtension;
    }
}
