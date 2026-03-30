package ca.consmatt.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
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

import ca.consmatt.beans.Fish;
import ca.consmatt.repositories.FishRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API for the fish species reference catalog (not individual catches).
 */
@RestController
@RequestMapping("/api/fish")
@CrossOrigin
@RequiredArgsConstructor
public class FishController {

	private final FishRepository fishRepo;

	/** @return all species rows */
	@GetMapping({ "", "/" })
	public List<Fish> getFishCollection() {
		return fishRepo.findAll();
	}

	/**
	 * @param id species primary key
	 * @return optional containing the fish or empty
	 */
	@GetMapping("/{id}")
	public Optional<Fish> getFishById(@PathVariable Long id) {
		return fishRepo.findById(id);
	}

	/**
	 * @param fish entity JSON
	 * @return confirmation message with new id
	 */
	@PostMapping({ "", "/" })
	public String createFish(@Valid @RequestBody Fish fish) {
		Fish saved = fishRepo.save(fish);
		return "Record added at index " + saved.getId();
	}

	/** @param id species primary key */
	@DeleteMapping("/{id}")
	public String deleteById(@PathVariable Long id) {
		fishRepo.deleteById(id);
		return "deleted";
	}

	/**
	 * @param id species primary key
	 * @param updated replacement fields
	 * @return updated entity
	 */
	@PutMapping("/{id}")
	public Fish updateFish(@PathVariable Long id, @Valid @RequestBody Fish updated) {
		return fishRepo.findById(id).map(fish -> {
			fish.setSpecies(updated.getSpecies());
			fish.setAverageWeight(updated.getAverageWeight());
			fish.setAverageLength(updated.getAverageLength());
			fish.setMaxWeight(updated.getMaxWeight());
			fish.setMaxLength(updated.getMaxLength());
			fish.setImageUrl(updated.getImageUrl());
			fish.setDescription(updated.getDescription());
			return fishRepo.save(fish);
		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fish not found with id " + id));
	}
}
