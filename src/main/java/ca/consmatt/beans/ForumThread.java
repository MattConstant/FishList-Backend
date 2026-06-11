package ca.consmatt.beans;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A discussion thread in the community feed (forum-style post, separate from geo-tied catches).
 */
@Entity
@Table(name = "forum_threads")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumThread {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 5000)
	private String body;

	@Column(nullable = false, length = 64)
	private String createdAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", length = 16, columnDefinition = "varchar(16)")
	private PostVisibility visibility;
}
