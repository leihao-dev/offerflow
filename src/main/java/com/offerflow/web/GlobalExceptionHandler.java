package com.offerflow.web;

import com.offerflow.service.ApplicationNotFoundException;
import com.offerflow.service.InterviewNoteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleApplicationNotFound(ApplicationNotFoundException ex, Model model) {
        model.addAttribute("message", "未找到该投递记录，可能已被删除。");
        return "error/404";
    }

    @ExceptionHandler(InterviewNoteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleInterviewNoteNotFound(InterviewNoteNotFoundException ex, Model model) {
        model.addAttribute("message", "未找到该面试复盘，可能已被删除。");
        return "error/404";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex, Model model) {
        model.addAttribute("message", "链接无效，请从列表或仪表盘重新进入。");
        return "error/404";
    }
}
