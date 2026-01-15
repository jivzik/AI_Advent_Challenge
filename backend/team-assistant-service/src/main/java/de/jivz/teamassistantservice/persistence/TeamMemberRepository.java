package de.jivz.teamassistantservice.persistence;

import de.jivz.teamassistantservice.persistence.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    Optional<TeamMember> findByEmail(String email);

    boolean existsByEmail(String email);
}
