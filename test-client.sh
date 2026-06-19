#!/bin/bash
cd "$(dirname "$0")"
rm -f logs/*.log
echo "Starting server..."
./runS > /dev/null 2>&1 &
SRV_PID=$!
sleep 10

echo "Starting client..."
./runC &
CLIENT_PID=$!
sleep 8

echo ""
echo "=== Client Log (monitoring status) ==="
grep "Monitoring" logs/tcp-client.log

echo ""
echo "=== Waiting 15 seconds to see if reconnections happen ==="
sleep 15

echo ""
echo "=== Check for ERROR/reconnect in logs ==="
grep -E "(ERROR|WARN|reconnect)" logs/tcp-client.log | tail -10

echo ""
echo "Stopping..."
kill $CLIENT_PID $SRV_PID 2>/dev/null
wait 2>/dev/null
