/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import type { Me, Role } from '../api/types';

export function makeMe(role: Role): Me {
  return {
    id: 1,
    username: role.toLowerCase(),
    fullName: `Test ${role}`,
    email: 'test@example.com',
    role,
    hasPhoto: false,
    teacherId: role === 'TEACHER' ? 10 : null,
    studentId: role === 'STUDENT' ? 20 : null,
  };
}
