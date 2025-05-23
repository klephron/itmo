package ru.itmopai.backend.exception;

public class PermissionDenied extends RuntimeException {
  public PermissionDenied(String message) {
    super(message);
  }
}
