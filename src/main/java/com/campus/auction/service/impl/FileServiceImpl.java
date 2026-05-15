package com.campus.auction.service.impl;

import com.campus.auction.exception.ServiceException;
import com.campus.auction.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    /** Configured via {@code app.upload.dir} in application.yml. */
    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "No file provided");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new ServiceException(HttpStatus.BAD_REQUEST,
                    "File size exceeds the 10 MB limit");
        }

        // Build a UUID filename, preserving the original extension
        String original  = file.getOriginalFilename();
        String extension = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename));
        } catch (IOException ex) {
            throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store image: " + ex.getMessage());
        }

        return "/api/images/" + filename;
    }
}
