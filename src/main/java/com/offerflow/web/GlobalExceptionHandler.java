package com.offerflow.web;

import com.offerflow.service.ApplicationNotFoundException;
import com.offerflow.service.CompanyNotFoundException;
import com.offerflow.service.InterviewNoteNotFoundException;
import org.hibernate.LazyInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleApplicationNotFound(ApplicationNotFoundException ex, Model model) {
        model.addAttribute("message", "未找到该投递记录，可能已被删除。");
        return "error/404";
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCompanyNotFound(CompanyNotFoundException ex, Model model) {
        model.addAttribute("message", "未找到该公司档案，可能已被删除。");
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

    @ExceptionHandler(LazyInitializationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleLazyInitialization(LazyInitializationException ex, Model model) {
        log.warn("LazyInitializationException while rendering page", ex);
        model.addAttribute("message", "页面加载数据时出错，请刷新后重试。");
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex, Model model) {
        log.warn("Unhandled exception while processing request", ex);
        model.addAttribute("message", "服务器处理请求时发生错误，请稍后重试。");
        return "error/500";
    }
}
