package com.celcioblond.pms.exception;

/** Thrown when a request clashes with existing data (e.g. duplicate email). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
