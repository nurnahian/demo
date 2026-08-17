package com.example.demo.exception;

import com.example.demo.dto.commonDto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for the whole application.
 * Extends ResponseEntityExceptionHandler to also override Spring MVC's
 * built-in exception handling (e.g. malformed JSON, missing params, 404s).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ----------------------------------------------------------------
    // 400 - Bean Validation (@Valid on @RequestBody)
    // ----------------------------------------------------------------
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        String traceId = generateTraceId();
        log.warn("[{}] Validation failed: {}", traceId, fieldErrors);

        ErrorResponse body = buildBaseError(HttpStatus.BAD_REQUEST, "Validation failed",
                extractPath(request), traceId)
                .toBuilder()
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ----------------------------------------------------------------
    // 400 - Constraint violations (@Validated on @PathVariable / @RequestParam)
    // ----------------------------------------------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        String traceId = generateTraceId();
        log.warn("[{}] Constraint violation: {}", traceId, fieldErrors);

        ErrorResponse body = buildBaseError(HttpStatus.BAD_REQUEST, "Validation failed",
                request.getRequestURI(), traceId)
                .toBuilder()
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    // ----------------------------------------------------------------
    // 400 - Malformed JSON body
    // ----------------------------------------------------------------
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Malformed request body: {}", traceId, ex.getMessage());

        ErrorResponse body = buildBaseError(HttpStatus.BAD_REQUEST,
                "Malformed JSON request body", extractPath(request), traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ----------------------------------------------------------------
    // 400 - Missing request parameter
    // ----------------------------------------------------------------
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        String traceId = generateTraceId();
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        log.warn("[{}] {}", traceId, message);

        ErrorResponse body = buildBaseError(HttpStatus.BAD_REQUEST, message,
                extractPath(request), traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ----------------------------------------------------------------
    // 400 - Type mismatch (e.g. passing "abc" for a Long path variable)
    // ----------------------------------------------------------------
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        log.warn("[{}] {}", traceId, message);

        ErrorResponse body = buildBaseError(HttpStatus.BAD_REQUEST, message,
                request.getRequestURI(), traceId);

        return ResponseEntity.badRequest().body(body);
    }

    // ----------------------------------------------------------------
    // 404 - Resource not found (custom)
    // ----------------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Resource not found: {}", traceId, ex.getMessage());

        ErrorResponse body = buildBaseError(HttpStatus.NOT_FOUND, ex.getMessage(),
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ----------------------------------------------------------------
    // 404 - No handler found for route (requires setting
    // spring.mvc.throw-exception-if-no-handler-found=true and
    // spring.web.resources.add-mappings=false in application.properties)
    // ----------------------------------------------------------------
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        String traceId = generateTraceId();
        String message = String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL());
        log.warn("[{}] {}", traceId, message);

        ErrorResponse body = buildBaseError(HttpStatus.NOT_FOUND, message,
                extractPath(request), traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ----------------------------------------------------------------
    // 405 - Method not allowed
    // ----------------------------------------------------------------
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        String traceId = generateTraceId();
        String message = String.format("Method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());

        log.warn("[{}] {}", traceId, message);

        ErrorResponse body = buildBaseError(HttpStatus.METHOD_NOT_ALLOWED, message,
                extractPath(request), traceId);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // ----------------------------------------------------------------
    // 401 - Bad credentials
    // ----------------------------------------------------------------
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Authentication failed: invalid credentials", traceId);

        ErrorResponse body = buildBaseError(HttpStatus.UNAUTHORIZED, "Invalid username or password",
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ----------------------------------------------------------------
    // 401 - Generic authentication failure (expired/invalid JWT, etc.)
    // ----------------------------------------------------------------
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Authentication error: {}", traceId, ex.getMessage());

        ErrorResponse body = buildBaseError(HttpStatus.UNAUTHORIZED, "Authentication failed",
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ----------------------------------------------------------------
    // 403 - Access denied (role/authority insufficient)
    // ----------------------------------------------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Access denied for path {}: {}", traceId, request.getRequestURI(), ex.getMessage());

        ErrorResponse body = buildBaseError(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource",
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ----------------------------------------------------------------
    // 409 - Duplicate resource (custom)
    // ----------------------------------------------------------------
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Duplicate resource: {}", traceId, ex.getMessage());

        ErrorResponse body = buildBaseError(HttpStatus.CONFLICT, ex.getMessage(),
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ----------------------------------------------------------------
    // 409 - Database constraint violations (e.g. unique key, FK violation)
    // ----------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        // Full exception logged internally; generic message returned to client
        log.error("[{}] Data integrity violation at {}: {}", traceId, request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse body = buildBaseError(HttpStatus.CONFLICT,
                "The request could not be completed due to a data conflict (e.g. duplicate or referenced entry)",
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ----------------------------------------------------------------
    // Custom business exception with configurable status/error code
    // ----------------------------------------------------------------
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Business error [{}]: {}", traceId, ex.getErrorCode(), ex.getMessage());

        ErrorResponse body = buildBaseError(ex.getStatus(), ex.getMessage(),
                request.getRequestURI(), traceId);

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ----------------------------------------------------------------
    // 404 - Optional.get() / no such element
    // ----------------------------------------------------------------
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(
            NoSuchElementException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] No such element: {}", traceId, ex.getMessage());

        ErrorResponse body = buildBaseError(HttpStatus.NOT_FOUND, "The requested resource was not found",
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ----------------------------------------------------------------
    // 400 - Illegal arguments / illegal state thrown manually in service layer
    // ----------------------------------------------------------------
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            RuntimeException ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("[{}] Illegal argument/state: {}", traceId, ex.getMessage());

        ErrorResponse body = buildBaseError(HttpStatus.BAD_REQUEST, ex.getMessage(),
                request.getRequestURI(), traceId);

        return ResponseEntity.badRequest().body(body);
    }

    // ----------------------------------------------------------------
    // 500 - Catch-all fallback for anything unhandled
    // ----------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String traceId = generateTraceId();
        // Full stack trace logged internally; never exposed to the client
        log.error("[{}] Unhandled exception at {}: {}", traceId, request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse body = buildBaseError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(), traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private ErrorResponse buildBaseError(HttpStatus status, String message, String path, String traceId) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .traceId(traceId)
                .build();
    }

    private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
        return ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }

    private ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;

        return ErrorResponse.FieldError.builder()
                .field(field)
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build();
    }

    private String extractPath(WebRequest request) {
        // WebRequest description looks like "uri=/api/users"
        String desc = request.getDescription(false);
        return desc.replace("uri=", "");
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}