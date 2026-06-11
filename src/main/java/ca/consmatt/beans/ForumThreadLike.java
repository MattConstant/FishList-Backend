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
 * One like on a forum thread by an account. (account, thread) is unique.
 */
@Entity
@Table(name = "forum_thread_likes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_forum_thread_like_account_thread", columnNames = { "account_id", "thread_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumThreadLike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private Account account;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "thread_id")
	private ForumThread thread;
}
