package sk.posam.fsa.foodrescue.controller;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sk.posam.fsa.foodrescue.domain.exceptions.FoodRescueException;
import sk.posam.fsa.foodrescue.rest.dto.ErrorResponseDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FoodRescueException.class)
    public ResponseEntity<ErrorResponseDto> handleFoodRescueException(FoodRescueException ex, WebRequest request) {
        LoggerFactory.getLogger(GlobalExceptionHandler.class).warn("Domain error: {}", ex.getMessage());
        return new ResponseEntity<>(
                createError(resolveCode(ex), ex.getMessage(), ex.getDetails(), request),
                resolveStatus(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();
        return new ResponseEntity<>(
                createError("VALIDATION_ERROR", "Invalid request body", details, request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        return new ResponseEntity<>(
                createError("VALIDATION_ERROR", "Malformed JSON request body", List.of("Request body is not valid JSON"), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleHandlerMethodValidationException(HandlerMethodValidationException ex, WebRequest request) {
        List<String> details = Stream.concat(
                        ex.getParameterValidationResults().stream()
                                .flatMap(result -> result.getResolvableErrors().stream()),
                        ex.getCrossParameterValidationResults().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        return new ResponseEntity<>(
                createError("VALIDATION_ERROR", "Invalid request parameters", details, request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .toList();
        return new ResponseEntity<>(
                createError("VALIDATION_ERROR", "Invalid request parameters", details, request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String detail = ex.getName() + " has invalid value '" + ex.getValue() + "'";
        return new ResponseEntity<>(
                createError("VALIDATION_ERROR", "Invalid request parameters", List.of(detail), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String detail = "Method '" + ex.getMethod() + "' is not supported for this endpoint";

        return new ResponseEntity<>(
                createError("METHOD_NOT_ALLOWED", "HTTP method not allowed", List.of(detail), request),
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingPathVariableException.class})
    public ResponseEntity<ErrorResponseDto> handleMissingParameter(Exception ex, WebRequest request) {
        return new ResponseEntity<>(
                createError("VALIDATION_ERROR", "Missing required request parameters", List.of(ex.getMessage()), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        return new ResponseEntity<>(
                createError("CONFLICT", "Request conflicts with existing data", List.of("Duplicate or invalid database state"), request),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        LoggerFactory.getLogger(GlobalExceptionHandler.class).warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(
                createError("FORBIDDEN", "Access denied", List.of("Insufficient permissions"), request),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponseDto> handleThrowable(Throwable ex, WebRequest request) {
        LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Global error occurred", ex);
        return new ResponseEntity<>(
                createError("INTERNAL_ERROR", "Unexpected internal error", List.of("Please contact support"), request),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponseDto createError(String code, String message, List<String> details, WebRequest request) {
        return new ErrorResponseDto()
                .code(code)
                .message(message)
                .details(details == null ? List.of() : details)
                .timestamp(OffsetDateTime.now())
                .path(resolvePath(request));
    }

    private String resolvePath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "";
    }

    private HttpStatus resolveStatus(FoodRescueException ex) {
        return switch (ex.getType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
        };
    }

    private String resolveCode(FoodRescueException ex) {
        return switch (ex.getType()) {
            case NOT_FOUND -> "NOT_FOUND";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case CONFLICT -> "CONFLICT";
            case VALIDATION -> "VALIDATION_ERROR";
        };
    }
}