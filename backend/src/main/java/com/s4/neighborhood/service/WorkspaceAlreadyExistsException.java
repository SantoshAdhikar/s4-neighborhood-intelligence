package com.s4.neighborhood.service;

public class WorkspaceAlreadyExistsException extends RuntimeException {
    public WorkspaceAlreadyExistsException() {
        super("This account already has a business workspace. Edit the existing profile instead.");
    }
}
