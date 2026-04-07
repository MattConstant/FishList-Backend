package ca.consmatt.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.AccountRole;
import ca.consmatt.beans.Location;
import ca.consmatt.dto.AdminAccountRowResponse;
import ca.consmatt.dto.AdminMeResponse;
import ca.consmatt.dto.AdminSummaryResponse;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CatchCommentRepository;
import ca.consmatt.repositories.CatchLikeRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.FriendshipRepository;
import ca.consmatt.repositories.LocationRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Admin-only operations for moderation and basic operational visibility.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
@RequiredArgsConstructor
@Validated
public class AdminController {

	private static final Logger log = LoggerFactory.getLogger(AdminController.class);

	private final AccountRepository accountRepository;
	private final LocationRepository locationRepository;
	private final CatchRepository catchRepository;
	private final CatchCommentRepository catchCommentRepository;
	private final CatchLikeRepository catchLikeRepository;
	private final FriendshipRepository friendshipRepository;

	@GetMapping("/me")
	public AdminMeResponse me(Authentication authentication) {
		Account current = requireAccount(authentication);
		return new AdminMeResponse(current.getUsername(), current.getRole() == AccountRole.ADMIN);
	}

	@GetMapping("/summary")
	public AdminSummaryResponse summary(Authentication authentication) {
		requireAdmin(authentication);
		return new AdminSummaryResponse(
				accountRepository.count(),
				locationRepository.count(),
				catchRepository.count(),
				catchCommentRepository.count(),
				catchLikeRepository.count(),
				friendshipRepository.count());
	}

	@GetMapping("/accounts")
	public List<AdminAccountRowResponse> listAccounts(
			@RequestParam(name = "query", defaultValue = "") @Size(max = 100, message = "query length must be <= 100") String query,
			@RequestParam(name = "limit", defaultValue = "50") @Min(value = 1, message = "limit must be >= 1") @Max(value = 100, message = "limit must be <= 100") int limit,
			Authentication authentication) {
		requireAdmin(authentication);
		String normalized = query == null ? "" : query.trim();
		List<Account> candidates = normalized.isBlank()
				? accountRepository.findTop200ByOrderByUsernameAsc()
				: accountRepository.findTop200ByUsernameContainingIgnoreCaseOrderByUsernameAsc(normalized);
		return candidates.stream()
				.limit(limit)
				.map(a -> new AdminAccountRowResponse(
						a.getId(),
						a.getUsername(),
						locationRepository.countByAccount_Id(a.getId()),
						catchRepository.countByLocation_Account_Id(a.getId()),
						catchCommentRepository.countByAccount_Id(a.getId()),
						catchLikeRepository.countByAccount_Id(a.getId())))
				.toList();
	}

	@DeleteMapping("/accounts/{id}")
	@Transactional
	public ResponseEntity<Void> deleteAccount(@PathVariable Long id, Authentication authentication) {
		Account admin = requireAdmin(authentication);
		Account target = accountRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Account not found"));
		if (admin.getId().equals(target.getId())) {
			throw new ResponseStatusException(BAD_REQUEST, "Admins cannot delete their own account");
		}
		if (target.getRole() == AccountRole.ADMIN) {
			throw new ResponseStatusException(FORBIDDEN, "Cannot delete another admin account");
		}

		List<Location> ownedLocations = locationRepository.findByAccount_Id(target.getId());
		log.warn("ADMIN_DELETE_ACCOUNT_START admin={} target={} ownedLocations={}",
				admin.getUsername(), target.getUsername(), ownedLocations.size());
		for (Location location : ownedLocations) {
			Long locationId = location.getId();
			catchCommentRepository.deleteByCatchRecord_Location_Id(locationId);
			catchLikeRepository.deleteByCatchRecord_Location_Id(locationId);
			catchRepository.deleteByLocation_Id(locationId);
			locationRepository.deleteById(locationId);
		}

		catchCommentRepository.deleteByAccount_Id(target.getId());
		catchLikeRepository.deleteByAccount_Id(target.getId());
		friendshipRepository.deleteByAccountA_IdOrAccountB_Id(target.getId(), target.getId());
		accountRepository.deleteById(target.getId());
		log.warn("ADMIN_DELETE_ACCOUNT_SUCCESS admin={} target={}", admin.getUsername(), target.getUsername());
		return ResponseEntity.noContent().build();
	}

	private Account requireAdmin(Authentication authentication) {
		Account account = requireAccount(authentication);
		if (account.getRole() != AccountRole.ADMIN) {
			throw new ResponseStatusException(FORBIDDEN, "Admin access required");
		}
		return account;
	}

	private Account requireAccount(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new ResponseStatusException(UNAUTHORIZED);
		}
		return accountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
	}
}
