/**
 * Admin Panel JavaScript
 * Handles real-time monitoring and manual simulation triggers
 */

(function() {
    'use strict';

    // Configuration
    const REFRESH_INTERVAL = 5000; // Refresh every 5 seconds

    // Initialize
    function init() {
        console.log('Initializing admin panel');

        // Load initial data
        loadAdminData();

        // Setup refresh button
        $('#refreshSimulationsBtn').on('click', function() {
            $(this).find('i').addClass('fa-spin');
            loadAdminData();
            setTimeout(() => {
                $('#refreshSimulationsBtn').find('i').removeClass('fa-spin');
            }, 1000);
        });

        // Auto-refresh every 5 seconds
        setInterval(loadAdminData, REFRESH_INTERVAL);
    }

    // Load all admin data
    function loadAdminData() {
        loadSystemMetrics();
        loadActiveSimulations();
    }

    // Load system metrics
    function loadSystemMetrics() {
        $.ajax({
            url: '/api/admin/metrics',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    updateSystemMetrics(response);
                } else {
                    console.error('Error loading metrics:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading system metrics:', xhr);
            }
        });
    }

    // Update system metrics display
    function updateSystemMetrics(data) {
        // Summary cards
        $('#activeSimulations').text(data.jobs.activeJobs || 0);
        $('#threadCount').text(data.threads.threadCount || 0);
        $('#memoryUsed').text(formatBytes(data.memory.usedMemory));
        $('#cpuCores').text(data.cpu.availableProcessors || 0);

        // Detailed info
        $('#totalMemory').text(formatBytes(data.memory.totalMemory));
        $('#freeMemory').text(formatBytes(data.memory.freeMemory));
        $('#maxMemory').text(formatBytes(data.memory.maxMemory));
        $('#peakThreadCount').text(data.threads.peakThreadCount || 0);
        $('#daemonThreads').text(data.threads.daemonThreadCount || 0);
        $('#lastUpdate').text(new Date(data.timestamp).toLocaleTimeString());
    }

    // Format bytes to human-readable format
    function formatBytes(bytes) {
        if (bytes === 0) return '0 MB';
        const mb = bytes / (1024 * 1024);
        return mb.toFixed(2) + ' MB';
    }

    // Load active simulations
    function loadActiveSimulations() {
        $.ajax({
            url: '/api/admin/simulations/active',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    renderActiveSimulations(response.simulations);
                } else {
                    console.error('Error loading simulations:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error loading active simulations:', xhr);
            }
        });
    }

    // Render active simulations table
    function renderActiveSimulations(simulations) {
        const tbody = $('#activeSimulationsTable');
        tbody.empty();

        if (simulations.length === 0) {
            tbody.append('<tr><td colspan="9" class="text-center text-muted">No active simulations</td></tr>');
            return;
        }

        simulations.forEach(function(sim) {
            const statusBadge = getStatusBadge(sim.status);
            const progressBar = getProgressBar(sim.progress, sim.status);
            const startedAt = sim.startedAt ? new Date(sim.startedAt).toLocaleString() : '--';

            const row = $('<tr>');
            row.append('<td><small><code>' + sim.jobId.substring(0, 8) + '...</code></small></td>');
            row.append('<td>' + sim.fileName + '</td>');
            row.append('<td><span class="badge bg-secondary">' + sim.operationType + '</span></td>');
            row.append('<td><span class="badge bg-info">' + sim.quality + '</span></td>');

            // Reveal simulation type
            const simType = sim.simulationType || 'NONE';
            let simBadge = '<span class="badge bg-dark">' + simType + '</span>';
            if (simType.includes('CPU')) {
                simBadge = '<span class="badge bg-danger">' + simType + '</span>';
            } else if (simType.includes('DATABASE')) {
                simBadge = '<span class="badge bg-primary">' + simType + '</span>';
            }
            row.append('<td>' + simBadge + '</td>');

            // Reveal endpoint
            const endpoint = sim.simulationEndpoint || '--';
            row.append('<td><small><code>' + endpoint + '</code></small></td>');

            row.append('<td>' + statusBadge + '</td>');
            row.append('<td>' + progressBar + '</td>');
            row.append('<td><small>' + startedAt + '</small></td>');

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

    // Trigger simulation (called from HTML)
    window.triggerSimulation = function(type) {
        console.log('Triggering simulation:', type);

        const btn = event.target.closest('button');
        const originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Starting...';

        $.ajax({
            url: '/api/admin/simulations/trigger',
            method: 'POST',
            data: { type: type },
            success: function(response) {
                if (response.success) {
                    showNotification('Success', response.message, 'success');

                    // Refresh simulations after 2 seconds
                    setTimeout(loadActiveSimulations, 2000);
                } else {
                    showNotification('Error', response.error || 'Failed to trigger simulation', 'error');
                }
            },
            error: function(xhr) {
                console.error('Error triggering simulation:', xhr);
                showNotification('Error', 'Failed to trigger simulation', 'error');
            },
            complete: function() {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
            }
        });
    };

    // Show notification
    function showNotification(title, message, type) {
        const alertClass = type === 'success' ? 'alert-success' : 'alert-danger';
        const icon = type === 'success' ? 'check-circle' : 'exclamation-triangle';

        const alert = $('<div>')
            .addClass('alert ' + alertClass + ' alert-dismissible fade show position-fixed')
            .css({
                'top': '80px',
                'right': '20px',
                'z-index': '9999',
                'min-width': '300px'
            })
            .html(
                '<i class="fas fa-' + icon + ' me-2"></i>' +
                '<strong>' + title + ':</strong> ' + message +
                '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>'
            );

        $('body').append(alert);

        // Auto-dismiss after 5 seconds
        setTimeout(function() {
            alert.fadeOut(function() {
                $(this).remove();
            });
        }, 5000);
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
