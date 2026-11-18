/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import com.erpzap.teacher.Teacher;
import jakarta.persistence.*;

@Entity
@Table(name = "school_classes")
public class SchoolClass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(name = "grade_level", nullable = false)
	private Integer gradeLevel;

	@Column(nullable = false, length = 10)
	private String section;

	@Column(name = "academic_year", nullable = false, length = 9)
	private String academicYear;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_id")
	private Teacher teacher;

	public Long getId() { return id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public Integer getGradeLevel() { return gradeLevel; }
	public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }
	public String getSection() { return section; }
	public void setSection(String section) { this.section = section; }
	public String getAcademicYear() { return academicYear; }
	public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
	public Teacher getTeacher() { return teacher; }
	public void setTeacher(Teacher teacher) { this.teacher = teacher; }
}
