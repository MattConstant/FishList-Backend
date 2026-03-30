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

import ca.consmatt.beans.Condition;
import ca.consmatt.repositories.ConditionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API for environmental / fishing condition snapshots (weather, water, moon, etc.).
 */
@RestController
@RequestMapping("/api/conditions")
@CrossOrigin
@RequiredArgsConstructor
public class ConditionController {

	private final ConditionRepository conditionRepo;

	/** @return all condition records */
	@GetMapping({ "", "/" })
	public List<Condition> getConditionCollection() {
		return conditionRepo.findAll();
	}

	/**
	 * @param id condition primary key
	 * @return optional row or empty
	 */
	@GetMapping("/{id}")
	public Optional<Condition> getConditionById(@PathVariable Long id) {
		return conditionRepo.findById(id);
	}

	/**
	 * @param condition entity JSON
	 * @return confirmation with new id
	 */
	@PostMapping({ "", "/" })
	public String createCondition(@Valid @RequestBody Condition condition) {
		Condition saved = conditionRepo.save(condition);
		return "Record added at index " + saved.getId();
	}

	/** @param id condition primary key */
	@DeleteMapping("/{id}")
	public String deleteById(@PathVariable Long id) {
		conditionRepo.deleteById(id);
		return "deleted";
	}

	/**
	 * @param id condition primary key
	 * @param updated replacement fields
	 * @return updated entity
	 */
	@PutMapping("/{id}")
	public Condition updateCondition(@PathVariable Long id, @Valid @RequestBody Condition updated) {
		return conditionRepo.findById(id).map(condition -> {
			condition.setWeather(updated.getWeather());
			condition.setTemperature(updated.getTemperature());
			condition.setWindSpeed(updated.getWindSpeed());
			condition.setTimeOfDay(updated.getTimeOfDay());
			condition.setWaterTemperature(updated.getWaterTemperature());
			condition.setWaterClarity(updated.getWaterClarity());
			condition.setPressure(updated.getPressure());
			condition.setMoonPhase(updated.getMoonPhase());
			condition.setNotes(updated.getNotes());
			return conditionRepo.save(condition);
		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condition not found with id " + id));
	}
}
