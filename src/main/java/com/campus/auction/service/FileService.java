package com.campus.auction.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * Validates the file (type and size), persists it to the configured upload directory
     * with a UUID-based filename, and returns the relative URL path that callers can store.
     *
     * @param file the uploaded multipart file
     * @return relative URL path, e.g. {@code "/api/images/550e8400-e29b.jpg"}
     * @throws com.campus.auction.exception.ServiceException (400) for invalid type or size
     * @throws com.campus.auction.exception.ServiceException (500) for IO failures
     */
    String uploadImage(MultipartFile file);
}
