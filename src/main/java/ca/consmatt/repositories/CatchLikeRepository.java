package ca.consmatt.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.consmatt.beans.CatchLike;

/**
 * Persistence for per-account likes on catches.
 */
public interface CatchLikeRepository extends JpaRepository<CatchLike, Long> {

	long countByCatchRecord_Id(Long catchId);

	boolean existsByCatchRecord_IdAndAccount_Id(Long catchId, Long accountId);

	Optional<CatchLike> findByCatchRecord_IdAndAccount_Id(Long catchId, Long accountId);

	void deleteByCatchRecord_Id(Long catchId);

	void deleteByCatchRecord_Location_Id(Long locationId);

	long countByAccount_Id(Long accountId);

	void deleteByAccount_Id(Long accountId);

	/** Total likes left on catches owned by the given account (used by the POPULAR_CATCH achievement). */
	@Query("select count(cl) from CatchLike cl join cl.catchRecord c join c.location l where l.account.id = :accountId")
	long countLikesReceivedByAccountId(@Param("accountId") Long accountId);
}
