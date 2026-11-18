/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

public record SchoolClassDto(
		Long id,
		String name,
		Integer gradeLevel,
		String section,
		String academicYear,
		Long teacherId,
		String teacherName,
		Long studentCount
) {
}
