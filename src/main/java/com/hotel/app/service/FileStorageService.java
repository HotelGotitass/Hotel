package com.hotel.app.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /** Valida y guarda la imagen; devuelve la URL publica absoluta con la que se mostrara. */
    String storeFile(MultipartFile file);
}
