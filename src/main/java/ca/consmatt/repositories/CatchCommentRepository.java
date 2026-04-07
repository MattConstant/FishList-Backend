package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.CatchComment;

/**
 * Persistence for comments on catches.
 */
public interface CatchCommentRepository extends JpaRepository<CatchComment, Long> {

	List<CatchComment> findByCatchRecord_IdOrderByIdAsc(Long catchId);

	Page<CatchComment> findByCatchRecord_IdOrderByIdDesc(Long catchId, Pageable pageable);

	Optional<CatchComment> findByIdAndCatchRecord_Id(Long id, Long catchId);

	void deleteByCatchRecord_Id(Long catchId);

	void deleteByCatchRecord_Location_Id(Long locationId);

	long countByAccount_Id(Long accountId);

	void deleteByAccount_Id(Long accountId);
}
