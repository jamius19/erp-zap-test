/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

	String DTO_SELECT = """
			select new com.erpzap.schoolclass.SchoolClassDto(
				c.id, c.name, c.gradeLevel, c.section, c.academicYear, t.id, u.fullName,
				(select count(e) from Enrollment e where e.schoolClass = c)
			)
			from SchoolClass c left join c.teacher t left join t.user u
			""";

	String ORDER = " order by c.gradeLevel, c.section, c.academicYear";

	@Query(DTO_SELECT + ORDER)
	List<SchoolClassDto> findAllDtos();

	@Query(DTO_SELECT + " where t.id = :teacherId" + ORDER)
	List<SchoolClassDto> findDtosByTeacherId(@Param("teacherId") Long teacherId);

	@Query(DTO_SELECT
			+ " where exists (select 1 from Enrollment e2 where e2.schoolClass = c and e2.student.id = :studentId)"
			+ ORDER)
	List<SchoolClassDto> findDtosByStudentId(@Param("studentId") Long studentId);

	@Query(DTO_SELECT + " where c.id = :id")
	Optional<SchoolClassDto> findDtoById(@Param("id") Long id);

	boolean existsByGradeLevelAndSectionAndAcademicYear(Integer gradeLevel, String section, String academicYear);

	boolean existsByGradeLevelAndSectionAndAcademicYearAndIdNot(
			Integer gradeLevel, String section, String academicYear, Long id
	);

	long countByTeacherId(Long teacherId);
}
