package ca.consmatt.controllers;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.consmatt.beans.Account;
import ca.consmatt.beans.AccountRole;
import ca.consmatt.beans.ForumThread;
import ca.consmatt.beans.ForumThreadComment;
import ca.consmatt.beans.ForumThreadLike;
import ca.consmatt.beans.PostVisibility;
import ca.consmatt.dto.CreateForumThreadCommentRequest;
import ca.consmatt.dto.CreateForumThreadRequest;
import ca.consmatt.dto.ForumThreadCommentResponse;
import ca.consmatt.dto.ForumThreadCommentsPageResponse;
import ca.consmatt.dto.ForumThreadLikeResponse;
import ca.consmatt.dto.ForumThreadResponse;
import ca.consmatt.dto.UpdateForumThreadRequest;
import ca.consmatt.dto.UnlockedAchievementSummary;
import ca.consmatt.repositories.AccountRepository;
import ca.consmatt.repositories.ForumThreadCommentRepository;
import ca.consmatt.repositories.ForumThreadLikeRepository;
import ca.consmatt.repositories.ForumThreadRepository;
import ca.consmatt.repositories.FriendshipRepository;
import ca.consmatt.service.AchievementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * REST API for forum-style discussion threads (likes and one-level comment replies).
 */
@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
@Validated
public class ForumThreadController {

	private static final Logger log = LoggerFactory.getLogger(ForumThreadController.class);

	private final ForumThreadRepository forumThreadRepository;
	private final ForumThreadCommentRepository forumThreadCommentRepository;
	private final ForumThreadLikeRepository forumThreadLikeRepository;
	private final AccountRepository accountRepository;
	private final FriendshipRepository friendshipRepository;
	private final AchievementService achievementService;

	@GetMapping("/feed")
	@Transactional(readOnly = true)
	public List<ForumThreadResponse> getFeed(
			@RequestParam(name = "offset", defaultValue = "0") @Min(value = 0, message = "offset must be >= 0") int offset,
			@RequestParam(name = "limit", defaultValue = "24") @Min(value = 1, message = "limit must be >= 1") @Max(value = 100, message = "limit must be <= 100") int limit,
			Authentication authentication) {
		int normalizedOffset = Math.max(offset, 0);
		int normalizedLimit = Math.min(Math.max(limit, 1), 100);
		int page = normalizedOffset / normalizedLimit;
		int pageOffset = page * normalizedLimit;

		Account viewer = requireAccount(authentication);
		List<ForumThreadResponse> batch = forumThreadRepository.findFeedForViewer(
				viewer.getId(),
				PostVisibility.PUBLIC,
				PostVisibility.FRIENDS,
				PageRequest.of(page, normalizedLimit));
		if (normalizedOffset == pageOffset) {
			return batch;
		}
		int skip = normalizedOffset - pageOffset;
		if (skip >= batch.size()) {
			return List.of();
		}
		return batch.subList(skip, batch.size());
	}

	@PostMapping({ "", "/" })
	public ResponseEntity<ForumThreadResponse> create(@Valid @RequestBody CreateForumThreadRequest request,
			Authentication authentication) {
		Account owner = requireAccount(authentication);
		PostVisibility visibility = request.visibility() != null ? request.visibility() : PostVisibility.PUBLIC;
		ForumThread saved = forumThreadRepository.save(new ForumThread(
				null,
				owner,
				request.title().trim(),
				request.body().trim(),
				Instant.now().toString(),
				visibility));
		log.info("FORUM_THREAD_CREATED user={} threadId={}", owner.getUsername(), saved.getId());
		achievementService.evaluateAll(owner);
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
	}

