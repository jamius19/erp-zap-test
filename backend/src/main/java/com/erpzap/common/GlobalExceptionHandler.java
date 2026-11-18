/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * All errors are RFC 9457 ProblemDetail JSON. Validation errors add an "errors" map (field -> message).
 * Extends ResponseEntityExceptionHandler so framework errors (malformed JSON, missing params, ...) are ProblemDetail too.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ProblemDetail notFound(NotFoundException e) {
		return problem(NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ProblemDetail conflict(ConflictException e) {
		return problem(CONFLICT, e.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ProblemDetail badRequest(BadRequestException e) {
		return problem(BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail integrity(DataIntegrityViolationException e) {
		return problem(CONFLICT, "Request conflicts with existing data");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail denied(AccessDeniedException e) {
		return problem(FORBIDDEN, "Access denied");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException e,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request
	) {
		ProblemDetail pd = problem(BAD_REQUEST, "Validation failed");
		Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						f -> f.getField(),
						f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
						(a, b) -> a
				));
		pd.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(pd);
	}

	private static ProblemDetail problem(HttpStatus status, String detail) {
		return ProblemDetail.forStatusAndDetail(status, detail);
	}
}
