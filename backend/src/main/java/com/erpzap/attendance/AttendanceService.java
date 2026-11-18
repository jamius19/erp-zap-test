/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.attendance;

import com.erpzap.common.BadRequestException;
import com.erpzap.common.CurrentUser;
import com.erpzap.common.NotFoundException;
import com.erpzap.enrollment.EnrollmentRepository;
import com.erpzap.schoolclass.AccessGuard;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.schoolclass.SchoolClassRepository;
import com.erpzap.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

	/** Bounds used when the caller leaves `from` / `to` open. */
	private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
	private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

	private final AttendanceRepository attendanceRepository;
	private final SchoolClassRepository classRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final StudentRepository studentRepository;
	private final AccessGuard accessGuard;

	public AttendanceService(
			AttendanceRepository attendanceRepository,
			SchoolClassRepository classRepository,
			EnrollmentRepository enrollmentRepository,
			StudentRepository studentRepository,
			AccessGuard accessGuard
	) {
		this.attendanceRepository = attendanceRepository;
		this.classRepository = classRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.studentRepository = studentRepository;
		this.accessGuard = accessGuard;
	}

	@Transactional(readOnly = true)
	public List<AttendanceRecordDto> forClass(Long classId, LocalDate date) {
		if (!classRepository.existsById(classId)) {
			throw new NotFoundException("Class not found");
		}
		return attendanceRepository.findDtosByClassAndDate(classId, date);
	}

	/** Upserts one roll call. Every student must be enrolled in the class; future dates are rejected. */
	public List<AttendanceRecordDto> save(Long classId, AttendanceRequest req) {
		SchoolClass c = accessGuard.requireManageable(classId);
		if (req.date().isAfter(LocalDate.now())) {
			throw new BadRequestException("Attendance cannot be recorded for a future date");
		}
		Set<Long> enrolled = enrollmentRepository.findStudentIdsByClassId(classId);
		Set<Long> seen = new HashSet<>();
		for (AttendanceRequest.Entry r : req.records()) {
			if (!seen.add(r.studentId())) {
				throw new BadRequestException("Duplicate student in records: " + r.studentId());
			}
			if (!enrolled.contains(r.studentId())) {
				throw new BadRequestException("Student " + r.studentId() + " is not enrolled in this class");
			}
		}

		Map<Long, Attendance> existing = attendanceRepository.findBySchoolClassIdAndDate(classId, req.date())
				.stream()
				.collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity()));
		Long markedBy = CurrentUser.id();
		for (AttendanceRequest.Entry r : req.records()) {
			Attendance a = existing.get(r.studentId());
			if (a == null) {
				a = new Attendance();
				a.setSchoolClass(c);
				a.setStudent(studentRepository.getReferenceById(r.studentId()));
				a.setDate(req.date());
			}
			a.setStatus(r.status());
			a.setMarkedBy(markedBy);
			attendanceRepository.save(a);
		}
		return attendanceRepository.findDtosByClassAndDate(classId, req.date());
	}

	@Transactional(readOnly = true)
	public List<AttendanceRecordDto> forStudent(Long studentId, LocalDate from, LocalDate to) {
		accessGuard.requireStudentReadable(studentId);
		return attendanceRepository.findDtosByStudent(
				studentId,
				from == null ? MIN_DATE : from,
				to == null ? MAX_DATE : to
		);
	}
}