	@PutMapping("/{id:\\d+}")
	@Transactional
	public ForumThreadResponse update(@PathVariable Long id,
			@Valid @RequestBody UpdateForumThreadRequest request, Authentication authentication) {
		Account actor = requireAccount(authentication);
		ForumThread thread = forumThreadRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
		if (thread.getAccount() == null || !thread.getAccount().getId().equals(actor.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this thread");
		}
		thread.setTitle(request.title().trim());
		thread.setBody(request.body().trim());
		if (request.visibility() != null) {
			thread.setVisibility(request.visibility());
		}
		ForumThread saved = forumThreadRepository.save(thread);
		log.info("FORUM_THREAD_UPDATED user={} threadId={}", actor.getUsername(), saved.getId());
		return toResponse(saved);
	}

	@DeleteMapping("/{id:\\d+}")
	@Transactional
	public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
		Account actor = requireAccount(authentication);
		ForumThread thread = forumThreadRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
		boolean isOwner = thread.getAccount() != null && actor.getId().equals(thread.getAccount().getId());
		if (!isOwner && actor.getRole() != AccountRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this thread");
		}
		forumThreadLikeRepository.deleteByThread_Id(id);
		forumThreadCommentRepository.deleteByThread_Id(id);
		forumThreadRepository.delete(thread);
		log.info("FORUM_THREAD_DELETED user={} threadId={}", actor.getUsername(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id:\\d+}/comments")
	@Transactional(readOnly = true)
	public ForumThreadCommentsPageResponse getComments(@PathVariable Long id,
			@RequestParam(name = "offset", defaultValue = "0") @Min(value = 0, message = "offset must be >= 0") int offset,
			@RequestParam(name = "limit", defaultValue = "3") @Min(value = 1, message = "limit must be >= 1") @Max(value = 20, message = "limit must be <= 20") int limit,
			@RequestParam(name = "order", defaultValue = "asc") String orderRaw,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		ForumThread thread = requireVisibleThread(id, account);
		int normalizedOffset = Math.max(offset, 0);
		int normalizedLimit = Math.min(Math.max(limit, 1), 20);
		int requestWindow = normalizedOffset + normalizedLimit;
		boolean desc = orderRaw != null && "desc".equalsIgnoreCase(orderRaw.trim());

		Page<ForumThreadComment> pageResult = desc
				? forumThreadCommentRepository.findByThread_IdOrderByIdDesc(thread.getId(),
						PageRequest.of(0, requestWindow))
				: forumThreadCommentRepository.findByThread_IdOrderByIdAsc(thread.getId(),
						PageRequest.of(0, requestWindow));

		List<ForumThreadCommentResponse> comments = pageResult.getContent()
				.stream()
				.skip(normalizedOffset)
				.limit(normalizedLimit)
				.map(comment -> toCommentResponse(comment, account))
				.toList();

		return new ForumThreadCommentsPageResponse(
				comments,
				pageResult.getTotalElements(),
				normalizedOffset,
				normalizedLimit);
	}

	@PostMapping("/{id:\\d+}/comments")
	public ResponseEntity<ForumThreadCommentResponse> addComment(@PathVariable Long id,
			@Valid @RequestBody CreateForumThreadCommentRequest request, Authentication authentication) {
		Account account = requireAccount(authentication);
		ForumThread thread = requireVisibleThread(id, account);
		ForumThreadComment parentComment = null;
		if (request.parentCommentId() != null) {
			parentComment = forumThreadCommentRepository
					.findByIdAndThread_Id(request.parentCommentId(), thread.getId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment not found"));
			if (parentComment.getParent() != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reply to a reply");
			}
		}
		ForumThreadComment saved = forumThreadCommentRepository.save(new ForumThreadComment(
				null,
				thread,
				account,
				request.message().trim(),
				Instant.now().toString(),
				parentComment));
		log.info("FORUM_COMMENT_ADDED user={} thread={} commentId={}", account.getUsername(), thread.getId(),
				saved.getId());
		List<UnlockedAchievementSummary> unlocked = achievementService.evaluateAll(account);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toCommentResponseWithUnlocks(saved, account, unlocked));
	}

	@DeleteMapping("/{id:\\d+}/comments/{commentId:\\d+}")
	@Transactional
	public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId,
			Authentication authentication) {
		Account account = requireAccount(authentication);
		ForumThread thread = requireVisibleThread(id, account);
		ForumThreadComment comment = forumThreadCommentRepository.findByIdAndThread_Id(commentId, thread.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

		boolean isCommentOwner = comment.getAccount() != null && account.getId().equals(comment.getAccount().getId());
		boolean isThreadOwner = thread.getAccount() != null && account.getId().equals(thread.getAccount().getId());
		if (!isCommentOwner && !isThreadOwner && account.getRole() != AccountRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this comment");
		}

		deleteChildCommentsRecursively(commentId);
		forumThreadCommentRepository.delete(comment);
		log.info("FORUM_COMMENT_DELETED user={} thread={} commentId={}", account.getUsername(), thread.getId(),
				commentId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id:\\d+}/like")
	@Transactional(readOnly = true)
	public ForumThreadLikeResponse getLike(@PathVariable Long id, Authentication authentication) {
		Account account = requireAccount(authentication);
		ForumThread thread = requireVisibleThread(id, account);
		long likesCount = forumThreadLikeRepository.countByThread_Id(thread.getId());
		boolean likedByMe = forumThreadLikeRepository.existsByThread_IdAndAccount_Id(thread.getId(), account.getId());
		return new ForumThreadLikeResponse(thread.getId(), likesCount, likedByMe);
	}

	@PostMapping("/{id:\\d+}/like")
	public ForumThreadLikeResponse like(@PathVariable Long id, Authentication authentication) {
		Account account = requireAccount(authentication);
		ForumThread thread = requireVisibleThread(id, account);

		List<UnlockedAchievementSummary> unlockedForLiker = List.of();
		if (!forumThreadLikeRepository.existsByThread_IdAndAccount_Id(thread.getId(), account.getId())) {
			forumThreadLikeRepository.save(new ForumThreadLike(null, account, thread));
			log.info("FORUM_LIKE user={} thread={}", account.getUsername(), thread.getId());
			unlockedForLiker = achievementService.evaluateAll(account);
		}

		long likesCount = forumThreadLikeRepository.countByThread_Id(thread.getId());
		return new ForumThreadLikeResponse(thread.getId(), likesCount, true, unlockedForLiker);
	}

	@DeleteMapping("/{id:\\d+}/like")
	public ForumThreadLikeResponse unlike(@PathVariable Long id, Authentication authentication) {
		Account account = requireAccount(authentication);
		ForumThread thread = requireVisibleThread(id, account);

		forumThreadLikeRepository.findByThread_IdAndAccount_Id(thread.getId(), account.getId())
				.ifPresent(like -> {
					forumThreadLikeRepository.delete(like);
					log.info("FORUM_UNLIKE user={} thread={}", account.getUsername(), thread.getId());
				});

		long likesCount = forumThreadLikeRepository.countByThread_Id(thread.getId());
		return new ForumThreadLikeResponse(thread.getId(), likesCount, false);
	}

	private Account requireAccount(Authentication authentication) {
		return accountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	private ForumThread requireVisibleThread(Long id, Account viewer) {
		ForumThread thread = forumThreadRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
		if (!canViewThread(thread, viewer)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
		}
		return thread;
	}

	private boolean canViewThread(ForumThread thread, Account viewer) {
		Account owner = thread.getAccount();
		if (owner == null) {
			return false;
		}
		if (owner.getId().equals(viewer.getId())) {
			return true;
		}
		PostVisibility vis = thread.getVisibility();
		if (vis == null || vis == PostVisibility.PUBLIC) {
			return true;
		}
		if (vis == PostVisibility.PRIVATE) {
			return false;
		}
		Long a = owner.getId();
		Long b = viewer.getId();
		long minId = Math.min(a, b);
		long maxId = Math.max(a, b);
		return friendshipRepository.findByAccountA_IdAndAccountB_Id(minId, maxId).isPresent();
	}

	private ForumThreadResponse toResponse(ForumThread thread) {
		Long accountId = thread.getAccount() != null ? thread.getAccount().getId() : null;
		String username = thread.getAccount() != null ? thread.getAccount().getUsername() : "unknown";
		long commentsCount = forumThreadCommentRepository.countByThread_Id(thread.getId());
		long likesCount = forumThreadLikeRepository.countByThread_Id(thread.getId());
		return new ForumThreadResponse(
				thread.getId(),
				accountId,
				username,
				thread.getTitle(),
				thread.getBody(),
				thread.getCreatedAt(),
				thread.getVisibility(),
				commentsCount,
				likesCount);
	}

	private ForumThreadCommentResponse toCommentResponse(ForumThreadComment comment, Account currentAccount) {
		return toCommentResponseWithUnlocks(comment, currentAccount, List.of());
	}

	private ForumThreadCommentResponse toCommentResponseWithUnlocks(ForumThreadComment comment,
			Account currentAccount, List<UnlockedAchievementSummary> unlockedAchievements) {
		Long accountId = comment.getAccount() != null ? comment.getAccount().getId() : null;
		String username = comment.getAccount() != null ? comment.getAccount().getUsername() : "unknown";
		boolean ownedByMe = accountId != null
				&& currentAccount != null
				&& accountId.equals(currentAccount.getId());
		Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;
		String inReplyToUsername = null;
		if (comment.getParent() != null && comment.getParent().getAccount() != null) {
			inReplyToUsername = comment.getParent().getAccount().getUsername();
		}
		return new ForumThreadCommentResponse(
				comment.getId(),
				comment.getThread() != null ? comment.getThread().getId() : null,
				accountId,
				username,
				comment.getMessage(),
				comment.getCreatedAt(),
				ownedByMe,
				unlockedAchievements == null ? List.of() : unlockedAchievements,
				parentId,
				inReplyToUsername);
	}

	private void deleteChildCommentsRecursively(Long parentCommentId) {
		List<ForumThreadComment> replies = forumThreadCommentRepository.findByParent_Id(parentCommentId);
		for (ForumThreadComment reply : replies) {
			deleteChildCommentsRecursively(reply.getId());
			forumThreadCommentRepository.delete(reply);
		}
	}
}
