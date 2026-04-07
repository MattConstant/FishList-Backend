package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.Account;

/**
 * Persistence for {@link Account} entities.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUsername(String username);

	boolean existsByUsername(String username);

	List<Account> findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);

	List<Account> findTop200ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);

	List<Account> findTop200ByOrderByUsernameAsc();

}

