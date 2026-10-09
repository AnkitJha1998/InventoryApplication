package com.example.user.service;

public final class UserExceptions {
    private UserExceptions() {
    }

    public static class DuplicateUsernameException extends Exception {
        public DuplicateUsernameException() {
            super("username already exists");
        }
    }

    public static class InvalidCredentialsException extends Exception {
        public InvalidCredentialsException() {
            super("invalid credentials");
        }
    }

    public static class InvalidRoleException extends Exception {
        public InvalidRoleException() {
            super("role must be CUSTOMER or STORE_MANAGER");
        }
    }
}
