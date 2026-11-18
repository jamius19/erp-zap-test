/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.grade;

import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import com.erpzap.subject.Subject;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "grades")
public class Grade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private SchoolClass schoolClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@Column(nullable = false, length = 20)
	private String term;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal score;

	@Column(length = 255)
	private String remarks;

	/** users.id of whoever last graded this record. */
	@Column(name = "graded_by")
	private Long gradedBy;

	public Long getId() { return id; }
	public SchoolClass getSchoolClass() { return schoolClass; }
	public void setSchoolClass(SchoolClass schoolClass) { this.schoolClass = schoolClass; }
	public Student getStudent() { return student; }
	public void setStudent(Student student) { this.student = student; }
	public Subject getSubject() { return subject; }
	public void setSubject(Subject subject) { this.subject = subject; }
	public String getTerm() { return term; }
	public void setTerm(String term) { this.term = term; }
	public BigDecimal getScore() { return score; }
	public void setScore(BigDecimal score) { this.score = score; }
	public String getRemarks() { return remarks; }
	public void setRemarks(String remarks) { this.remarks = remarks; }
	public Long getGradedBy() { return gradedBy; }
	public void setGradedBy(Long gradedBy) { this.gradedBy = gradedBy; }
}
