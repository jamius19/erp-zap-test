/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */

export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT';

export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: Role;
  hasPhoto: boolean;
}

export interface Me extends UserSummary {
  teacherId: number | null;
  studentId: number | null;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  user: UserSummary;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Teacher {
  id: number;
  userId: number;
  username: string;
  fullName: string;
  email: string;
  employeeNo: string;
  phone: string;
}

export interface Student {
  id: number;
  userId: number;
  username: string;
  fullName: string;
  email: string;
  studentNo: string;
  dateOfBirth: string;
  guardianName: string;
  guardianPhone: string;
}

export interface Subject {
  id: number;
  code: string;
  name: string;
}

export interface SchoolClass {
  id: number;
  name: string;
  gradeLevel: number;
  section: string;
  academicYear: string;
  teacherId: number | null;
  teacherName: string | null;
  studentCount: number;
}

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
export const ATTENDANCE_STATUSES: AttendanceStatus[] = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];

export interface AttendanceRecord {
  id: number;
  classId: number;
  className: string;
  studentId: number;
  studentName: string;
  date: string;
  status: AttendanceStatus;
}

export const TERMS = ['TERM1', 'TERM2', 'FINAL'];

export interface Grade {
  id: number;
  classId: number;
  className: string;
  studentId: number;
  studentName: string;
  subjectId: number;
  subjectName: string;
  term: string;
  score: number;
  remarks: string | null;
}

export type Audience = 'ALL' | 'TEACHERS' | 'STUDENTS';

export interface Announcement {
  id: number;
  title: string;
  body: string;
  audience: Audience;
  classId: number | null;
  className: string | null;
  authorId: number;
  authorName: string;
  createdAt: string;
}
