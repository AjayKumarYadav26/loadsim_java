package com.ktlo.simulator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for LoadSimulatorEntity.
 * Provides database operations and custom queries for failure simulation.
 */
@Repository
public interface LoadSimulatorRepository extends JpaRepository<LoadSimulatorEntity, Long> {

    List<LoadSimulatorEntity> findByStatus(String status);

    /**
     * Slow query using PostgreSQL pg_sleep function.
     * This simulates a query that takes a long time to execute.
     *
     * @param delaySeconds Number of seconds to delay
     * @return All records (after delay)
     */
    @Query(value = "SELECT pg_sleep(:delaySeconds), ls.* FROM loadsimulator ls", nativeQuery = true)
    List<LoadSimulatorEntity> findAllWithDelay(@Param("delaySeconds") int delaySeconds);

    /**
     * Count records by status.
     *
     * @param status Status value
     * @return Count of records
     */
    long countByStatus(String status);
}
