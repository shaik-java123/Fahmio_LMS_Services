package com.lms.repository;

import com.lms.model.AssignmentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentAllocationRepository extends JpaRepository<AssignmentAllocation, Long> {

    /**
     * Find all allocations for a specific assignment
     */
    List<AssignmentAllocation> findByAssignmentId(Long assignmentId);

    /**
     * Find all allocations for a specific candidate
     */
    List<AssignmentAllocation> findByCandidateId(Long candidateId);

    /**
     * Find allocation by assignment ID and candidate ID
     */
    Optional<AssignmentAllocation> findByAssignmentIdAndCandidateId(Long assignmentId, Long candidateId);

    /**
     * Find all allocations that haven't sent notifications yet
     */
    @Query("SELECT aa FROM AssignmentAllocation aa WHERE aa.notificationSent = false")
    List<AssignmentAllocation> findUnnotifiedAllocations();

    /**
     * Find allocations for a candidate by course
     */
    @Query("SELECT aa FROM AssignmentAllocation aa " +
           "WHERE aa.candidate.id = :candidateId " +
           "AND aa.assignment.course.id = :courseId")
    List<AssignmentAllocation> findByCandidateAndCourse(@Param("candidateId") Long candidateId,
                                                        @Param("courseId") Long courseId);

    /**
     * Find allocations by status
     */
    List<AssignmentAllocation> findByStatus(AssignmentAllocation.AllocationStatus status);

    /**
     * Count allocations for an assignment
     */
    Long countByAssignmentId(Long assignmentId);
}

