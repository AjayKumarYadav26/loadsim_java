package com.ktlo.simulator.controller.web;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.service.web.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for document processing pages.
 */
@Slf4j
@Controller
@RequestMapping("/portal/process")
@RequiredArgsConstructor
public class DocumentProcessingController {

    private final DocumentProcessingService processingService;

    /**
     * Show processing status page for a job.
     *
     * @param jobId the job ID
     * @param model the model
     * @return processing template
     */
    @GetMapping("/{jobId}")
    public String showProcessingPage(@PathVariable String jobId, Model model) {
        log.info("Loading processing page for job: {}", jobId);

        ProcessingJob job = processingService.getJobStatus(jobId);
        if (job == null) {
            log.warn("Job not found: {}", jobId);
            model.addAttribute("error", "Job not found");
            return "error/404";
        }

        model.addAttribute("job", job);
        model.addAttribute("pageTitle", "Processing");
        return "processing";
    }
}
