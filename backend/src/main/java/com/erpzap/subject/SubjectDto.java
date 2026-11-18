/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.subject;

public record SubjectDto(Long id, String code, String name) {

	static SubjectDto of(Subject s) {
		return new SubjectDto(s.getId(), s.getCode(), s.getName());
	}
}
