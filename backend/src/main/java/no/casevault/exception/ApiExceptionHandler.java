package no.casevault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(String error, String message, Map<String, String> fields) {
        static ApiError of(String error, String message) {
            return new ApiError(error, message, null);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (var error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
            .body(new ApiError("VALIDATION_ERROR", "One or more fields are invalid", fields));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiError> handlePasswordMismatch(PasswordMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("PASSWORD_MISMATCH", ex.getMessage()));
    }

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<ApiError> handleUsernameTaken(UsernameTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("USERNAME_TAKEN", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<ApiError> handleCaseNotFound(CaseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("CASE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ApiError> handleInvalidFile(InvalidFileException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("INVALID_FILE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ApiError> handleInvalidDocument(InvalidDocumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("INVALID_DOCUMENT", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCaseOperationException.class)
    public ResponseEntity<ApiError> handleInvalidCaseOperation(InvalidCaseOperationException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("INVALID_CASE_OPERATION", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidResetCodeException.class)
    public ResponseEntity<ApiError> handleInvalidResetCode(InvalidResetCodeException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("INVALID_RESET_CODE", ex.getMessage()));
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiError> handleMessageNotFound(MessageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("MESSAGE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CaseShareNotFoundException.class)
    public ResponseEntity<ApiError> handleCaseShareNotFound(CaseShareNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("CASE_SHARE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ApiError> handleConversationNotFound(ConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("CONVERSATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CannotDeleteUserException.class)
    public ResponseEntity<ApiError> handleCannotDeleteUser(CannotDeleteUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("CANNOT_DELETE_USER", ex.getMessage()));
    }

    @ExceptionHandler(ProfilePictureNotFoundException.class)
    public ResponseEntity<ApiError> handleProfilePictureNotFound(ProfilePictureNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("PROFILE_PICTURE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidMessageException.class)
    public ResponseEntity<ApiError> handleInvalidMessage(InvalidMessageException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("INVALID_MESSAGE", ex.getMessage()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiError> handleDocumentNotFound(DocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("DOCUMENT_NOT_FOUND", ex.getMessage()));
    }
}
