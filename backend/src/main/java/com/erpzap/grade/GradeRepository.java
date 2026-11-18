/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.grade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

	String DTO_SELECT = """
			select new com.erpzap.grade.GradeDto(
				g.id, c.id, c.name, s.id, u.fullName, sub.id, sub.name, g.term, g.score, g.remarks
			)
			from Grade g join g.schoolClass c join g.student s join s.user u join g.subject sub
			""";

	@Query(DTO_SELECT + """
			where c.id = :classId
			and (:subjectId is null or sub.id = :subjectId)
			and (:term is null or g.term = :term)
			order by u.fullName, sub.name, g.term
			""")
	List<GradeDto> findDtosByClass(
			@Param("classId") Long classId,
			@Param("subjectId") Long subjectId,
			@Param("term") String term
	);

	@Query(DTO_SELECT + " where s.id = :studentId order by c.name, sub.name, g.term")
	List<GradeDto> findDtosByStudent(@Param("studentId") Long studentId);

	@Query(DTO_SELECT + " where g.id = :id")
	Optional<GradeDto> findDtoById(@Param("id") Long id);

	boolean existsBySchoolClassIdAndStudentIdAndSubjectIdAndTerm(
			Long classId, Long studentId, Long subjectId, String term
	);

	@Query("select avg(g.score) from Grade g where g.student.id = :studentId")
	Double averageScoreByStudentId(@Param("studentId") Long studentId);
}
