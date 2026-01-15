package de.jivz.supportservice.persistence;

import de.jivz.supportservice.persistence.entity.SupportUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportUserRepository extends JpaRepository<SupportUser, UUID> {

    Optional<SupportUser> findByEmail(String email);

    Optional<SupportUser> findByCompanyInn(String companyInn);

    boolean existsByEmail(String email);

    boolean existsByCompanyInn(String companyInn);
}