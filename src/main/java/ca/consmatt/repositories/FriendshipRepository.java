package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.Friendship;

/**
 * Persistence for account friendship links.
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

	Optional<Friendship> findByAccountA_IdAndAccountB_Id(Long accountAId, Long accountBId);

	List<Friendship> findByAccountA_IdOrAccountB_Id(Long accountAId, Long accountBId);
}
