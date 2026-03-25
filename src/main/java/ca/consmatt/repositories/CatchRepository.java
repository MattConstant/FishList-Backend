package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ca.consmatt.beans.Catch;

/**
 * Persistence for {@link Catch} entities.
 */
public interface CatchRepository extends JpaRepository<Catch, Long> {

	List<Catch> findByLocation_IdOrderByIdAsc(Long locationId);

	Optional<Catch> findByIdAndLocation_Id(Long id, Long locationId);

	/** Removes all catches for the given location (used before deleting the location). */
	void deleteByLocation_Id(Long locationId);
}
