package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.Account;

/**
 * Persistence for {@link Account} entities.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUsername(String username);

	Optional<Account> findByUsernameIgnoreCase(String username);

	Optional<Account> findByGoogleSub(String googleSub);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	Optional<Account> findByEmail(String email);

	Optional<Account> findByEmailVerificationToken(String token);

	Optional<Account> findByPasswordResetToken(String token);

	List<Account> findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);

	Page<Account> findAllByOrderByUsernameAsc(Pageable pageable);

	Page<Account> findByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username, Pageable pageable);

}
