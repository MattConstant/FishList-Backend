package ca.consmatt.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.ForumThreadLike;

/**
 * Persistence for per-account likes on forum threads.
 */
public interface ForumThreadLikeRepository extends JpaRepository<ForumThreadLike, Long> {

	long countByThread_Id(Long threadId);

	boolean existsByThread_IdAndAccount_Id(Long threadId, Long accountId);

	Optional<ForumThreadLike> findByThread_IdAndAccount_Id(Long threadId, Long accountId);

	void deleteByThread_Id(Long threadId);

	void deleteByAccount_Id(Long accountId);
}
