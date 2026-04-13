/**
 * Dashboard JavaScript
 * Handles analytics data fetching and Chart.js visualizations
 */

(function() {
    'use strict';

    // Configuration
    const REFRESH_INTERVAL = 30000; // Refresh every 30 seconds

    // Chart instances
    let trendsChart = null;
    let operationsChart = null;
    let statusChart = null;
    let qualityChart = null;

    // Initialize
    function init() {
        console.log('Initializing dashboard');

        // Load all data
        loadDashboardData();

        // Setup refresh button
        $('#refreshBtn').on('click', function() {
            $(this).find('i').addClass('fa-spin');
            loadDashboardData();
            setTimeout(() => {
                $('#refreshBtn').find('i').removeClass('fa-spin');
            }, 1000);
        });

        // Auto-refresh every 30 seconds
        setInterval(loadDashboardData, REFRESH_INTERVAL);
    }

    // Load all dashboard data
    function loadDashboardData() {
        loadSummaryStats();
        loadJobTrends();
        loadOperationBreakdown();
        loadStatusBreakdown();
        loadQualityBreakdown();
        loadRecentJobs();
    }

    // Load summary statistics
    function loadSummaryStats() {
        $.ajax({
            url: '/api/portal/analytics/summary',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    updateSummaryCards(response);
                } else {
                    console.error('Error loading summary:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading summary stats:', xhr);
            }
        });
    }

    // Update summary cards
    function updateSummaryCards(data) {
        $('#totalJobs').text(data.totalJobs || 0);
        $('#completedJobs').text(data.completedJobs || 0);
        $('#successRate').text((data.successRate || 0).toFixed(1) + '%');
        $('#avgTime').text((data.avgProcessingTimeSeconds || 0) + 's');
    }

    // Load job trends
    function loadJobTrends() {
        $.ajax({
            url: '/api/portal/analytics/trends',
            method: 'GET',
            data: { days: 7 },
            success: function(response) {
                if (response.success) {
                    renderTrendsChart(response.trends);
                } else {
                    console.error('Error loading trends:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading job trends:', xhr);
            }
        });
    }

    // Render trends chart (line chart)
    function renderTrendsChart(trends) {
        const ctx = document.getElementById('trendsChart');
        if (!ctx) return;

        // Prepare data
        const labels = trends.map(t => t.date);
        const data = trends.map(t => t.count);

        // Destroy existing chart
        if (trendsChart) {
            trendsChart.destroy();
        }

        // Create new chart
        trendsChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Jobs Per Day',
                    data: data,
                    borderColor: 'rgb(13, 110, 253)',
                    backgroundColor: 'rgba(13, 110, 253, 0.1)',
                    tension: 0.1,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
    }

    // Load operation breakdown
    function loadOperationBreakdown() {
        $.ajax({
            url: '/api/portal/analytics/operations',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    renderOperationsChart(response.operations);
                } else {
                    console.error('Error loading operations:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading operation breakdown:', xhr);
            }
        });
    }

    // Render operations chart (pie chart)
    function renderOperationsChart(operations) {
        const ctx = document.getElementById('operationsChart');
        if (!ctx) return;

        // Prepare data
        const labels = Object.keys(operations);
        const data = Object.values(operations);
        const colors = [
            'rgba(13, 110, 253, 0.7)',   // Blue - ANALYZE
            'rgba(25, 135, 84, 0.7)',    // Green - CONVERT
            'rgba(255, 193, 7, 0.7)',    // Yellow - OPTIMIZE
            'rgba(220, 53, 69, 0.7)'     // Red - REPORT
        ];

        // Destroy existing chart
        if (operationsChart) {
            operationsChart.destroy();
        }

        // Create new chart
        operationsChart = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        position: 'bottom'
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((value / total) * 100).toFixed(1);
                                return label + ': ' + value + ' (' + percentage + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    // Load status breakdown
    function loadStatusBreakdown() {
        $.ajax({
            url: '/api/portal/analytics/status',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    renderStatusChart(response.status);
                } else {
                    console.error('Error loading status:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading status breakdown:', xhr);
            }
        });
    }

    // Render status chart (doughnut chart)
    function renderStatusChart(statusData) {
        const ctx = document.getElementById('statusChart');
        if (!ctx) return;

        // Prepare data
        const labels = Object.keys(statusData);
        const data = Object.values(statusData);
        const colors = {
            'COMPLETED': 'rgba(25, 135, 84, 0.7)',
            'FAILED': 'rgba(220, 53, 69, 0.7)',
            'PROCESSING': 'rgba(13, 110, 253, 0.7)',
            'QUEUED': 'rgba(255, 193, 7, 0.7)',
            'CANCELLED': 'rgba(108, 117, 125, 0.7)'
        };
        const backgroundColors = labels.map(l => colors[l] || 'rgba(128, 128, 128, 0.7)');

        // Destroy existing chart
        if (statusChart) {
            statusChart.destroy();
        }

        // Create new chart
        statusChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: backgroundColors,
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    }

    // Load quality breakdown
    function loadQualityBreakdown() {
        $.ajax({
            url: '/api/portal/analytics/quality',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    renderQualityChart(response.quality);
                } else {
                    console.error('Error loading quality:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading quality breakdown:', xhr);
            }
        });
    }

    // Render quality chart (bar chart)
    function renderQualityChart(qualityData) {
        const ctx = document.getElementById('qualityChart');
        if (!ctx) return;

        // Prepare data
        const labels = Object.keys(qualityData);
        const data = Object.values(qualityData);
        const colors = {
            'HIGH': 'rgba(220, 53, 69, 0.7)',
            'MEDIUM': 'rgba(255, 193, 7, 0.7)',
            'LOW': 'rgba(25, 135, 84, 0.7)'
        };
        const backgroundColors = labels.map(l => colors[l] || 'rgba(128, 128, 128, 0.7)');

        // Destroy existing chart
        if (qualityChart) {
            qualityChart.destroy();
        }

        // Create new chart
        qualityChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Jobs Count',
                    data: data,
                    backgroundColor: backgroundColors,
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
    }

    // Load recent jobs
    function loadRecentJobs() {
        $.ajax({
            url: '/api/portal/analytics/recent',
            method: 'GET',
            data: { limit: 10 },
            success: function(response) {
                if (response.success) {
                    renderRecentJobsTable(response.jobs);
                } else {
                    console.error('Error loading recent jobs:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading recent jobs:', xhr);
            }
        });
    }

    // Render recent jobs table
    function renderRecentJobsTable(jobs) {
        const tbody = $('#recentJobsTable');
        tbody.empty();

        if (jobs.length === 0) {
            tbody.append('<tr><td colspan="8" class="text-center text-muted">No jobs found</td></tr>');
            return;
        }

        jobs.forEach(function(job) {
            const statusBadge = getStatusBadge(job.status);
            const progressBar = getProgressBar(job.progress, job.status);
            const createdAt = new Date(job.createdAt).toLocaleString();

            const row = $('<tr>');
            row.append('<td><small><code>' + job.jobId.substring(0, 8) + '...</code></small></td>');
            row.append('<td>' + job.fileName + '</td>');
            row.append('<td><span class="badge bg-secondary">' + job.operationType + '</span></td>');
            row.append('<td><span class="badge bg-info">' + job.quality + '</span></td>');
            row.append('<td>' + statusBadge + '</td>');
            row.append('<td>' + progressBar + '</td>');
            row.append('<td><small>' + createdAt + '</small></td>');

            const actionsCell = $('<td>');
            if (job.status === 'PROCESSING' || job.status === 'QUEUED') {
                actionsCell.append('<a href="/portal/process/' + job.jobId + '" class="btn btn-sm btn-primary"><i class="fas fa-eye"></i></a>');
            } else {
                actionsCell.append('<button class="btn btn-sm btn-outline-secondary" disabled><i class="fas fa-info-circle"></i></button>');
            }
            row.append(actionsCell);

            tbody.append(row);
        });
    }

    // Get status badge HTML
    function getStatusBadge(status) {
        const badges = {
            'COMPLETED': '<span class="badge bg-success"><i class="fas fa-check"></i> Completed</span>',
            'FAILED': '<span class="badge bg-danger"><i class="fas fa-times"></i> Failed</span>',
            'PROCESSING': '<span class="badge bg-primary"><i class="fas fa-spinner fa-spin"></i> Processing</span>',
            'QUEUED': '<span class="badge bg-warning"><i class="fas fa-clock"></i> Queued</span>',
            'CANCELLED': '<span class="badge bg-secondary"><i class="fas fa-ban"></i> Cancelled</span>'
        };
        return badges[status] || '<span class="badge bg-secondary">' + status + '</span>';
    }

    // Get progress bar HTML
    function getProgressBar(progress, status) {
        let colorClass = 'bg-primary';
        if (status === 'COMPLETED') {
            colorClass = 'bg-success';
        } else if (status === 'FAILED') {
            colorClass = 'bg-danger';
        }

        return '<div class="progress" style="height: 20px;">' +
               '<div class="progress-bar ' + colorClass + '" role="progressbar" ' +
               'style="width: ' + progress + '%">' + progress + '%</div>' +
               '</div>';
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
