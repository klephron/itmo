package ru.itmo.zavar.account.exception;

public class ObjectNotFoundException extends RuntimeException {
  public ObjectNotFoundException(String message) {
    super(message);
  }
}
