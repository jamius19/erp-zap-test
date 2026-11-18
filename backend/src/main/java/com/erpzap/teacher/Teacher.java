/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.teacher;

import com.erpzap.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "teachers")
public class Teacher {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "employee_no", nullable = false, unique = true, length = 30)
	private String employeeNo;

	@Column(length = 30)
	private String phone;

	public Long getId() { return id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getEmployeeNo() { return employeeNo; }
	public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
}
