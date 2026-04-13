package com.ktlo.simulator.controller.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Main controller for the Document Processing Portal.
 * Handles navigation and renders Thymeleaf templates.
 */
@Slf4j
@Controller
@RequestMapping("/portal")
public class DocumentPortalController {

    /**
     * Home page of the document processing portal.
     * GET /portal
     */
    @GetMapping({"", "/"})
    public String home(Model model) {
        log.info("Accessing portal home page");

        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activePage", "home");

        return "home";
    }

    /**
     * Upload page for document processing.
     * GET /portal/upload
     */
    @GetMapping("/upload")
    public String upload(Model model) {
        log.info("Accessing document upload page");

        model.addAttribute("pageTitle", "Upload Document");
        model.addAttribute("activePage", "upload");

        return "upload";
    }

    /**
     * Dashboard page showing analytics and job history.
     * GET /portal/dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Accessing dashboard page");

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");

        return "dashboard";
    }
}
