package com.ai.texttosql.repository;

import com.ai.texttosql.model.QueryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, UUID> {

    /**
     * Find all queries with pagination, ordered by timestamp (most recent first)
     */
    Page<QueryHistory> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find a query by its ID
     */
    @Override
    Optional<QueryHistory> findById(UUID queryId);

    /**
     * Find queries by user ID with pagination, ordered by timestamp (most recent first)
     */
    Page<QueryHistory> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    
    /**
     * Find recent queries with pagination, ordered by timestamp (most recent first)
     */
    @Query("SELECT q FROM QueryHistory q ORDER BY q.timestamp DESC")
    Page<QueryHistory> findRecentQueries(Pageable pageable);
}