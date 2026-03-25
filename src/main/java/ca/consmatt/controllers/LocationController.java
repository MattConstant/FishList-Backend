package ca.consmatt.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.Catch;
import ca.consmatt.beans.Location;
import ca.consmatt.dto.AddCatchRequest;
import ca.consmatt.dto.LocationDetailResponse;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.LocationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API for fishing locations and nested catch records. Create, update, and delete require
 * ownership; reads are available to any authenticated user unless noted.
 */
@RestController
@RequestMapping("/api/locations")
@CrossOrigin
@RequiredArgsConstructor
public class LocationController {

	private final LocationRepository locationRepo;
	private final AccountRepository accountRepository;
	private final CatchRepository catchRepo;

	/**
	 * Simple health check (unauthenticated). Available at {@code /heartbeat} and {@code /healthcheck}.
	 *
	 * @return plain-text greeting
	 */
	@GetMapping({ "/heartbeat", "/healthcheck" })
	public String root() {
		return "Hello, this is a plain string!";
	}

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
	public ResponseEntity<String> createLocation(@RequestBody Location location, Authentication authentication) {
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
		Catch catchEntity = Catch.builder()
				.location(location)
				.species(request.species().trim())
				.quantity(quantity)
				.lengthCm(request.lengthCm())
				.weightKg(request.weightKg())
				.notes(request.notes())
				.imageUrl(request.imageUrl())
				.description(request.description())
				.build();
		return ResponseEntity.status(HttpStatus.CREATED).body(catchRepo.save(catchEntity));
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
	public Location updateLocation(@PathVariable Long id, @RequestBody Location updatedLocation,
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
}
