package ca.consmatt.beans;

import jakarta.persistence.Entity;
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
 * One "like" on a catch by an account. (account, catch) is unique.
 */
@Entity
@Table(name = "catch_likes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_catch_like_account_catch", columnNames = { "account_id", "catch_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatchLike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private Account account;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "catch_id")
	private Catch catchRecord;
}
