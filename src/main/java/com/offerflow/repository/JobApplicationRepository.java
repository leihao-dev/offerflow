package com.offerflow.repository;

import com.offerflow.model.ApplicationStage;
import com.offerflow.model.JobApplication;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    @Query("SELECT a FROM JobApplication a LEFT JOIN FETCH a.interviewNotes LEFT JOIN FETCH a.company WHERE a.id = :id")
    Optional<JobApplication> findByIdWithNotes(@Param("id") Long id);

    List<JobApplication> findByStageOrderByUpdatedAtDesc(ApplicationStage stage);

    List<JobApplication> findAllByOrderByUpdatedAtDesc();

    long countByStageNotIn(Collection<ApplicationStage> terminalStages);

    List<JobApplication> findByNextFollowUpAtNotNullAndNextFollowUpAtLessThanEqualAndStageNotIn(
            LocalDate date, Collection<ApplicationStage> terminalStages);

    List<JobApplication> findTop5ByOrderByUpdatedAtDesc();

    long countByCompanyId(Long companyId);

    List<JobApplication> findByCompanyIdOrderByUpdatedAtDesc(Long companyId);

    List<JobApplication> findByCompanyNameContainingIgnoreCaseOrPositionTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            String companyName, String positionTitle);

    @Query("""
            SELECT a FROM JobApplication a
            WHERE a.stage = :stage
              AND (LOWER(a.companyName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.positionTitle) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY a.updatedAt DESC""")
    List<JobApplication> searchByStageAndQuery(
            @Param("stage") ApplicationStage stage, @Param("q") String q);
}
