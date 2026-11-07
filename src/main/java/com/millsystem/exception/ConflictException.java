package com.millsystem.exception;

/** The request is valid but conflicts with the current state (e.g. deleting a farmer who has purchases). Maps to 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
