package ca.consmatt.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A comment left by an account on a catch post.
 */
@Entity
@Table(name = "catch_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatchComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "catch_id")
	private Catch catchRecord;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(nullable = false, length = 64)
	private String createdAt;
}
