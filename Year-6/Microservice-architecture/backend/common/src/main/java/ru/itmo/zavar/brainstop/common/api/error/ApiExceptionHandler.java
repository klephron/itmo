package ru.itmo.zavar.brainstop.common.api.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = Type.SERVLET)
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        return ResponseEntity.badRequest().body(buildValidationError(ex.getBindingResult().getFieldErrors(),
                ex.getBindingResult().getGlobalErrors(), "Validation failed", request));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBindException(BindException ex, WebRequest request) {
        return ResponseEntity.badRequest().body(buildValidationError(ex.getBindingResult().getFieldErrors(),
                ex.getBindingResult().getGlobalErrors(), "Validation failed", request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<ApiFieldError> fields = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "request";
            fields.add(new ApiFieldError(field, violation.getMessage(), violation.getInvalidValue()));
        }
        ApiError error = buildError(HttpStatus.BAD_REQUEST, "Validation failed", request, fields);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        List<ApiFieldError> fields = List.of(new ApiFieldError(ex.getParameterName(), ex.getMessage(), null));
        return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request, fields));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        List<ApiFieldError> fields = List.of(new ApiFieldError(ex.getName(), message, ex.getValue()));
        return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, message, request, fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, message, request, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(buildError(status, message, request, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest request) {
        log.warn("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, null));
    }

    private ApiError buildValidationError(List<FieldError> fieldErrors,
                                          List<ObjectError> globalErrors,
                                          String message,
                                          WebRequest request) {
        List<ApiFieldError> fields = new ArrayList<>();
        if (fieldErrors != null) {
            for (FieldError err : fieldErrors) {
                fields.add(new ApiFieldError(err.getField(), err.getDefaultMessage(), err.getRejectedValue()));
            }
        }
        if (globalErrors != null) {
            for (ObjectError err : globalErrors) {
                fields.add(new ApiFieldError(err.getObjectName(), err.getDefaultMessage(), null));
            }
        }
        return buildError(HttpStatus.BAD_REQUEST, message, request, fields);
    }

    private ApiError buildError(HttpStatus status, String message, WebRequest request, List<ApiFieldError> fields) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                extractPath(request),
                fields == null ? List.of() : fields
        );
    }

    private String extractPath(WebRequest request) {
        if (request == null) {
            return null;
        }
        String desc = request.getDescription(false);
        if (desc == null) {
            return null;
        }
        return desc.startsWith("uri=") ? desc.substring(4) : desc;
    }
}
