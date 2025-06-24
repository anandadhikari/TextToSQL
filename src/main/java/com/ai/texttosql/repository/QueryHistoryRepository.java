package com.ai.texttosql.repository;

import com.ai.texttosql.model.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, UUID> {

    // Get all queries ordered by timestamp (most recent first)
    List<QueryHistory> findAllByOrderByTimestampDesc();

    // Find by ID only
    Optional<QueryHistory> findById(UUID id);

    // Optional: Get recent queries (limit to 50 for performance)
    List<QueryHistory> findTop50ByOrderByTimestampDesc();
}