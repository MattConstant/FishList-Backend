package ca.consmatt.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
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
import ca.consmatt.dto.CreateConditionRequest;
import ca.consmatt.repositories.ConditionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API for environmental / fishing condition snapshots (weather, water, moon, etc.).
 */
@RestController
@RequestMapping("/api/conditions")
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
	 * @param request condition fields (not a persistent entity)
	 * @return confirmation with new id
	 */
	@PostMapping({ "", "/" })
	public String createCondition(@Valid @RequestBody CreateConditionRequest request) {
		Condition saved = conditionRepo.save(request.toNewEntity());
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
	 * @param request replacement fields (not a persistent entity)
	 * @return updated entity
	 */
	@PutMapping("/{id}")
	public Condition updateCondition(@PathVariable Long id, @Valid @RequestBody CreateConditionRequest request) {
		return conditionRepo.findById(id).map(condition -> {
			request.applyTo(condition);
			return conditionRepo.save(condition);
		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condition not found with id " + id));
	}
}
