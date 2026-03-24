package ca.consmatt.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.Fish;

/** Persistence for {@link Fish} species catalog rows. */
public interface FishRepository extends JpaRepository<Fish, Long> {

}
