package com.campus.auction.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown by the service layer for domain-rule violations.
 * Carry an HttpStatus so the web layer can map it directly (409 / 403 / 404, etc.).
 */
@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatus status;

    public ServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
