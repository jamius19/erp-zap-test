/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.enrollment;

import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "enrollments")
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private SchoolClass schoolClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(name = "enrolled_at", nullable = false, updatable = false)
	private Instant enrolledAt = Instant.now();

	public Enrollment() {
	}

	public Enrollment(SchoolClass schoolClass, Student student) {
		this.schoolClass = schoolClass;
		this.student = student;
	}

	public Long getId() { return id; }
	public SchoolClass getSchoolClass() { return schoolClass; }
	public Student getStudent() { return student; }
	public Instant getEnrolledAt() { return enrolledAt; }
}
