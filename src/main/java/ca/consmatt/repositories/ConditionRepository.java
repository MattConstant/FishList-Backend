package ca.consmatt.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.Condition;

/** Persistence for {@link Condition} snapshots. */
public interface ConditionRepository extends JpaRepository<Condition, Long> {

}
