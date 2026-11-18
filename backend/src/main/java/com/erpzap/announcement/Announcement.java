/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.announcement;

import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "announcements")
public class Announcement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Audience audience;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_id")
	private SchoolClass schoolClass;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private User author;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getBody() { return body; }
	public void setBody(String body) { this.body = body; }
	public Audience getAudience() { return audience; }
	public void setAudience(Audience audience) { this.audience = audience; }
	public SchoolClass getSchoolClass() { return schoolClass; }
	public void setSchoolClass(SchoolClass schoolClass) { this.schoolClass = schoolClass; }
	public User getAuthor() { return author; }
	public void setAuthor(User author) { this.author = author; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
