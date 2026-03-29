package ca.consmatt.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
