package com.offerflow.controller;

import com.offerflow.service.InterviewSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/interviews")
public class InterviewSearchController {

    private final InterviewSearchService searchService;

    public InterviewSearchController(InterviewSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("searchQuery", q);
        boolean hasQuery = q != null && !q.trim().isEmpty();
        model.addAttribute("hasQuery", hasQuery);
        if (hasQuery) {
            model.addAttribute("hits", searchService.search(q));
            model.addAttribute("showingRecent", false);
        } else {
            model.addAttribute("hits", searchService.listRecent());
            model.addAttribute("showingRecent", true);
        }
        return "interviews/search";
    }
}
