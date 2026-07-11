package com.offerflow.web;

import com.offerflow.service.ApplicationNotFoundException;
import com.offerflow.service.InterviewNoteNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public String handleApplicationNotFound(ApplicationNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(InterviewNoteNotFoundException.class)
    public String handleInterviewNoteNotFound(InterviewNoteNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }
}
