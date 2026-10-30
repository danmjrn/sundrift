package solutions.shapeit.noirstay.common;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/** Converts expected failures to stable, non-sensitive API errors.
 * @author Daniel Jr Nkulu
 */
@RestControllerAdvice
public class Errors {
    public static Map<String, Object> body(String code, String message) {
        return Map.of("code", code, "message", message, "correlationId", String.valueOf(MDC.get("correlationId")));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> business(ApiException e) { return ResponseEntity.status(e.status).body(body(e.code, e.getMessage())); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class, IllegalArgumentException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.bind.MissingRequestHeaderException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> validation(Exception e) { return ResponseEntity.badRequest().body(body("VALIDATION", "Check the submitted values and date format.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(body("CONFLICT", "The dates or unique value were taken. Refresh and try again."));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> forbidden(AccessDeniedException e) { return ResponseEntity.status(403).body(body("FORBIDDEN", "Access denied.")); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> size(MaxUploadSizeExceededException e) { return ResponseEntity.status(413).body(body("UPLOAD_SIZE", "Images must be 5 MB or smaller.")); }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<?> notFound(Exception e) { return ResponseEntity.status(404).body(body("NOT_FOUND","This API resource is unavailable.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(Errors.class).error("Unhandled API failure type={} correlationId={}",e.getClass().getSimpleName(),MDC.get("correlationId"));
        return ResponseEntity.internalServerError().body(body("INTERNAL_ERROR","The service could not complete this request. Please try again."));
    }
}

