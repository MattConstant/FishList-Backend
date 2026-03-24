package ca.consmatt.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

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
}


