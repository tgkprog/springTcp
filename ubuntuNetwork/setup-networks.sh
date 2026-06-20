#!/bin/bash
# ----------------------------------------------------
# Multi-Network Namespace Setup Script
# Run this script with sudo (e.g. sudo ./setup-networks.sh)
# ----------------------------------------------------

set -e

# Configuration
IP_C="10.200.1.2"
GW_C="10.200.1.1"
SUBNET_C="10.200.1.0/24"

IP_R="10.200.2.2"
GW_R="10.200.2.1"
SUBNET_R="10.200.2.0/24"

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Error: Please run this script as root (sudo)."
  exit 1
fi

echo "=== Auto-detecting Host WAN Interface ==="
WAN_IFACE=$(ip route show | grep default | awk '{print $5}' | head -n1)
if [ -z "$WAN_IFACE" ]; then
  echo "Warning: Default WAN interface not found. Falling back to eth0."
  WAN_IFACE="eth0"
else
  echo "Detected WAN interface: $WAN_IFACE"
fi

echo "=== Enabling IP Routing on Host ==="
sysctl -w net.ipv4.ip_forward=1

echo "=== Creating Namespaces ==="
ip netns add netns_client
ip netns add netns_relay

echo "=== Configuring Client Network (Namespace: netns_client) ==="
# Create virtual ethernet pair connecting host and client namespace
ip link add veth_c type veth peer name veth_c_ns
ip link set veth_c_ns netns netns_client

# Bind gateway IP to host side of the tunnel
ip addr add ${GW_C}/24 dev veth_c
ip link set veth_c up

# Bind static IP to namespace side of the tunnel and set default route
ip netns exec netns_client ip addr add ${IP_C}/24 dev veth_c_ns
ip netns exec netns_client ip link set veth_c_ns up
ip netns exec netns_client ip link set lo up
ip netns exec netns_client ip route add default via ${GW_C}

echo "=== Configuring Relay Network (Namespace: netns_relay) ==="
# Create virtual ethernet pair connecting host and relay namespace
ip link add veth_r type veth peer name veth_r_ns
ip link set veth_r_ns netns netns_relay

# Bind gateway IP to host side of the tunnel
ip addr add ${GW_R}/24 dev veth_r
ip link set veth_r up

# Bind static IP to namespace side of the tunnel and set default route
ip netns exec netns_relay ip addr add ${IP_R}/24 dev veth_r_ns
ip netns exec netns_relay ip link set veth_r_ns up
ip netns exec netns_relay ip link set lo up
ip netns exec netns_relay ip route add default via ${GW_R}

echo "=== Setting Up Host Routing and NAT (Masquerading) ==="
# Forwarding rules to allow subnets to communicate via the host router
iptables -A FORWARD -i veth_c -o veth_r -j ACCEPT
iptables -A FORWARD -i veth_r -o veth_c -j ACCEPT

# Allow subnets to reach the outside internet via host's WAN interface
iptables -A FORWARD -i veth_c -o $WAN_IFACE -j ACCEPT
iptables -A FORWARD -i veth_r -o $WAN_IFACE -j ACCEPT
iptables -A FORWARD -m state --state ESTABLISHED,RELATED -j ACCEPT

# Perform masquerade translation
iptables -t nat -A POSTROUTING -s $SUBNET_R -o $WAN_IFACE -j MASQUERADE
iptables -t nat -A POSTROUTING -s $SUBNET_C -o $WAN_IFACE -j MASQUERADE

echo "=== Setup Completed Successfully ==="
echo "Client Network: $IP_C/24 via gateway $GW_C"
echo "Relay Network:  $IP_R/24 via gateway $GW_R"
echo "To test connection between namespaces:"
echo "  sudo ip netns exec netns_client ping -c 3 $IP_R"
