package ru.itmo.zavar.accountservice.exception;

public class ObjectNotFoundException extends RuntimeException {
  public ObjectNotFoundException(String message) {
    super(message);
  }
}
