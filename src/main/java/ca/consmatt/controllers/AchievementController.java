package ca.consmatt.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.dto.AchievementProgressResponse;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.service.AchievementService;
import lombok.RequiredArgsConstructor;

/**
 * Read-only achievements + role/XP API. Writes happen as side-effects of other mutations
 * (catches, likes, comments, friend adds) — there is no public endpoint to manually unlock.
 */
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

	private final AccountRepository accountRepository;
	private final AchievementService achievementService;

	/**
	 * Achievements + role/XP for the authenticated user.
	 */
	@GetMapping("/me")
	public AchievementProgressResponse getMyAchievements(Authentication authentication) {
		Account current = accountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		return achievementService.getProgress(current, AchievementService.AdminCheck.byRole());
	}

	/**
	 * Achievements + role/XP for any user (visible to any authenticated viewer).
	 *
	 * @param id account primary key
	 */
	@GetMapping("/users/{id:\\d+}")
	public ResponseEntity<AchievementProgressResponse> getAccountAchievements(@PathVariable Long id) {
		return accountRepository.findById(id)
				.map(account -> ResponseEntity.ok(
						achievementService.getProgress(account, AchievementService.AdminCheck.byRole())))
				.orElse(ResponseEntity.notFound().build());
	}
}
