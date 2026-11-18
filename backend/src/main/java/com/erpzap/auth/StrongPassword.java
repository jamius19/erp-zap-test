/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Password policy: at least 8 chars with a letter and a digit. Also capped at 72 UTF-8 bytes,
 * which is the most BCrypt can hash (longer input is rejected by the encoder).
 */
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = StrongPassword.Validator.class)
public @interface StrongPassword {

	String message() default "must be 8-72 characters with at least one letter and one digit";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<StrongPassword, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return value != null
					&& value.length() >= 8
					&& value.getBytes(StandardCharsets.UTF_8).length <= 72
					&& value.chars().anyMatch(Character::isLetter)
					&& value.chars().anyMatch(Character::isDigit);
		}
	}
}
