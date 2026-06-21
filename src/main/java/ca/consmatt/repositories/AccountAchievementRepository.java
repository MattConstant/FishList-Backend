package ca.consmatt.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.consmatt.beans.AccountAchievement;

/**
 * Persistence for unlocked achievements per account.
 */
public interface AccountAchievementRepository extends JpaRepository<AccountAchievement, Long> {

	List<AccountAchievement> findByAccount_Id(Long accountId);

	void deleteByAccount_Id(Long accountId);
}
