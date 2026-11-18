/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.student;

import com.erpzap.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "students")
public class Student {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "student_no", nullable = false, unique = true, length = 30)
	private String studentNo;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@Column(name = "guardian_name", length = 100)
	private String guardianName;

	@Column(name = "guardian_phone", length = 30)
	private String guardianPhone;

	public Long getId() { return id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getStudentNo() { return studentNo; }
	public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
	public LocalDate getDateOfBirth() { return dateOfBirth; }
	public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
	public String getGuardianName() { return guardianName; }
	public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
	public String getGuardianPhone() { return guardianPhone; }
	public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }
}
