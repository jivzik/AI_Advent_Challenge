package de.jivz.teamassistantservice.persistence;

import de.jivz.teamassistantservice.persistence.entity.QueryLog;
import de.jivz.teamassistantservice.persistence.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QueryLogRepository extends JpaRepository<QueryLog, UUID> {

    List<QueryLog> findByTeamMemberOrderByCreatedAtDesc(TeamMember teamMember);

    List<QueryLog> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<QueryLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

    long countByTeamMember(TeamMember teamMember);
}
