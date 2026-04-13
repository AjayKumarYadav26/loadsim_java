/**
 * Document Processing JavaScript
 * Handles real-time status polling and progress updates
 */

(function() {
    'use strict';

    // Configuration
    const POLL_INTERVAL = 2000; // Poll every 2 seconds
    const MAX_POLL_ATTEMPTS = 900; // Max 30 minutes (900 * 2s = 1800s)

    let pollCount = 0;
    let pollInterval = null;
    let isProcessingStarted = false;

    // Initialize
    function init() {
        console.log('Initializing processing page for job:', jobId);

        // Start processing if not already started
        if (initialStatus === 'QUEUED' || initialStatus === 'UPLOADING') {
            startProcessing();
        } else if (initialStatus === 'PROCESSING') {
            isProcessingStarted = true;
            startPolling();
        } else if (initialStatus === 'COMPLETED') {
            handleCompletion();
        } else if (initialStatus === 'FAILED') {
            handleFailure('Processing failed');
        }

        // Setup cancel button
        $('#cancelBtn').on('click', handleCancel);
    }

    // Start processing job
    function startProcessing() {
        console.log('Starting processing for job:', jobId);

        $.ajax({
            url: '/api/portal/processing/start',
            method: 'POST',
            data: { jobId: jobId },
            success: function(response) {
                if (response.success) {
                    console.log('Processing started successfully');
                    isProcessingStarted = true;
                    updateStatus('PROCESSING', 0, 'Processing started...');
                    startPolling();
                } else {
                    handleFailure(response.error || 'Failed to start processing');
                }
            },
            error: function(xhr) {
                console.error('Error starting processing:', xhr);
                handleFailure('Failed to start processing. Please try again.');
            }
        });
    }

    // Start polling for status updates
    function startPolling() {
        console.log('Starting status polling');
        pollStatus(); // Poll immediately
        pollInterval = setInterval(pollStatus, POLL_INTERVAL);
    }

    // Stop polling
    function stopPolling() {
        if (pollInterval) {
            clearInterval(pollInterval);
            pollInterval = null;
            console.log('Stopped status polling');
        }
    }

    // Poll job status
    function pollStatus() {
        pollCount++;

        // Safety check: stop polling after max attempts
        if (pollCount > MAX_POLL_ATTEMPTS) {
            console.warn('Max poll attempts reached');
            stopPolling();
            handleFailure('Processing timeout - please check job status manually');
            return;
        }

        $.ajax({
            url: '/api/portal/processing/' + jobId + '/status',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    handleStatusUpdate(response);
                } else {
                    console.error('Status error:', response.error);
                }
            },
            error: function(xhr) {
                console.error('Error polling status:', xhr);
                // Don't stop polling on network errors - retry
            }
        });
    }

    // Handle status update
    function handleStatusUpdate(data) {
        console.log('Status update:', data.status, data.progress + '%');

        // Update UI
        updateStatus(data.status, data.progress, getStatusMessage(data));

        // Update time information
        if (data.elapsedSeconds !== undefined) {
            updateElapsedTime(data.elapsedSeconds);
        }
        if (data.remainingSeconds !== undefined) {
            updateRemainingTime(data.remainingSeconds);
        }

        // Update details section
        updateDetails(data);

        // Check for terminal states
        if (data.status === 'COMPLETED') {
            stopPolling();
            handleCompletion(data.result);
        } else if (data.status === 'FAILED') {
            stopPolling();
            handleFailure(data.error || 'Processing failed');
        } else if (data.status === 'CANCELLED') {
            stopPolling();
            handleCancellation();
        }
    }

    // Update status display
    function updateStatus(status, progress, message) {
        // Update progress bar
        const progressBar = $('#progressBar');
        progressBar.css('width', progress + '%');
        progressBar.attr('aria-valuenow', progress);
        $('#progressText').text(progress + '%');

        // Update status text
        $('#statusText').text(status);

        // Update message
        $('#messageText').text(message);

        // Update progress bar color based on progress
        progressBar.removeClass('bg-primary bg-success bg-danger bg-warning');
        if (progress >= 100) {
            progressBar.addClass('bg-success');
            progressBar.removeClass('progress-bar-animated');
        } else if (progress >= 75) {
            progressBar.addClass('bg-primary');
        } else {
            progressBar.addClass('bg-primary');
        }
    }

    // Get status message based on data
    function getStatusMessage(data) {
        const operation = data.operationType || 'document';
        const progress = data.progress || 0;

        if (progress === 0) {
            return 'Initializing processing...';
        } else if (progress < 25) {
            return 'Starting ' + operation.toLowerCase() + ' operation...';
        } else if (progress < 50) {
            return 'Processing ' + operation.toLowerCase() + '...';
        } else if (progress < 75) {
            return 'Continuing ' + operation.toLowerCase() + '...';
        } else if (progress < 100) {
            return 'Finalizing ' + operation.toLowerCase() + '...';
        } else {
            return 'Completed!';
        }
    }

    // Update elapsed time
    function updateElapsedTime(seconds) {
        const formatted = formatTime(seconds);
        $('#elapsedTime').text(formatted);
    }

    // Update remaining time
    function updateRemainingTime(seconds) {
        const formatted = formatTime(seconds);
        $('#remainingTime').text(formatted);
    }

    // Format time (seconds to MM:SS)
    function formatTime(seconds) {
        if (seconds === undefined || seconds < 0) {
            return '--:--';
        }
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return mins + ':' + (secs < 10 ? '0' : '') + secs;
    }

    // Update details section
    function updateDetails(data) {
        if (data.status) {
            $('#detailStatus').text(data.status);
        }
        if (data.progress !== undefined) {
            $('#detailProgress').text(data.progress + '%');
        }
        if (data.simulationType) {
            $('#detailSimulationType').text(data.simulationType);
        }
        if (data.elapsedSeconds !== undefined) {
            const startTime = new Date(Date.now() - (data.elapsedSeconds * 1000));
            $('#detailStartedAt').text(startTime.toLocaleString());
        }
    }

    // Handle completion
    function handleCompletion(resultMessage) {
        console.log('Processing completed');

        // Update UI
        updateStatus('COMPLETED', 100, 'Processing completed successfully!');
        $('#remainingTime').text('0:00');

        // Update status message
        $('#statusMessage').removeClass('alert-info').addClass('alert-success');
        $('#statusMessage').html('<i class="fas fa-check-circle me-2"></i><span>Processing completed successfully!</span>');

        // Show completion section
        if (resultMessage) {
            $('#resultMessage').text(resultMessage);
        }
        $('#completionSection').show();
        $('#cancelSection').hide();

        // Remove spinner from heading
        $('#statusHeading i').removeClass('fa-spinner fa-spin').addClass('fa-check-circle');
    }

    // Handle failure
    function handleFailure(errorMessage) {
        console.error('Processing failed:', errorMessage);

        // Update status message
        $('#statusMessage').removeClass('alert-info').addClass('alert-danger');
        $('#statusMessage').html('<i class="fas fa-exclamation-triangle me-2"></i><span>Processing failed</span>');

        // Show error section
        $('#errorMessage').text(errorMessage || 'An error occurred during processing');
        $('#errorSection').show();
        $('#cancelSection').hide();

        // Update progress bar
        $('#progressBar').removeClass('progress-bar-animated bg-primary').addClass('bg-danger');

        // Remove spinner from heading
        $('#statusHeading i').removeClass('fa-spinner fa-spin').addClass('fa-exclamation-triangle');
    }

    // Handle cancellation
    function handleCancellation() {
        console.log('Processing cancelled');

        // Update status message
        $('#statusMessage').removeClass('alert-info').addClass('alert-warning');
        $('#statusMessage').html('<i class="fas fa-info-circle me-2"></i><span>Processing was cancelled</span>');

        // Update progress bar
        $('#progressBar').removeClass('progress-bar-animated bg-primary').addClass('bg-warning');

        // Hide cancel button
        $('#cancelSection').hide();

        // Show return button
        const returnBtn = $('<a>')
            .attr('href', '/portal')
            .addClass('btn btn-primary btn-lg w-100 mt-3')
            .html('<i class="fas fa-home me-2"></i>Return to Home');
        $('#cancelSection').replaceWith(returnBtn);
    }

    // Handle cancel button click
    function handleCancel() {
        if (!confirm('Are you sure you want to cancel this processing job?')) {
            return;
        }

        console.log('Cancelling job:', jobId);
        $('#cancelBtn').prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-2"></i>Cancelling...');

        $.ajax({
            url: '/api/portal/processing/' + jobId + '/cancel',
            method: 'PUT',
            success: function(response) {
                if (response.success) {
                    stopPolling();
                    handleCancellation();
                } else {
                    alert('Failed to cancel: ' + (response.error || 'Unknown error'));
                    $('#cancelBtn').prop('disabled', false).html('<i class="fas fa-times me-2"></i>Cancel Processing');
                }
            },
            error: function(xhr) {
                console.error('Error cancelling job:', xhr);
                alert('Failed to cancel processing. Please try again.');
                $('#cancelBtn').prop('disabled', false).html('<i class="fas fa-times me-2"></i>Cancel Processing');
            }
        });
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
