package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import ca.consmatt.beans.Catch;
import ca.consmatt.dto.FeedPostResponse;

/**
 * Persistence for {@link Catch} entities.
 */
public interface CatchRepository extends JpaRepository<Catch, Long> {

	List<Catch> findByLocation_IdOrderByIdAsc(Long locationId);

	Optional<Catch> findByIdAndLocation_Id(Long id, Long locationId);

	@Query("""
			select new ca.consmatt.dto.FeedPostResponse(
				l.id,
				l.locationName,
				l.latitude,
				l.longitude,
				l.timeStamp,
				a.id,
				a.username,
				c.id,
				c.species,
				c.quantity,
				c.lengthCm,
				c.weightKg,
				c.notes,
				c.description,
				c.imageUrl,
				c.imageUrlsRaw
			)
			from Catch c
			join c.location l
			join l.account a
			order by l.timeStamp desc, c.id desc
			""")
	List<FeedPostResponse> findFeedPosts(Pageable pageable);

	/** Removes all catches for the given location (used before deleting the location). */
	void deleteByLocation_Id(Long locationId);
}
