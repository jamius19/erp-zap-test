/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.enrollment;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(@NotNull Long studentId) {
}
