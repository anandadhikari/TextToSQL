package com.ai.texttosql.repository;

import com.ai.texttosql.model.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, UUID> {

    // Get all queries ordered by timestamp (most recent first)
    List<QueryHistory> findAllByOrderByTimestampDesc();

    // Find by query ID only
    Optional<QueryHistory> findById(UUID queryId);

    // Optional: Get recent queries (limit to 50 for performance)
    List<QueryHistory> findTop50ByOrderByTimestampDesc();

    // Find by User ID only
    Optional<QueryHistory> getQueryHistoryByUserId(String userId);
}