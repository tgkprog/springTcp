#!/bin/bash
cd "$(dirname "$0")"

echo "=========================================="
echo "Final Comprehensive Test"
echo "=========================================="
echo ""

# Clean up
pkill -f "Tcp" 2>/dev/null
rm -f logs/*

# Start server
echo "1. Starting server..."
./runS > /dev/null 2>&1 &
SRV_PID=$!
sleep 10

# Test client
echo "2. Testing client with multiple commands..."
./runC-terminal > /tmp/client-output.txt 2>&1 <<EOF &
status
probe:shallow
probe:deep
mgt:info
mgt:time
m: 100 + 200
status
exit
EOF

CLIENT_PID=$!
sleep 5
echo "3. Cleaning up..."
kill $CLIENT_PID 2>/dev/null
kill $SRV_PID 2>/dev/null
pkill -9 -f "java.*tcp" 2>/dev/null

echo "=========================================="
echo "Final test complete."
echo "=========================================="
