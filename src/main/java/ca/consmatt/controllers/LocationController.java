package ca.consmatt.controllers;

import java.util.List;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.Catch;
import ca.consmatt.beans.CatchComment;
import ca.consmatt.beans.CatchLike;
import ca.consmatt.beans.Location;
import ca.consmatt.dto.AddCatchRequest;
import ca.consmatt.dto.CatchCommentResponse;
import ca.consmatt.dto.CatchCommentsPageResponse;
import ca.consmatt.dto.CatchLikeResponse;
import ca.consmatt.dto.CreateCatchCommentRequest;
import ca.consmatt.dto.FeedPostResponse;
import ca.consmatt.dto.LocationDetailResponse;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CatchCommentRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.CatchLikeRepository;
import ca.consmatt.repositories.LocationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * REST API for fishing locations and nested catch records. Create, update, and delete require
 * ownership; reads are available to any authenticated user unless noted.
 */
@RestController
@RequestMapping("/api/locations")
@CrossOrigin
@RequiredArgsConstructor
@Validated
public class LocationController {

	private final LocationRepository locationRepo;
	private final AccountRepository accountRepository;
	private final CatchRepository catchRepo;
	private final CatchLikeRepository catchLikeRepo;
	private final CatchCommentRepository catchCommentRepo;

	/**
	 * Lists all locations (summary rows include {@code accountId}).
	 *
	 * @return every stored location
	 */
	@GetMapping({ "", "/" })
	public List<Location> getLocationCollection() {
		return locationRepo.findAll();
	}

	/**
	 * Paginated latest feed rows (flattened catches + location + owner), newest first.
	 */
	@GetMapping("/feed")
	public List<FeedPostResponse> getFeedPosts(
			@RequestParam(name = "offset", defaultValue = "0") @Min(value = 0, message = "offset must be >= 0") int offset,
			@RequestParam(name = "limit", defaultValue = "24") @Min(value = 1, message = "limit must be >= 1") @Max(value = 100, message = "limit must be <= 100") int limit) {
		int normalizedOffset = Math.max(offset, 0);
		int normalizedLimit = Math.min(Math.max(limit, 1), 100);
		int page = normalizedOffset / normalizedLimit;
		int pageOffset = page * normalizedLimit;

		// Query one page and trim in-memory if offset isn't page-aligned.
		List<FeedPostResponse> batch = catchRepo.findFeedPosts(PageRequest.of(page, normalizedLimit));
		if (normalizedOffset == pageOffset) {
			return batch;
		}
		int skip = normalizedOffset - pageOffset;
		if (skip >= batch.size()) {
			return List.of();
		}
		return batch.subList(skip, batch.size());
	}

