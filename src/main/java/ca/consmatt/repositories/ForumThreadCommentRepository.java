package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

	/** Replies to comments written by the given account. */
	Page<ForumThreadComment> findByParent_Account_IdAndAccount_IdNotOrderByIdDesc(
			Long parentAuthorId,
			Long excludeAccountId,
			Pageable pageable);

	long countByThread_Id(Long threadId);
}
