package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.consmatt.beans.ForumThreadComment;

/**
 * Persistence for comments on forum threads.
 */
public interface ForumThreadCommentRepository extends JpaRepository<ForumThreadComment, Long> {

	Page<ForumThreadComment> findByThread_IdOrderByIdDesc(Long threadId, Pageable pageable);

	Page<ForumThreadComment> findByThread_IdOrderByIdAsc(Long threadId, Pageable pageable);

	List<ForumThreadComment> findByParent_Id(Long parentCommentId);

	Optional<ForumThreadComment> findByIdAndThread_Id(Long id, Long threadId);

	void deleteByThread_Id(Long threadId);

	void deleteByAccount_Id(Long accountId);

	/** Top-level comments on threads owned by the given account (for "new comment on your discussion" notifications). */
	Page<ForumThreadComment> findByThread_Account_IdAndParentIsNullAndAccount_IdNotOrderByIdDesc(
			Long threadOwnerId,
			Long excludeAccountId,
			Pageable pageable);

	/** Replies to comments written by the given account. */
	Page<ForumThreadComment> findByParent_Account_IdAndAccount_IdNotOrderByIdDesc(
			Long parentAuthorId,
			Long excludeAccountId,
			Pageable pageable);

	long countByThread_Id(Long threadId);

	@Query("select count(c) from ForumThreadComment c where c.thread.account.id = :accountId")
	long countOnThreadsOwnedByAccountId(@Param("accountId") Long accountId);
}
