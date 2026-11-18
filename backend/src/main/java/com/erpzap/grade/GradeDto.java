/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.grade;

import java.math.BigDecimal;

public record GradeDto(
		Long id,
		Long classId,
		String className,
		Long studentId,
		String studentName,
		Long subjectId,
		String subjectName,
		String term,
		BigDecimal score,
		String remarks
) {
}
