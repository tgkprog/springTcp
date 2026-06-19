#!/bin/bash
# Test Connection State Machine and Event Listeners

cd "$(dirname "$0")"

echo "=========================================="
echo "Connection State Machine & Events Test"
echo "=========================================="

# Clean up
pkill -9 -f "java.*tcp" 2>/dev/null
sleep 3
rm -f logs/*

wait_for_port() {
    local port=$1
    local timeout=$2
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if nc -z localhost $port >/dev/null 2>&1; then
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    return 1
}

wait_for_log() {
    local file=$1
    local pattern=$2
    local timeout=$3
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if [ -f "$file" ] && grep -q "$pattern" "$file"; then
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    return 1
}

# Start server
echo "1. Starting server..."
java -jar tcp-server/target/tcp-server-1.0.0.jar > /dev/null 2>&1 &
SRV_PID=$!

echo "Waiting for server to start on port 5039..."
if ! wait_for_port 5039 30; then
    echo "❌ Server failed to start on port 5039 in 30 seconds"
    pkill -9 -f "java.*tcp" 2>/dev/null
    exit 1
fi
echo "   ✅ Server is listening on port 5039"

# Start client
echo "2. Starting client with event listeners..."
tail -f /dev/null | TCP_CLIENT_MODE=terminal java -jar tcp-client/target/tcp-client-1.0.0.jar > /dev/null 2>&1 &
CLIENT_PID=$!

echo "Waiting for client to start and connect..."
if ! wait_for_log "logs/tcp-client.log" "Successfully connected to server" 60; then
    echo "❌ Client failed to connect in 60 seconds"
    tail -n 50 logs/tcp-client.log
    kill $CLIENT_PID 2>/dev/null
    pkill -9 -f "java.*tcp" 2>/dev/null
    exit 1
fi
echo "   ✅ Client successfully connected"

echo ""
echo "3. Checking for TCP CONNECTION OPENED event..."
grep "TCP CONNECTION OPENED" logs/tcp-client.log && echo "   ✅ Open event detected" || echo "   ❌ No open event"

echo ""
echo "4. Checking state machine transitions..."
grep "State transition:" logs/tcp-client.log | tail -5

echo ""
echo "5. Killing server to test disconnect detection..."
kill -9 $SRV_PID

echo "Waiting for client to detect disconnect..."
if ! wait_for_log "logs/tcp-client.log" "TCP CONNECTION CLOSED" 30; then
    echo "❌ Client failed to detect disconnect"
    tail -n 50 logs/tcp-client.log
else
    echo "   ✅ Disconnect detected"
fi

echo ""
echo "6. Checking for TCP CONNECTION CLOSED/EXCEPTION events..."
grep -E "TCP CONNECTION (CLOSED|EXCEPTION)" logs/tcp-client.log | tail -5

echo ""
echo "7. State machine transitions after disconnect:"
grep "State transition:.*DISCONNECTED\|State transition:.*ERROR" logs/tcp-client.log | tail -3

echo ""
echo "8. Summary:"
OPEN_EVENTS=$(grep "TCP CONNECTION OPENED" logs/tcp-client.log 2>/dev/null | wc -l)
CLOSE_EVENTS=$(grep "TCP CONNECTION CLOSED" logs/tcp-client.log 2>/dev/null | wc -l)
EXCEPT_EVENTS=$(grep "TCP CONNECTION EXCEPTION" logs/tcp-client.log 2>/dev/null | wc -l)
STATE_CHANGES=$(grep "State transition:" logs/tcp-client.log 2>/dev/null | wc -l)

echo "   Open events: $OPEN_EVENTS"
echo "   Close events: $CLOSE_EVENTS"
echo "   Exception events: $EXCEPT_EVENTS"
echo "   Total state transitions: $STATE_CHANGES"

if [ "$OPEN_EVENTS" -gt "0" ] && ([ "$CLOSE_EVENTS" -gt "0" ] || [ "$EXCEPT_EVENTS" -gt "0" ]); then
    echo ""
    echo "✅✅✅ SUCCESS! Event listeners are working!"
else
    echo ""
    echo "⚠️ Partial success or events not detected"
fi

echo ""
echo "9. Cleaning up..."
kill $CLIENT_PID 2>/dev/null
pkill -9 -f "java.*tcp" 2>/dev/null

echo ""
echo "=========================================="
echo "Test Complete - See logs/tcp-client.log"
echo "=========================================="
