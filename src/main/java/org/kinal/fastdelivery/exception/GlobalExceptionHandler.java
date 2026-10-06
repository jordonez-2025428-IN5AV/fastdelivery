package org.kinal.fastdelivery.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.kinal.fastdelivery.dto.error.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private ResponseEntity<ErrorResponse> error(
      HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(
            new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> missing(RuntimeException e, HttpServletRequest r) {
    return error(HttpStatus.NOT_FOUND, e.getMessage(), r);
  }

  @ExceptionHandler({
    InsufficientStockException.class,
    InvalidStatusException.class,
    EmailAlreadyExistsException.class
  })
  public ResponseEntity<ErrorResponse> conflict(RuntimeException e, HttpServletRequest r) {
    return error(HttpStatus.CONFLICT, e.getMessage(), r);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> bad(RuntimeException e, HttpServletRequest r) {
    return error(HttpStatus.BAD_REQUEST, e.getMessage(), r);
  }

  @ExceptionHandler({UnauthorizedOperationException.class, AccessDeniedException.class})
  public ResponseEntity<ErrorResponse> forbidden(RuntimeException e, HttpServletRequest r) {
    return error(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación", r);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> auth(AuthenticationException e, HttpServletRequest r) {
    return error(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", r);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validation(
      MethodArgumentNotValidException e, HttpServletRequest r) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .sorted()
            .distinct()
            .collect(java.util.stream.Collectors.joining("; "));
    return error(HttpStatus.BAD_REQUEST, message, r);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorResponse> malformed(Exception e, HttpServletRequest r) {
    return error(
        HttpStatus.BAD_REQUEST,
        "JSON o parámetro inválido; verifica campos, tipos y enumeraciones",
        r);
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    PessimisticLockingFailureException.class
  })
  public ResponseEntity<ErrorResponse> database(RuntimeException e, HttpServletRequest r) {
    return error(
        HttpStatus.CONFLICT, "Conflicto de datos; verifica unicidad y vuelve a intentar", r);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> route(Exception e, HttpServletRequest r) {
    return error(HttpStatus.NOT_FOUND, "Ruta no encontrada", r);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> method(Exception e, HttpServletRequest r) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido", r);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
    LOG.error("Error al procesar {}", r.getRequestURI(), e);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", r);
  }
}
