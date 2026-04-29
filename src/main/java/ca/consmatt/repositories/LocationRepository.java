package ca.consmatt.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.consmatt.beans.Location;

/**
 * Persistence for {@link Location} entities.
 */
public interface LocationRepository extends JpaRepository<Location, Long> {

	/**
	 * @param accountId owner account primary key
	 * @return locations owned by that account
	 */
	List<Location> findByAccount_Id(Long accountId);

	long countByAccount_Id(Long accountId);

	/**
	 * Latitude/longitude string pairs for every location <i>not</i> owned by {@code accountId}.
	 * Used by the trailblazer achievement: callers parse and round these to detect spots no other
	 * angler has logged. Returned as raw strings (not parsed) since lat/lng are stored that way.
	 */
	@Query("select l.latitude, l.longitude from Location l where l.account.id <> :accountId")
	List<Object[]> findOtherAccountsLatLng(@Param("accountId") Long accountId);
}
