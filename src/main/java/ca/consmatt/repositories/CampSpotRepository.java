package ca.consmatt.repositories;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.consmatt.beans.CampSpot;
import ca.consmatt.beans.PostVisibility;

/**
 * Persistence for {@link CampSpot} entities.
 */
public interface CampSpotRepository extends JpaRepository<CampSpot, Long> {

	@Query("""
			select c
			from CampSpot c
			join c.account a
			where (c.visibility is null or c.visibility = :publicVis)
				or a.id = :viewerId
				or (c.visibility = :friendsVis and (
					exists (select 1 from Friendship f where f.accountA.id = a.id and f.accountB.id = :viewerId)
					or exists (select 1 from Friendship f2 where f2.accountA.id = :viewerId and f2.accountB.id = a.id)
				))
			order by c.timeStamp desc, c.id desc
			""")
	List<CampSpot> findVisibleForViewer(
			@Param("viewerId") Long viewerId,
			@Param("publicVis") PostVisibility publicVis,
			@Param("friendsVis") PostVisibility friendsVis,
			Pageable pageable);
}

