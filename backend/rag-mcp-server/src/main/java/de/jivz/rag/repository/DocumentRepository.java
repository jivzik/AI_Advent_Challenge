package de.jivz.rag.repository;

import de.jivz.rag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByFileName(String fileName);

    List<Document> findByStatus(Document.DocumentStatus status);

    @Query("SELECT d FROM Document d WHERE d.status = 'READY' ORDER BY d.createdAt DESC")
    List<Document> findAllReady();

    boolean existsByFileName(String fileName);
}

