#!/bin/bash
# -----------------------------------------------------------------------------
# serviceUninstall.sh
# Stops, disables, and removes the "mathServer" systemd service.
# Run with sudo: sudo ./serviceUninstall.sh
# -----------------------------------------------------------------------------

set -e

# Must be run as root
if [ "$EUID" -ne 0 ]; then
    echo "Error: This script must be run as root (sudo)."
    exit 1
fi

SERVICE_NAME="mathServer"
SERVICE_PATH="/etc/systemd/system/${SERVICE_NAME}.service"

echo "=== Uninstalling ${SERVICE_NAME} Service ==="

# Stop service
if systemctl is-active --quiet "${SERVICE_NAME}"; then
    echo "Stopping active service..."
    systemctl stop "${SERVICE_NAME}"
fi

# Disable service
if systemctl is-enabled --quiet "${SERVICE_NAME}" 2>/dev/null; then
    echo "Disabling service..."
    systemctl disable "${SERVICE_NAME}"
fi

# Remove service file
if [ -f "${SERVICE_PATH}" ]; then
    echo "Removing service file: ${SERVICE_PATH}"
    rm "${SERVICE_PATH}"
fi

# Reload systemd
echo "Reloading systemd daemon..."
systemctl daemon-reload
systemctl reset-failed 2>/dev/null || true

echo "=== Uninstall Completed Successfully ==="
