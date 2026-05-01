package ca.consmatt.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.MapFavoriteSpot;
import ca.consmatt.dto.CreateMapFavoriteSpotRequest;
import ca.consmatt.dto.MapFavoriteSpotResponse;
import ca.consmatt.repositories.MapFavoriteSpotRepository;
import ca.consmatt.util.MapSpotKey;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapFavoriteSpotService {

	private final MapFavoriteSpotRepository repo;

	public java.util.List<MapFavoriteSpotResponse> listFor(Account account) {
		return repo.findByAccount_IdOrderByIdDesc(account.getId()).stream()
				.map(MapFavoriteSpotService::toResponse)
				.toList();
	}

	/**
	 * Creates a bookmark or returns an existing row for the same snapped coordinates (same
	 * {@link MapSpotKey#spotKey}); updates label when the duplicate had a stale label.
	 */
	@Transactional
	public MapFavoriteSpotResponse create(Account account, CreateMapFavoriteSpotRequest req) {
		double lat = req.latitude();
		double lng = req.longitude();
		String spotKey = MapSpotKey.spotKey(lat, lng);
		String label = sanitizedLabel(req.label());

		java.util.Optional<MapFavoriteSpot> existing = repo.findByAccount_IdAndSpotKey(account.getId(), spotKey);
		if (existing.isPresent()) {
			MapFavoriteSpot row = existing.get();
			row.setLatitude(MapSpotKey.snap(lat));
			row.setLongitude(MapSpotKey.snap(lng));
			if (!label.equals(row.getLabel())) {
				row.setLabel(label);
				repo.save(row);
			}
			return toResponse(row);
		}

		double slat = MapSpotKey.snap(lat);
		double slng = MapSpotKey.snap(lng);
		MapFavoriteSpot saved = repo.save(new MapFavoriteSpot(null, account, spotKey, slat, slng, label,
				Instant.now().toString()));
		return toResponse(saved);
	}

	@Transactional
	public void deleteOwned(Account owner, Long id) {
		MapFavoriteSpot row = repo.findByAccount_IdAndId(owner.getId(), id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		repo.delete(row);
	}

	private static final String DEFAULT_LABEL = "Saved spot";

	private static String sanitizedLabel(String raw) {
		if (raw == null) {
			return DEFAULT_LABEL;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return DEFAULT_LABEL;
		}
		return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
	}

	private static MapFavoriteSpotResponse toResponse(MapFavoriteSpot row) {
		long ms = 0;
		try {
			ms = Instant.parse(row.getCreatedAt()).toEpochMilli();
		} catch (RuntimeException ignored) {
			/* fall back — invalid legacy row */
		}
		return new MapFavoriteSpotResponse(
				row.getId(),
				row.getLatitude(),
				row.getLongitude(),
				row.getSpotKey(),
				row.getLabel(),
				ms);
	}
}
