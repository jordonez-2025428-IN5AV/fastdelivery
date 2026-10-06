package org.kinal.fastdelivery.exception;

public class InvalidStatusException extends RuntimeException {
  public InvalidStatusException(String message) {
    super(message);
  }
}
