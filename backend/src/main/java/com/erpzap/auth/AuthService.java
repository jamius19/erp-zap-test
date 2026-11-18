/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

import com.erpzap.auth.AuthDtos.LoginResponse;
import com.erpzap.auth.AuthDtos.MeResponse;
import com.erpzap.common.BadRequestException;
import com.erpzap.common.CurrentUser;
import com.erpzap.common.NotFoundException;
import com.erpzap.config.AppProperties;
import com.erpzap.student.Student;
import com.erpzap.student.StudentRepository;
import com.erpzap.teacher.Teacher;
import com.erpzap.teacher.TeacherRepository;
import com.erpzap.user.User;
import com.erpzap.user.UserRepository;
import com.erpzap.user.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

	/** Result of login/refresh: the JSON body plus the raw refresh token for the cookie. */
	public record Session(LoginResponse body, String refreshToken) {
	}

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final Duration accessTtl;
	private final Duration refreshTtl;
	/** Compared against when the username does not exist, so response time doesn't reveal valid usernames. */
	private final String dummyHash;

	public AuthService(UserRepository userRepository, TeacherRepository teacherRepository,
			StudentRepository studentRepository, RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, AppProperties props) {
		this.userRepository = userRepository;
		this.teacherRepository = teacherRepository;
		this.studentRepository = studentRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.accessTtl = props.jwt().accessTokenTtl();
		this.refreshTtl = props.jwt().refreshTokenTtl();
		this.dummyHash = passwordEncoder.encode("timing-equalizer-password-1");
	}

	public Duration refreshTtl() {
		return refreshTtl;
	}

	@Transactional
	public Session login(String username, String password) {
		User user = userRepository.findByUsername(username).orElse(null);
		boolean matches = passwordMatches(password, user == null ? dummyHash : user.getPasswordHash());
		if (user == null || !matches || !user.isEnabled()) {
			throw new InvalidCredentialsException();
		}
		return issue(user);
	}

	/**
	 * Rotates the refresh token. Presenting a token that is already revoked is treated as theft:
	 * every refresh token of that user is revoked.
	 */
	@Transactional(noRollbackFor = InvalidCredentialsException.class)
	public Session refresh(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvalidCredentialsException();
		}
		RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
				.orElseThrow(InvalidCredentialsException::new);
		if (token.isRevoked() || refreshTokenRepository.revoke(token.getId()) == 0) {
			refreshTokenRepository.revokeAllForUser(token.getUserId());
			throw new InvalidCredentialsException();
		}
		if (token.getExpiresAt().isBefore(Instant.now())) {
			throw new InvalidCredentialsException();
		}
		User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidCredentialsException::new);
		if (!user.isEnabled()) {
			throw new InvalidCredentialsException();
		}
		return issue(user);
	}

	@Transactional
	public void logout(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(sha256(rawToken))
				.ifPresent(t -> refreshTokenRepository.revoke(t.getId()));
	}

	@Transactional(readOnly = true)
	public MeResponse me() {
		User user = userRepository.findById(CurrentUser.id())
				.orElseThrow(() -> new NotFoundException("User not found"));
		Long teacherId = teacherRepository.findByUserId(user.getId()).map(Teacher::getId).orElse(null);
		Long studentId = studentRepository.findByUserId(user.getId()).map(Student::getId).orElse(null);
		return new MeResponse(
				user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole(),
				user.getPhotoPath() != null, teacherId, studentId
		);
	}

	@Transactional
	public void changePassword(String currentPassword, String newPassword) {
		User user = userRepository.findById(CurrentUser.id())
				.orElseThrow(() -> new NotFoundException("User not found"));
		if (!passwordMatches(currentPassword, user.getPasswordHash())) {
			throw new BadRequestException("Current password is incorrect");
		}
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		refreshTokenRepository.revokeAllForUser(user.getId());
	}

	private Session issue(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(accessTtl))
				.claim("username", user.getUsername())
				.claim("role", user.getRole().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		refreshTokenRepository.save(new RefreshToken(user.getId(), sha256(refreshToken), now.plus(refreshTtl)));

		LoginResponse body = new LoginResponse(accessToken, accessTtl.toSeconds(), UserSummary.of(user));
		return new Session(body, refreshToken);
	}

	/** BCrypt only accepts up to 72 bytes; longer input simply doesn't match. */
	private boolean passwordMatches(String raw, String hash) {
		if (raw.getBytes(StandardCharsets.UTF_8).length > 72) {
			return false;
		}
		return passwordEncoder.matches(raw, hash);
	}

	private static String sha256(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
