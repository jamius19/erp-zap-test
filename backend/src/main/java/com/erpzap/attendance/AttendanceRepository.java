/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	String DTO_SELECT = """
			select new com.erpzap.attendance.AttendanceRecordDto(
				a.id, c.id, c.name, s.id, u.fullName, a.date, a.status
			)
			from Attendance a join a.schoolClass c join a.student s join s.user u
			""";

	@Query(DTO_SELECT + " where c.id = :classId and a.date = :date order by u.fullName")
	List<AttendanceRecordDto> findDtosByClassAndDate(
			@Param("classId") Long classId,
			@Param("date") LocalDate date
	);

	@Query(DTO_SELECT
			+ " where s.id = :studentId and a.date between :from and :to order by a.date desc, c.name")
	List<AttendanceRecordDto> findDtosByStudent(
			@Param("studentId") Long studentId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to
	);

	List<Attendance> findBySchoolClassIdAndDate(Long classId, LocalDate date);

	long countByStudentId(Long studentId);

	long countByStudentIdAndStatusIn(Long studentId, Collection<AttendanceStatus> statuses);
}