	/**
	 * Lists catch records for a location, ordered by id.
	 *
	 * @param id location primary key
	 * @return catches or 404 if the location is missing
	 */
	@GetMapping("/{id}/catches")
	public ResponseEntity<List<Catch>> getCatchesForLocation(@PathVariable Long id) {
		if (!locationRepo.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(catchRepo.findByLocation_IdOrderByIdAsc(id));
	}

	/**
	 * Returns one location with its catches.
	 *
	 * @param id location primary key
	 * @return detail payload or 404
	 */
	@GetMapping("/{id}")
	public ResponseEntity<LocationDetailResponse> getLocationById(@PathVariable Long id) {
		return locationRepo.findById(id)
				.map(loc -> ResponseEntity.ok(LocationDetailResponse.from(loc,
						catchRepo.findByLocation_IdOrderByIdAsc(id))))
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Creates a location for the authenticated user (owner is set server-side).
	 *
	 * @param location JSON body (no {@code account} field required)
	 * @param authentication current user
	 * @return 201 with message containing new id
	 */
	@PostMapping({ "", "/" })
	public ResponseEntity<String> createLocation(@Valid @RequestBody Location location, Authentication authentication) {
		Account account = requireAccount(authentication);
		location.setAccount(account);
		Location saved = locationRepo.save(location);
		return ResponseEntity.status(HttpStatus.CREATED).body("Record added at index " + saved.getId());
	}

	/**
	 * Adds a self-contained catch to a location (owner only).
	 *
	 * @param id location id
	 * @param request species, optional quantity and measurements
	 * @param authentication current user
	 * @return 201 with saved entity, 404/403 as appropriate
	 */
	@PostMapping("/{id}/catches")
	public ResponseEntity<Catch> addCatch(@PathVariable Long id, @Valid @RequestBody AddCatchRequest request,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		Location location = locationRepo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
		assertLocationOwner(location, account);
		int quantity = request.quantity() != null ? request.quantity() : 1;
		List<String> imageUrls = normalizeImageUrls(request.imageUrls(), request.imageUrl());
		Catch catchEntity = Catch.builder()
				.location(location)
				.species(request.species().trim())
				.quantity(quantity)
				.lengthCm(request.lengthCm())
				.weightKg(request.weightKg())
				.notes(request.notes())
				.imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
				.imageUrlsRaw(String.join("\n", imageUrls))
				.description(request.description())
				.build();
		return ResponseEntity.status(HttpStatus.CREATED).body(catchRepo.save(catchEntity));
	}

	/**
	 * Lists comments for one catch.
	 */
	@GetMapping("/{id}/catches/{catchId}/comments")
	public CatchCommentsPageResponse getCatchComments(@PathVariable Long id, @PathVariable Long catchId,
			@RequestParam(name = "offset", defaultValue = "0") @Min(value = 0, message = "offset must be >= 0") int offset,
			@RequestParam(name = "limit", defaultValue = "3") @Min(value = 1, message = "limit must be >= 1") @Max(value = 20, message = "limit must be <= 20") int limit,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		Catch catchEntity = requireCatchInLocation(id, catchId);
		int normalizedOffset = Math.max(offset, 0);
		int normalizedLimit = Math.min(Math.max(limit, 1), 20);
		int requestWindow = normalizedOffset + normalizedLimit;

		Page<CatchComment> pageResult = catchCommentRepo.findByCatchRecord_IdOrderByIdDesc(
				catchEntity.getId(),
				PageRequest.of(0, requestWindow));

		List<CatchCommentResponse> comments = pageResult.getContent()
				.stream()
				.skip(normalizedOffset)
				.limit(normalizedLimit)
				.map(comment -> toCatchCommentResponse(comment, account))
				.toList();

		return new CatchCommentsPageResponse(
				comments,
				pageResult.getTotalElements(),
				normalizedOffset,
				normalizedLimit);
	}

	/**
	 * Adds a comment on a catch.
	 */
	@PostMapping("/{id}/catches/{catchId}/comments")
	public ResponseEntity<CatchCommentResponse> addCatchComment(@PathVariable Long id, @PathVariable Long catchId,
			@Valid @RequestBody CreateCatchCommentRequest request, Authentication authentication) {
		Account account = requireAccount(authentication);
		Catch catchEntity = requireCatchInLocation(id, catchId);
		CatchComment saved = catchCommentRepo.save(new CatchComment(
				null,
				catchEntity,
				account,
				request.message().trim(),
				Instant.now().toString()));
		return ResponseEntity.status(HttpStatus.CREATED).body(toCatchCommentResponse(saved, account));
	}

	/**
	 * Deletes one comment. Allowed for the comment owner or location owner.
	 */
	@DeleteMapping("/{id}/catches/{catchId}/comments/{commentId}")
	public ResponseEntity<Void> deleteCatchComment(@PathVariable Long id, @PathVariable Long catchId,
			@PathVariable Long commentId, Authentication authentication) {
		Account account = requireAccount(authentication);
		Catch catchEntity = requireCatchInLocation(id, catchId);
		CatchComment comment = catchCommentRepo.findByIdAndCatchRecord_Id(commentId, catchEntity.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

		boolean isCommentOwner = comment.getAccount() != null && account.getId().equals(comment.getAccount().getId());
		boolean isLocationOwner = catchEntity.getLocation() != null
				&& catchEntity.getLocation().getAccount() != null
				&& account.getId().equals(catchEntity.getLocation().getAccount().getId());
		if (!isCommentOwner && !isLocationOwner) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this comment");
		}

		catchCommentRepo.delete(comment);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Returns likes count and whether the current user has liked this catch.
	 */
	@GetMapping("/{id}/catches/{catchId}/like")
	public CatchLikeResponse getCatchLike(@PathVariable Long id, @PathVariable Long catchId,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		Catch catchEntity = requireCatchInLocation(id, catchId);
		long likesCount = catchLikeRepo.countByCatchRecord_Id(catchEntity.getId());
		boolean likedByMe = catchLikeRepo.existsByCatchRecord_IdAndAccount_Id(catchEntity.getId(), account.getId());
		return new CatchLikeResponse(catchEntity.getId(), likesCount, likedByMe);
	}

	/**
	 * Adds a like for the current user (idempotent).
	 */
	@PostMapping("/{id}/catches/{catchId}/like")
	public CatchLikeResponse likeCatch(@PathVariable Long id, @PathVariable Long catchId,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		Catch catchEntity = requireCatchInLocation(id, catchId);

		boolean alreadyLiked = catchLikeRepo.existsByCatchRecord_IdAndAccount_Id(catchEntity.getId(), account.getId());
		if (!alreadyLiked) {
			catchLikeRepo.save(new CatchLike(null, account, catchEntity));
		}

		long likesCount = catchLikeRepo.countByCatchRecord_Id(catchEntity.getId());
		return new CatchLikeResponse(catchEntity.getId(), likesCount, true);
	}

	/**
	 * Removes a like for the current user (idempotent).
	 */
	@DeleteMapping("/{id}/catches/{catchId}/like")
	public CatchLikeResponse unlikeCatch(@PathVariable Long id, @PathVariable Long catchId,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		Catch catchEntity = requireCatchInLocation(id, catchId);

		catchLikeRepo.findByCatchRecord_IdAndAccount_Id(catchEntity.getId(), account.getId())
				.ifPresent(catchLikeRepo::delete);

		long likesCount = catchLikeRepo.countByCatchRecord_Id(catchEntity.getId());
		return new CatchLikeResponse(catchEntity.getId(), likesCount, false);
	}

	/**
	 * Deletes one catch from a location (owner only).
	 *
	 * @param id location id
	 * @param catchId catch id
	 * @param authentication current user
	 * @return 204 on success, 404/403 otherwise
	 */
	@DeleteMapping("/{id}/catches/{catchId}")
	public ResponseEntity<Void> deleteCatch(@PathVariable Long id, @PathVariable Long catchId,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		Location location = locationRepo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
		assertLocationOwner(location, account);
		Catch catchEntity = catchRepo.findByIdAndLocation_Id(catchId, id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catch not found"));
		catchCommentRepo.deleteByCatchRecord_Id(catchEntity.getId());
		catchLikeRepo.deleteByCatchRecord_Id(catchEntity.getId());
		catchRepo.delete(catchEntity);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Deletes a location and all its catches (owner only).
	 *
	 * @param id location id
	 * @param authentication current user
	 * @return plain-text confirmation or error status
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteById(@PathVariable Long id, Authentication authentication) {
		Account account = requireAccount(authentication);
		Location location = locationRepo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
		assertLocationOwner(location, account);
		catchCommentRepo.deleteByCatchRecord_Location_Id(id);
		catchLikeRepo.deleteByCatchRecord_Location_Id(id);
		catchRepo.deleteByLocation_Id(id);
		locationRepo.deleteById(id);
		return ResponseEntity.ok("deleted");
	}

	/**
	 * Updates mutable location fields (owner only).
	 *
	 * @param id location id
	 * @param updatedLocation replacement field values
	 * @param authentication current user
	 * @return updated entity
	 */
	@PutMapping("/{id}")
	public Location updateLocation(@PathVariable Long id, @Valid @RequestBody Location updatedLocation,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		return locationRepo.findById(id).map(location -> {
			assertLocationOwner(location, account);
			location.setLocationName(updatedLocation.getLocationName());
			location.setLatitude(updatedLocation.getLatitude());
			location.setLongitude(updatedLocation.getLongitude());
			location.setTimeStamp(updatedLocation.getTimeStamp());
			location.setDetails(updatedLocation.getDetails());
			return locationRepo.save(location);
		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
	}

	/**
	 * Resolves the {@link Account} for the authenticated username.
	 *
	 * @param authentication Spring Security context
	 * @return persisted account
	 */
	private Account requireAccount(Authentication authentication) {
		return accountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	private Catch requireCatchInLocation(Long locationId, Long catchId) {
		if (!locationRepo.existsById(locationId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found");
		}
		return catchRepo.findByIdAndLocation_Id(catchId, locationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catch not found"));
	}

	private CatchCommentResponse toCatchCommentResponse(CatchComment comment, Account currentAccount) {
		Long accountId = comment.getAccount() != null ? comment.getAccount().getId() : null;
		String username = comment.getAccount() != null ? comment.getAccount().getUsername() : "unknown";
		boolean ownedByMe = accountId != null
				&& currentAccount != null
				&& accountId.equals(currentAccount.getId());
		return new CatchCommentResponse(
				comment.getId(),
				comment.getCatchRecord() != null ? comment.getCatchRecord().getId() : null,
				accountId,
				username,
				comment.getMessage(),
				comment.getCreatedAt(),
				ownedByMe);
	}

	/**
	 * Ensures the location belongs to the given account.
	 *
	 * @param location location entity (account may be lazy-loaded)
	 * @param account expected owner
	 */
	private void assertLocationOwner(Location location, Account account) {
		if (location.getAccount() == null || !location.getAccount().getId().equals(account.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this location");
		}
	}

	private List<String> normalizeImageUrls(List<String> imageUrls, String imageUrl) {
		if (imageUrls != null && !imageUrls.isEmpty()) {
			return imageUrls.stream()
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.limit(4)
					.toList();
		}
		if (imageUrl != null && !imageUrl.isBlank()) {
			return List.of(imageUrl.trim());
		}
		return List.of();
	}
}
