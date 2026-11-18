/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.subject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

	List<Subject> findAllByOrderByCodeAsc();

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);
}
