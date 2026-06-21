package ca.consmatt.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.CatchComment;

/**
 * Persistence for comments on catches.
 */
public interface CatchCommentRepository extends JpaRepository<CatchComment, Long> {

	Page<CatchComment> findByCatchRecord_IdOrderByIdDesc(Long catchId, Pageable pageable);

	Page<CatchComment> findByCatchRecord_IdOrderByIdAsc(Long catchId, Pageable pageable);

	java.util.List<CatchComment> findByParent_Id(Long parentCommentId);

	Optional<CatchComment> findByIdAndCatchRecord_Id(Long id, Long catchId);

	void deleteByCatchRecord_Id(Long catchId);

	void deleteByCatchRecord_Location_Id(Long locationId);

	long countByAccount_Id(Long accountId);

	void deleteByAccount_Id(Long accountId);

	/**
	 * Replies where the parent comment belongs to {@code parentAccountId}, excluding comments
	 * authored by {@code excludeAuthorId} (so the user is not notified of their own replies).
	 */
	@EntityGraph(attributePaths = { "catchRecord", "catchRecord.location", "account" })
	Page<CatchComment> findByParent_Account_IdAndAccount_IdNotOrderByIdDesc(
			Long parentAccountId, Long excludeAuthorId, Pageable pageable);
}
