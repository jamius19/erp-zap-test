/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

import com.erpzap.auth.AuthDtos.ChangePasswordRequest;
import com.erpzap.auth.AuthDtos.LoginRequest;
import com.erpzap.auth.AuthDtos.LoginResponse;
import com.erpzap.auth.AuthDtos.MeResponse;
import com.erpzap.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final AppProperties.RefreshCookie cookieProps;

	public AuthController(AuthService authService, AppProperties props) {
		this.authService = authService;
		this.cookieProps = props.refreshCookie();
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return withCookie(authService.login(request.username(), request.password()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
		return withCookie(authService.refresh(readCookie(request)));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authService.logout(readCookie(request));
		return ResponseEntity.noContent().header(SET_COOKIE, cookie("", Duration.ZERO)).build();
	}

	@GetMapping("/me")
	public MeResponse me() {
		return authService.me();
	}

	@PostMapping("/change-password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(request.currentPassword(), request.newPassword());
		return ResponseEntity.noContent().header(SET_COOKIE, cookie("", Duration.ZERO)).build();
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ProblemDetail> invalidCredentials(InvalidCredentialsException e) {
		return ResponseEntity.status(UNAUTHORIZED)
				.header(WWW_AUTHENTICATE, "Bearer")
				.body(ProblemDetail.forStatusAndDetail(UNAUTHORIZED, e.getMessage()));
	}

	private ResponseEntity<LoginResponse> withCookie(AuthService.Session session) {
		return ResponseEntity.ok()
				.header(SET_COOKIE, cookie(session.refreshToken(), authService.refreshTtl()))
				.body(session.body());
	}

	private String readCookie(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, cookieProps.name());
		return cookie == null ? null : cookie.getValue();
	}

	private String cookie(String value, Duration maxAge) {
		return ResponseCookie.from(cookieProps.name(), value)
				.httpOnly(true)
				.secure(cookieProps.secure())
				.sameSite("Strict")
				.path("/api/auth")
				.maxAge(maxAge)
				.build()
				.toString();
	}
}
