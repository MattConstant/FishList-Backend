package ca.consmatt.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One achievement unlocked by one account. The {@code (account, code)} pair is unique so the
 * service can call save() unconditionally and rely on the DB to deduplicate races.
 */
@Entity
@Table(name = "account_achievements", uniqueConstraints = {
		@UniqueConstraint(name = "uk_account_achievement", columnNames = { "account_id", "code" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountAchievement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	@JsonIgnore
	private Account account;

	/**
	 * Persisted as plain {@code varchar} via {@code columnDefinition} so Hibernate doesn't generate
	 * a {@code CHECK (code in (...))} constraint that would block future enum additions.
	 * Application-level validation happens via the {@link AchievementCode} type itself.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 64, columnDefinition = "varchar(64) not null")
	private AchievementCode code;

	@Column(name = "unlocked_at", nullable = false, length = 64)
	private String unlockedAt;
}
