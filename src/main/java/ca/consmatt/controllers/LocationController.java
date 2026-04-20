package ca.consmatt.controllers;

import java.util.List;
import java.util.stream.Collectors;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.AccountRole;
import ca.consmatt.beans.Catch;
import ca.consmatt.beans.CatchComment;
import ca.consmatt.beans.CatchLike;
import ca.consmatt.beans.Location;
import ca.consmatt.dto.AddCatchRequest;
import ca.consmatt.dto.FishEntryRequest;
import ca.consmatt.dto.CreateLocationRequest;
import ca.consmatt.dto.CatchCommentResponse;
import ca.consmatt.dto.CatchCommentsPageResponse;
import ca.consmatt.dto.CatchLikeResponse;
import ca.consmatt.dto.CreateCatchCommentRequest;
import ca.consmatt.dto.FeedPostResponse;
import ca.consmatt.dto.LocationDetailResponse;
import ca.consmatt.beans.PostVisibility;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CatchCommentRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.CatchLikeRepository;
import ca.consmatt.repositories.FriendshipRepository;
import ca.consmatt.repositories.LocationRepository;
import ca.consmatt.storage.ImageStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * REST API for fishing locations and nested catch records. Create, update, and delete require
 * ownership (or admin for catch delete); reads are available to any authenticated user unless noted.
 */
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Validated
public class LocationController {

	private static final Logger log = LoggerFactory.getLogger(LocationController.class);

	/** Local mapper — avoids requiring a Spring {@code ObjectMapper} bean (some images exclude Jackson auto-config). */
	private static final ObjectMapper FISH_DETAILS_JSON = new ObjectMapper();

