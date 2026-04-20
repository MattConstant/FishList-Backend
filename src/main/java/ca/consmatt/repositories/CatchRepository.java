package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.consmatt.beans.Catch;
import ca.consmatt.beans.PostVisibility;
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
				c.imageUrlsRaw,
				c.fishDetailsJson,
				l.visibility
			)
			from Catch c
			join c.location l
			join l.account a
			where (l.visibility is null or l.visibility = :publicVis)
				or a.id = :viewerId
				or (l.visibility = :friendsVis and (
					exists (select 1 from Friendship f where f.accountA.id = a.id and f.accountB.id = :viewerId)
					or exists (select 1 from Friendship f2 where f2.accountA.id = :viewerId and f2.accountB.id = a.id)
				))
			order by l.timeStamp desc, c.id desc
			""")
	List<FeedPostResponse> findFeedPostsForViewer(
			@Param("viewerId") Long viewerId,
			@Param("publicVis") PostVisibility publicVis,
			@Param("friendsVis") PostVisibility friendsVis,
			Pageable pageable);

	/** Removes all catches for the given location (used before deleting the location). */
	void deleteByLocation_Id(Long locationId);

	long countByLocation_Account_Id(Long accountId);
}
