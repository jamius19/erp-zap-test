/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.subject;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	public Long getId() { return id; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
}
