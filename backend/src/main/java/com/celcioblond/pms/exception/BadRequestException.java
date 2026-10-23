package com.celcioblond.pms.exception;

/** Thrown when a request is well-formed but semantically invalid. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
