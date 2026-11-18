/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.attendance;

import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "attendance")
public class Attendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private SchoolClass schoolClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(nullable = false)
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private AttendanceStatus status;

	/** users.id of whoever last marked this record. */
	@Column(name = "marked_by")
	private Long markedBy;

	public Long getId() { return id; }
	public SchoolClass getSchoolClass() { return schoolClass; }
	public void setSchoolClass(SchoolClass schoolClass) { this.schoolClass = schoolClass; }
	public Student getStudent() { return student; }
	public void setStudent(Student student) { this.student = student; }
	public LocalDate getDate() { return date; }
	public void setDate(LocalDate date) { this.date = date; }
	public AttendanceStatus getStatus() { return status; }
	public void setStatus(AttendanceStatus status) { this.status = status; }
	public Long getMarkedBy() { return markedBy; }
	public void setMarkedBy(Long markedBy) { this.markedBy = markedBy; }
}
