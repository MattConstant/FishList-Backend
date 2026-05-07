package ca.consmatt.controllers;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.AccountRole;
import ca.consmatt.beans.CampSpot;
import ca.consmatt.beans.PostVisibility;
import ca.consmatt.dto.CampSpotResponse;
import ca.consmatt.dto.CreateCampSpotRequest;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CampSpotRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Camp spot pins for the map. These are separate from catches and do not appear in the feed.
 */
@RestController
@RequestMapping("/api/camps")
@RequiredArgsConstructor
@Validated
public class CampSpotController {

	private final CampSpotRepository campSpotRepository;
	private final AccountRepository accountRepository;

	/**
	 * Map-visible camp spots for the current user (PUBLIC + FRIENDS + mine).
	 */
	@GetMapping("/visible")
	@Transactional(readOnly = true)
	public List<CampSpotResponse> listVisible(
			@RequestParam(name = "limit", defaultValue = "500") @Min(1) @Max(1000) int limit,
			Authentication authentication) {
		Account viewer = requireAccount(authentication);
		int normalizedLimit = Math.min(Math.max(limit, 1), 1000);
		return campSpotRepository.findVisibleForViewer(
				viewer.getId(),
				PostVisibility.PUBLIC,
				PostVisibility.FRIENDS,
				PageRequest.of(0, normalizedLimit))
				.stream()
				.map(CampSpotController::toResponse)
				.toList();
	}

	@PostMapping({ "", "/" })
	public ResponseEntity<CampSpotResponse> create(@Valid @RequestBody CreateCampSpotRequest request,
			Authentication authentication) {
		Account owner = requireAccount(authentication);
		CampSpot saved = campSpotRepository.save(request.toNewEntity(owner));
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
		Account actor = requireAccount(authentication);
		CampSpot spot = campSpotRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camp spot not found"));
		boolean isOwner = spot.getAccount() != null && actor.getId().equals(spot.getAccount().getId());
		if (!isOwner && actor.getRole() != AccountRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this camp spot");
		}
		campSpotRepository.delete(spot);
		return ResponseEntity.noContent().build();
	}

	private Account requireAccount(Authentication authentication) {
		return accountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	private static CampSpotResponse toResponse(CampSpot c) {
		Long accountId = c.getAccount() != null ? c.getAccount().getId() : null;
		String username = c.getAccount() != null ? c.getAccount().getUsername() : "unknown";
		return new CampSpotResponse(
				c.getId(),
				c.getName(),
				c.getLatitude(),
				c.getLongitude(),
				c.getTimeStamp(),
				accountId,
				username,
				c.getVisibility(),
				c.getImageUrls() != null ? c.getImageUrls() : Collections.emptyList());
	}
}

