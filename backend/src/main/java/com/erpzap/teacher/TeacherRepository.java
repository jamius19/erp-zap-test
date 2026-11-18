/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.teacher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

	Optional<Teacher> findByUserId(Long userId);

	boolean existsByEmployeeNo(String employeeNo);

	boolean existsByEmployeeNoAndIdNot(String employeeNo, Long id);

	/**
	 * Case-insensitive substring search. {@code pattern} is already lower-cased, LIKE-escaped and wrapped in '%'.
	 */
	@Query(value = """
			select t from Teacher t join fetch t.user u
			where lower(u.fullName) like :pattern escape '\\'
			   or lower(u.username) like :pattern escape '\\'
			   or lower(t.employeeNo) like :pattern escape '\\'
			""", countQuery = """
			select count(t) from Teacher t join t.user u
			where lower(u.fullName) like :pattern escape '\\'
			   or lower(u.username) like :pattern escape '\\'
			   or lower(t.employeeNo) like :pattern escape '\\'
			""")
	Page<Teacher> search(@Param("pattern") String pattern, Pageable pageable);
}
