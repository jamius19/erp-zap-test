/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.user;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(unique = true, length = 150)
	private String email;

	@Column(name = "photo_path")
	private String photoPath;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	public Long getId() { return id; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
	public Role getRole() { return role; }
	public void setRole(Role role) { this.role = role; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPhotoPath() { return photoPath; }
	public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public Instant getCreatedAt() { return createdAt; }
}
