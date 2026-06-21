package ca.consmatt.repositories;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.consmatt.beans.ForumThread;
import ca.consmatt.beans.PostVisibility;
import ca.consmatt.dto.ForumThreadResponse;

/**
 * Persistence for {@link ForumThread} entities.
 */
public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {

	@Query("""
			select new ca.consmatt.dto.ForumThreadResponse(
				t.id,
				a.id,
				a.username,
				t.title,
				t.body,
				t.createdAt,
				t.visibility,
				(select count(tc) from ForumThreadComment tc where tc.thread.id = t.id),
				(select count(tl) from ForumThreadLike tl where tl.thread.id = t.id)
			)
			from ForumThread t
			join t.account a
			where (t.visibility is null or t.visibility = :publicVis)
				or a.id = :viewerId
				or (t.visibility = :friendsVis and (
					exists (select 1 from Friendship f where f.accountA.id = a.id and f.accountB.id = :viewerId)
					or exists (select 1 from Friendship f2 where f2.accountA.id = :viewerId and f2.accountB.id = a.id)
				))
			order by t.createdAt desc, t.id desc
			""")
	List<ForumThreadResponse> findFeedForViewer(
			@Param("viewerId") Long viewerId,
			@Param("publicVis") PostVisibility publicVis,
			@Param("friendsVis") PostVisibility friendsVis,
			Pageable pageable);

	java.util.List<ForumThread> findByAccount_Id(Long accountId);
}
