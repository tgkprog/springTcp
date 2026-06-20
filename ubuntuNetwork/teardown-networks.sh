#!/bin/bash
# ----------------------------------------------------
# Multi-Network Namespace Teardown Script
# Run this script with sudo (e.g. sudo ./teardown-networks.sh)
# ----------------------------------------------------

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Error: Please run this script as root (sudo)."
  exit 1
fi

SUBNET_C="10.200.1.0/24"
SUBNET_R="10.200.2.0/24"

echo "=== Auto-detecting Host WAN Interface ==="
WAN_IFACE=$(ip route show | grep default | awk '{print $5}' | head -n1)
if [ -z "$WAN_IFACE" ]; then
  WAN_IFACE="eth0"
fi

echo "=== Deleting Namespaces and Interfaces ==="
ip netns del netns_client 2>/dev/null || true
ip netns del netns_relay 2>/dev/null || true
ip link del veth_c 2>/dev/null || true
ip link del veth_r 2>/dev/null || true

echo "=== Cleaning Up Firewall Rules ==="
iptables -D FORWARD -i veth_c -o veth_r -j ACCEPT 2>/dev/null || true
iptables -D FORWARD -i veth_r -o veth_c -j ACCEPT 2>/dev/null || true
iptables -D FORWARD -i veth_c -o $WAN_IFACE -j ACCEPT 2>/dev/null || true
iptables -D FORWARD -i veth_r -o $WAN_IFACE -j ACCEPT 2>/dev/null || true
iptables -t nat -D POSTROUTING -s $SUBNET_R -o $WAN_IFACE -j MASQUERADE 2>/dev/null || true
iptables -t nat -D POSTROUTING -s $SUBNET_C -o $WAN_IFACE -j MASQUERADE 2>/dev/null || true

echo "=== Network Cleanup Complete ==="
