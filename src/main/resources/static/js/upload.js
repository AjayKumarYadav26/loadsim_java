/**
 * Document Upload JavaScript
 * Handles file selection, drag-and-drop, validation, and AJAX upload
 */

(function() {
    'use strict';

    // DOM Elements
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const uploadForm = document.getElementById('uploadForm');
    const submitBtn = document.getElementById('submitBtn');
    const fileInfo = document.getElementById('fileInfo');
    const fileName = document.getElementById('fileName');
    const fileSize = document.getElementById('fileSize');
    const uploadProgress = document.getElementById('uploadProgress');
    const progressBar = document.getElementById('progressBar');

    // Configuration
    const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    const ALLOWED_EXTENSIONS = ['pdf', 'csv', 'xlsx', 'xls', 'txt'];

    let selectedFile = null;

    // Initialize
    function init() {
        setupEventListeners();
    }

    // Setup Event Listeners
    function setupEventListeners() {
        // File input change
        fileInput.addEventListener('change', handleFileSelect);

        // Drag and drop
        dropZone.addEventListener('dragover', handleDragOver);
        dropZone.addEventListener('dragleave', handleDragLeave);
        dropZone.addEventListener('drop', handleDrop);

        // Form submit
        uploadForm.addEventListener('submit', handleFormSubmit);

        // Prevent default drag/drop on document
        document.addEventListener('dragover', function(e) {
            e.preventDefault();
        });
        document.addEventListener('drop', function(e) {
            e.preventDefault();
        });
    }

    // Handle File Selection
    function handleFileSelect(e) {
        const files = e.target.files;
        if (files.length > 0) {
            processFile(files[0]);
        }
    }

    // Handle Drag Over
    function handleDragOver(e) {
        e.preventDefault();
        e.stopPropagation();
        dropZone.classList.add('dragover');
    }

    // Handle Drag Leave
    function handleDragLeave(e) {
        e.preventDefault();
        e.stopPropagation();
        dropZone.classList.remove('dragover');
    }

    // Handle Drop
    function handleDrop(e) {
        e.preventDefault();
        e.stopPropagation();
        dropZone.classList.remove('dragover');

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            fileInput.files = files; // Set file input
            processFile(files[0]);
        }
    }

    // Process Selected File
    function processFile(file) {
        console.log('Processing file:', file.name, file.size);

        // Validate file
        const validation = validateFile(file);
        if (!validation.valid) {
            showError(validation.message);
            selectedFile = null;
            submitBtn.disabled = true;
            fileInfo.style.display = 'none';
            return;
        }

        // Store selected file
        selectedFile = file;

        // Display file info
        fileName.textContent = file.name;
        fileSize.textContent = formatFileSize(file.size);
        fileInfo.style.display = 'block';

        // Enable submit button
        submitBtn.disabled = false;
    }

    // Validate File
    function validateFile(file) {
        // Check if file exists
        if (!file) {
            return { valid: false, message: 'No file selected' };
        }

        // Check file size
        if (file.size > MAX_FILE_SIZE) {
            return {
                valid: false,
                message: 'File size exceeds maximum allowed size of 50MB'
            };
        }

        // Check file extension
        const extension = getFileExtension(file.name).toLowerCase();
        if (!ALLOWED_EXTENSIONS.includes(extension)) {
            return {
                valid: false,
                message: `File type '${extension}' is not allowed. Allowed types: ${ALLOWED_EXTENSIONS.join(', ')}`
            };
        }

        return { valid: true };
    }

    // Handle Form Submit
    function handleFormSubmit(e) {
        e.preventDefault();

        if (!selectedFile) {
            showError('Please select a file to upload');
            return;
        }

        // Get form data
        const operationType = document.querySelector('input[name="operationType"]:checked').value;
        const quality = document.querySelector('input[name="quality"]:checked').value;

        console.log('Uploading:', selectedFile.name, 'Operation:', operationType, 'Quality:', quality);

        // Create FormData
        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('operationType', operationType);
        formData.append('quality', quality);

        // Upload file
        uploadFile(formData);
    }

    // Upload File via AJAX
    function uploadFile(formData) {
        // Show progress
        uploadProgress.style.display = 'block';
        uploadForm.style.display = 'none';
        updateProgress(0);

        // Create XMLHttpRequest
        const xhr = new XMLHttpRequest();

        // Progress event
        xhr.upload.addEventListener('progress', function(e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                updateProgress(percentComplete);
            }
        });

        // Load event (success)
        xhr.addEventListener('load', function() {
            if (xhr.status === 200) {
                try {
                    const response = JSON.parse(xhr.responseText);
                    if (response.success) {
                        handleUploadSuccess(response);
                    } else {
                        handleUploadError(response.error || 'Upload failed');
                    }
                } catch (err) {
                    handleUploadError('Invalid server response');
                }
            } else {
                handleUploadError('Upload failed with status: ' + xhr.status);
            }
        });

        // Error event
        xhr.addEventListener('error', function() {
            handleUploadError('Network error during upload');
        });

        // Send request
        xhr.open('POST', '/api/portal/upload/document');
        xhr.send(formData);
    }

    // Handle Upload Success
    function handleUploadSuccess(response) {
        console.log('Upload successful:', response);
        updateProgress(100);

        // Show success message
        showSuccess('File uploaded successfully! Redirecting to processing page...');

        // Redirect to processing page after 1 second
        setTimeout(function() {
            window.location.href = '/portal/process/' + response.jobId;
        }, 1000);
    }

    // Handle Upload Error
    function handleUploadError(message) {
        console.error('Upload error:', message);
        uploadProgress.style.display = 'none';
        uploadForm.style.display = 'block';
        showError(message || 'Upload failed. Please try again.');
    }

    // Update Progress Bar
    function updateProgress(percent) {
        progressBar.style.width = percent + '%';
        progressBar.textContent = percent + '%';
        progressBar.setAttribute('aria-valuenow', percent);
    }

    // Show Success Message
    function showSuccess(message) {
        alert('Success: ' + message);
    }

    // Show Error Message
    function showError(message) {
        alert('Error: ' + message);
    }

    // Get File Extension
    function getFileExtension(filename) {
        const parts = filename.split('.');
        return parts.length > 1 ? parts[parts.length - 1] : '';
    }

    // Format File Size
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
