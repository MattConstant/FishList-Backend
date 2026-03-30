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
 * Symmetric friendship edge between two distinct accounts.
 */
@Entity
@Table(name = "friendships", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "account_a_id", "account_b_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_a_id")
	private Account accountA;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_b_id")
	private Account accountB;

	private String createdAt;
}
