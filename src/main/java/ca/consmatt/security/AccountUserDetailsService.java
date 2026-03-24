package ca.consmatt.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ca.consmatt.beans.Account;
import ca.consmatt.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;

/**
 * Bridges {@link Account} rows to Spring Security {@link UserDetails}.
 */
@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

	private final AccountRepository accountRepository;

	/**
	 * {@inheritDoc}
	 *
	 * @throws UsernameNotFoundException if no account matches {@code username}
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Account account = accountRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return User.builder()
				.username(account.getUsername())
				.password(account.getPassword())
				.roles("USER")
				.build();
	}
}
