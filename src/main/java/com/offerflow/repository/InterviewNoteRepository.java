package com.offerflow.repository;

import com.offerflow.model.InterviewNote;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewNoteRepository extends JpaRepository<InterviewNote, Long> {

    @Query("SELECT n FROM InterviewNote n JOIN FETCH n.application WHERE n.id = :id")
    Optional<InterviewNote> findByIdWithApplication(@Param("id") Long id);

    List<InterviewNote> findByInterviewDateBetween(LocalDate start, LocalDate end);

    List<InterviewNote> findByApplicationIdOrderByInterviewDateDesc(Long applicationId);

    @Query("""
            SELECT n FROM InterviewNote n JOIN FETCH n.application a
            ORDER BY n.interviewDate DESC, n.createdAt DESC""")
    List<InterviewNote> findAllWithApplicationOrderByInterviewDateDesc();

    @Query("""
            SELECT n FROM InterviewNote n JOIN FETCH n.application a
            ORDER BY n.interviewDate DESC, n.createdAt DESC""")
    List<InterviewNote> findRecentWithApplication(Pageable pageable);
}
