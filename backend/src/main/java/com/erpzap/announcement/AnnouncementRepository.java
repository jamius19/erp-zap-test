/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.announcement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

	String DTO_SELECT = """
			select new com.erpzap.announcement.AnnouncementDto(
				a.id, a.title, a.body, a.audience, c.id, c.name, au.id, au.fullName, a.createdAt
			)
			""";

	String FROM = " from Announcement a left join a.schoolClass c left join a.author au ";

	String ORDER = " order by a.createdAt desc, a.id desc";

	String TEACHER_WHERE = " where a.audience in :audiences or au.id = :userId";

	String STUDENT_WHERE = """
			 where a.audience in :audiences
			 and (a.schoolClass is null or exists (
				select 1 from Enrollment e where e.schoolClass = a.schoolClass and e.student.id = :studentId
			 ))
			""";

	@Query(value = DTO_SELECT + FROM + ORDER, countQuery = "select count(a) from Announcement a")
	Page<AnnouncementDto> findAllDtos(Pageable pageable);

	@Query(value = DTO_SELECT + FROM + TEACHER_WHERE + ORDER, countQuery = "select count(a)" + FROM + TEACHER_WHERE)
	Page<AnnouncementDto> findTeacherDtos(
			@Param("audiences") Collection<Audience> audiences,
			@Param("userId") Long userId,
			Pageable pageable
	);

	@Query(value = DTO_SELECT + FROM + STUDENT_WHERE + ORDER, countQuery = "select count(a)" + FROM + STUDENT_WHERE)
	Page<AnnouncementDto> findStudentDtos(
			@Param("audiences") Collection<Audience> audiences,
			@Param("studentId") Long studentId,
			Pageable pageable
	);

	@Query(DTO_SELECT + FROM + " where a.id = :id")
	Optional<AnnouncementDto> findDtoById(@Param("id") Long id);
}
