package com.offerflow.repository;

import com.offerflow.model.InterviewNote;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewNoteRepository extends JpaRepository<InterviewNote, Long> {

    List<InterviewNote> findByInterviewDateBetween(LocalDate start, LocalDate end);

    List<InterviewNote> findByApplicationIdOrderByInterviewDateDesc(Long applicationId);
}
