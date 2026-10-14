package com.hotel.app.service.serviceimpl;

import com.hotel.app.exception.BusinessException;
import com.hotel.app.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileStorageServiceImpl() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
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

        String newFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return newFileName;
        } catch (IOException ex) {
            throw new BusinessException("No se pudo guardar el archivo. Por favor, intente de nuevo.");
        }
    }
}
