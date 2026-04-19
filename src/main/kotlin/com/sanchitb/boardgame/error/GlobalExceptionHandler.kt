package com.sanchitb.boardgame.error

import com.sanchitb.boardgame.auth.InvalidAppleTokenException
import com.sanchitb.boardgame.auth.InvalidSessionTokenException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAppleTokenException::class)
    fun handleInvalidAppleToken(ex: InvalidAppleTokenException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiError(status = 401, error = "Unauthorized", message = ex.message ?: "Apple token rejected"),
        )

    @ExceptionHandler(InvalidSessionTokenException::class)
    fun handleInvalidSessionToken(ex: InvalidSessionTokenException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiError(status = 401, error = "Unauthorized", message = ex.message ?: "Session token rejected"),
        )

    @ExceptionHandler(SchemaValidationException::class)
    fun handleValidation(ex: SchemaValidationException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                status = 400,
                error = "Bad Request",
                message = ex.message ?: "Validation failed",
                violations = ex.violations,
            ),
        )

    @ExceptionHandler(GameNotFoundException::class)
    fun handleGameNotFound(ex: GameNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(status = 404, error = "Not Found", message = ex.message ?: "Not found"),
        )

    @ExceptionHandler(RecordNotFoundException::class)
    fun handleRecordNotFound(ex: RecordNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(status = 404, error = "Not Found", message = ex.message ?: "Not found"),
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(status = 400, error = "Bad Request", message = ex.mostSpecificCause.message ?: "Malformed JSON"),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBindingErrors(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val violations = ex.bindingResult.fieldErrors.map {
            Violation(path = "/" + it.field.replace('.', '/'), message = it.defaultMessage ?: "invalid")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                status = 400,
                error = "Bad Request",
                message = "Validation failed",
                violations = violations,
            ),
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(status = 400, error = "Bad Request", message = ex.message ?: "Invalid argument"),
        )
}
