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
 * A comment on a forum thread. Supports one-level replies (reply to top-level comment only).
 */
@Entity
@Table(name = "forum_thread_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumThreadComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "thread_id")
	private ForumThread thread;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(nullable = false, length = 64)
	private String createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	private ForumThreadComment parent;
}
