package com.ktlo.simulator.controller.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for admin panel pages.
 * Provides monitoring and control interface for simulations.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminPanelController {

    /**
     * Admin panel home page.
     * GET /admin
     */
    @GetMapping({"", "/"})
    public String adminHome(Model model) {
        log.info("Accessing admin panel home");

        model.addAttribute("pageTitle", "Admin Panel");
        model.addAttribute("activePage", "admin");

        return "admin/panel";
    }

    /**
     * Active simulations monitoring page.
     * GET /admin/simulations
     */
    @GetMapping("/simulations")
    public String simulations(Model model) {
        log.info("Accessing simulations monitoring page");

        model.addAttribute("pageTitle", "Active Simulations");
        model.addAttribute("activePage", "simulations");

        return "admin/simulations";
    }

    /**
     * JMX metrics monitoring page.
     * GET /admin/metrics
     */
    @GetMapping("/metrics")
    public String metrics(Model model) {
        log.info("Accessing metrics monitoring page");

        model.addAttribute("pageTitle", "System Metrics");
        model.addAttribute("activePage", "metrics");

        return "admin/metrics";
    }
}
