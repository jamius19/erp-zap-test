/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.enrollment;

import com.erpzap.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsBySchoolClassIdAndStudentId(Long classId, Long studentId);

	Optional<Enrollment> findBySchoolClassIdAndStudentId(Long classId, Long studentId);

	@Query("""
			select s from Enrollment e join e.student s join fetch s.user u
			where e.schoolClass.id = :classId
			order by u.fullName
			""")
	List<Student> findStudentsByClassId(@Param("classId") Long classId);

	@Query("select e.student.id from Enrollment e where e.schoolClass.id = :classId")
	Set<Long> findStudentIdsByClassId(@Param("classId") Long classId);

	@Query("select count(distinct e.student.id) from Enrollment e where e.schoolClass.teacher.id = :teacherId")
	long countStudentsByTeacherId(@Param("teacherId") Long teacherId);

	long countByStudentId(Long studentId);
}
