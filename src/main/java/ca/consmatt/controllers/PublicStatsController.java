package ca.consmatt.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.consmatt.dto.PublicStatsResponse;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.CatchRepository;
import ca.consmatt.repositories.LocationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Read-only platform totals for the logged-out home page (no auth).
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicStatsController {

	private final AccountRepository accountRepository;
	private final LocationRepository locationRepository;
	private final CatchRepository catchRepository;

	@GetMapping("/stats")
	public PublicStatsResponse stats() {
		return new PublicStatsResponse(
				catchRepository.count(),
				locationRepository.count(),
				catchRepository.countDistinctSpecies(),
				accountRepository.count());
	}
}