	private final LocationRepository locationRepo;
	private final AccountRepository accountRepository;
	private final CatchRepository catchRepo;
	private final CatchLikeRepository catchLikeRepo;
	private final CatchCommentRepository catchCommentRepo;
	private final FriendshipRepository friendshipRepository;
	private final ObjectProvider<ImageStorageService> imageStorageService;

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
			@RequestParam(name = "limit", defaultValue = "24") @Min(value = 1, message = "limit must be >= 1") @Max(value = 100, message = "limit must be <= 100") int limit,
			Authentication authentication) {
		int normalizedOffset = Math.max(offset, 0);
		int normalizedLimit = Math.min(Math.max(limit, 1), 100);
		int page = normalizedOffset / normalizedLimit;
		int pageOffset = page * normalizedLimit;

		Account viewer = requireAccount(authentication);
		// Query one page and trim in-memory if offset isn't page-aligned.
		List<FeedPostResponse> batch = catchRepo.findFeedPostsForViewer(
				viewer.getId(),
				ca.consmatt.beans.PostVisibility.PUBLIC,
				ca.consmatt.beans.PostVisibility.FRIENDS,
				PageRequest.of(page, normalizedLimit));
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
	public ResponseEntity<List<Catch>> getCatchesForLocation(@PathVariable Long id, Authentication authentication) {
		Account viewer = requireAccount(authentication);
		Location location = locationRepo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
		if (!canViewLocation(location, viewer)) {
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
	public ResponseEntity<LocationDetailResponse> getLocationById(@PathVariable Long id,
			Authentication authentication) {
		Account viewer = requireAccount(authentication);
		return locationRepo.findById(id)
				.filter(loc -> canViewLocation(loc, viewer))
				.map(loc -> ResponseEntity.ok(LocationDetailResponse.from(loc,
						catchRepo.findByLocation_IdOrderByIdAsc(id))))
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Creates a location for the authenticated user (owner is set server-side).
	 *
	 * @param request location fields (not a persistent entity; no {@code account} in body)
	 * @param authentication current user
	 * @return 201 with message containing new id
	 */
	@PostMapping({ "", "/" })
	public ResponseEntity<String> createLocation(@Valid @RequestBody CreateLocationRequest request,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		Location saved = locationRepo.save(request.toNewLocation(account));
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
		List<FishEntryRequest> lines = resolveFishLines(request);
		List<String> imageUrls = normalizeImageUrls(request.imageUrls(), request.imageUrl());

		String speciesOut;
		int quantityOut;
		Double lengthOut;
		Double weightOut;
		String notesOut;
		String fishDetailsJsonOut = null;

		if (lines.size() == 1) {
			FishEntryRequest a = lines.get(0);
			speciesOut = a.species().trim();
			quantityOut = request.quantity() != null ? request.quantity() : 1;
			lengthOut = a.lengthCm();
			weightOut = a.weightKg();
			notesOut = a.notes();
		} else {
			speciesOut = lines.stream().map(l -> l.species().trim()).collect(Collectors.joining(", "));
			quantityOut = lines.size();
			lengthOut = null;
			weightOut = null;
			notesOut = null;
			try {
				fishDetailsJsonOut = FISH_DETAILS_JSON.writeValueAsString(lines);
			} catch (JsonProcessingException e) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize fish details");
			}
		}

		Catch catchEntity = Catch.builder()
				.location(location)
				.species(speciesOut)
				.quantity(quantityOut)
				.lengthCm(lengthOut)
				.weightKg(weightOut)
				.notes(notesOut)
				.fishDetailsJson(fishDetailsJsonOut)
				.imageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0))
				.imageUrlsRaw(String.join("\n", imageUrls))
				.description(request.description())
				.build();
		return ResponseEntity.status(HttpStatus.CREATED).body(catchRepo.save(catchEntity));
	}

	private List<FishEntryRequest> resolveFishLines(AddCatchRequest request) {
		if (request.fish() != null && !request.fish().isEmpty()) {
			return request.fish();
		}
		if (request.species() == null || request.species().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "species or fish list is required");
		}
		return List.of(new FishEntryRequest(
				request.species().trim(),
				request.lengthCm(),
				request.weightKg(),
				request.notes()));
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
		Catch catchEntity = requireVisibleCatchInLocation(id, catchId, account);
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
		Catch catchEntity = requireVisibleCatchInLocation(id, catchId, account);
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
		Catch catchEntity = requireVisibleCatchInLocation(id, catchId, account);
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
		Catch catchEntity = requireVisibleCatchInLocation(id, catchId, account);
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
		Catch catchEntity = requireVisibleCatchInLocation(id, catchId, account);

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
		Catch catchEntity = requireVisibleCatchInLocation(id, catchId, account);

		catchLikeRepo.findByCatchRecord_IdAndAccount_Id(catchEntity.getId(), account.getId())
				.ifPresent(catchLikeRepo::delete);

		long likesCount = catchLikeRepo.countByCatchRecord_Id(catchEntity.getId());
		return new CatchLikeResponse(catchEntity.getId(), likesCount, false);
	}

	/**
	 * Deletes one catch from a location (location owner, or an admin moderating any post).
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
		assertLocationOwnerOrAdmin(location, account);
		Catch catchEntity = catchRepo.findByIdAndLocation_Id(catchId, id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catch not found"));
		for (String key : catchEntity.getImageUrlsList()) {
			if (isBlobObjectKey(key)) {
				imageStorageService.ifAvailable(svc -> {
					try {
						svc.deleteUploadedObject(key);
					} catch (Exception ex) {
						log.warn("DELETE_CATCH_STORAGE_FAILED key={} msg={}", key, ex.getMessage());
					}
				});
			}
		}
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
	 * @param request replacement field values (not a persistent entity)
	 * @param authentication current user
	 * @return updated entity
	 */
	@PutMapping("/{id}")
	public Location updateLocation(@PathVariable Long id, @Valid @RequestBody CreateLocationRequest request,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		return locationRepo.findById(id).map(location -> {
			assertLocationOwner(location, account);
			request.applyTo(location);
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

	private Catch requireVisibleCatchInLocation(Long locationId, Long catchId, Account viewer) {
		Catch c = requireCatchInLocation(locationId, catchId);
		Location loc = c.getLocation();
		if (loc == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catch not found");
		}
		if (!canViewLocation(loc, viewer)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catch not found");
		}
		return c;
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

	/**
	 * Whether the viewer may read this location (detail, catches list, comments, likes).
	 */
	private boolean canViewLocation(Location location, Account viewer) {
		Account owner = location.getAccount();
		if (owner == null) {
			return false;
		}
		if (owner.getId().equals(viewer.getId())) {
			return true;
		}
		PostVisibility vis = location.getVisibility();
		if (vis == null || vis == PostVisibility.PUBLIC) {
			return true;
		}
		if (vis == PostVisibility.PRIVATE) {
			return false;
		}
		// FRIENDS
		Long a = owner.getId();
		Long b = viewer.getId();
		long minId = Math.min(a, b);
		long maxId = Math.max(a, b);
		return friendshipRepository.findByAccountA_IdAndAccountB_Id(minId, maxId).isPresent();
	}

	private void assertLocationOwnerOrAdmin(Location location, Account account) {
		if (location.getAccount() != null && location.getAccount().getId().equals(account.getId())) {
			return;
		}
		if (account.getRole() == AccountRole.ADMIN) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this location");
	}

	private static boolean isBlobObjectKey(String key) {
		if (key == null) {
			return false;
		}
		String k = key.trim();
		return !k.startsWith("http://") && !k.startsWith("https://");
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
