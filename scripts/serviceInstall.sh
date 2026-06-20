#!/bin/bash
# -----------------------------------------------------------------------------
# serviceInstall.sh
# Installs and starts the "mathServer" systemd service.
# Run with sudo: sudo ./serviceInstall.sh
# -----------------------------------------------------------------------------

set -e

# Must be run as root
if [ "$EUID" -ne 0 ]; then
    echo "Error: This script must be run as root (sudo)."
    exit 1
fi

SERVICE_NAME="mathServer"
SERVICE_PATH="/etc/systemd/system/${SERVICE_NAME}.service"
APP_DIR="/data/code/springTcp"
JAR_PATH="${APP_DIR}/tcp-server/target/tcp-server-1.0.0.jar"

# Determine user to run the service
# We run it as the user who invoked sudo, otherwise default to "ubuntu"
SERVICE_USER=${SUDO_USER:-ubuntu}

echo "=== Installing ${SERVICE_NAME} Service ==="
echo "Application Directory: ${APP_DIR}"
echo "JAR Path:             ${JAR_PATH}"
echo "Service User:         ${SERVICE_USER}"

# Verify if target JAR exists (warning only, as the user might build it later)
if [ ! -f "${JAR_PATH}" ]; then
    echo "Warning: JAR file not found at ${JAR_PATH}."
    echo "Please ensure the project is built at that location."
fi

# Write systemd service file
cat <<EOF > "${SERVICE_PATH}"
[Unit]
Description=Math Server Service
After=network.target

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${APP_DIR}
ExecStart=/usr/bin/java -jar ${JAR_PATH}
Restart=always
RestartSec=5
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

echo "Service file written to ${SERVICE_PATH}"

# Reload systemd
echo "Reloading systemd daemon..."
systemctl daemon-reload

# Enable service
echo "Enabling service to start on boot..."
systemctl enable "${SERVICE_NAME}"

# Start service
echo "Starting service..."
systemctl start "${SERVICE_NAME}"

echo "=== Installation Completed Successfully ==="
systemctl status "${SERVICE_NAME}" --no-pager
