package ca.consmatt.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.MapFavoriteSpot;

public interface MapFavoriteSpotRepository extends JpaRepository<MapFavoriteSpot, Long> {

	List<MapFavoriteSpot> findByAccount_IdOrderByIdDesc(Long accountId);

	Optional<MapFavoriteSpot> findByAccount_IdAndSpotKey(Long accountId, String spotKey);

	Optional<MapFavoriteSpot> findByAccount_IdAndId(Long accountId, Long id);
}
