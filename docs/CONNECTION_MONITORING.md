# Connection Monitoring Feature

## Overview
The TCP client includes an optional connection monitoring feature that periodically sends shallow probe commands to verify the connection health.

## Configuration

### Enable/Disable Monitoring
Add to `tcp-client/src/main/resources/application.yml`:

```yaml
tcp:
  client:
    monitoring:
      enabled: false      # Set to true to enable
      tick-ms: 2000       # Monitoring interval in milliseconds
```

### Default Values
- **enabled**: `false` (monitoring disabled by default)
- **tick-ms**: `2000` (2 seconds between probes)

## Connection Status

The client maintains an atomic connection status with the following states:

| Status | Description |
|--------|-------------|
| `UNKNOWN` | Initial state before any connection attempt |
| `CONNECTING` | Client is attempting to establish connection |
| `CONNECTED` | Successfully connected to server |
| `ERROR` | Connection error detected (transient state) |

### Status Transitions

```
UNKNOWN
   ↓
CONNECTING (on startup or reconnection)
   ↓
CONNECTED (connection successful)
   ↓
ERROR (connection lost/probe failed) → CONNECTING (immediate reconnect)
   ↓
CONNECTED (reconnection successful)
```

## How It Works

### Monitoring Process
1. **Scheduled Task**: Runs every `tick-ms` milliseconds (default: 2000ms)
2. **Shallow Probe**: Sends `probe:shallow` command to server
3. **Response Check**: Verifies response contains "OK"
4. **Status Update**: Updates atomic connection status based on result

### On Success
- If status was not `CONNECTED`, logs status change
- Sets status to `CONNECTED`

### On Failure (ERROR State)
The ERROR state is transient and triggers immediate action:

1. **Log Error**: Records the failure reason
2. **Set to ERROR**: Briefly sets status to ERROR
3. **Immediate Reconnect**: Triggers reconnection without delay
4. **Set to CONNECTING**: Changes status to CONNECTING
5. **Background Thread**: Reconnects in background thread

### Example Log Output

**Monitoring Disabled:**
```
2026-06-19 18:01:21.757 [main] INFO  c.t.t.c.service.TcpClientService - Connection Monitoring: DISABLED (tick: 2000ms)
```

**Monitoring Enabled:**
```
2026-06-19 18:05:30.123 [main] INFO  c.t.t.c.service.TcpClientService - Connection Monitoring: ENABLED (tick: 2000ms)
```

**Status Changes:**
```
2026-06-19 18:05:30.145 [main] INFO  c.t.t.c.service.TcpClientService - Connection status: CONNECTING
2026-06-19 18:05:30.200 [main] INFO  c.t.t.c.service.TcpClientService - Successfully connected to server
2026-06-19 18:05:30.201 [main] INFO  c.t.t.c.service.TcpClientService - Connection status: CONNECTED
```

**Health Check (Debug Level):**
```
2026-06-19 18:05:32.201 [scheduling-1] DEBUG c.t.t.c.service.TcpClientService - Running connection health check (shallow probe)
```

**Connection Lost:**
```
2026-06-19 18:05:34.250 [scheduling-1] WARN  c.t.t.c.service.TcpClientService - Connection health check failed: Socket closed
2026-06-19 18:05:34.251 [scheduling-1] INFO  c.t.t.c.service.TcpClientService - Connection status changed: CONNECTED -> ERROR
2026-06-19 18:05:34.251 [scheduling-1] INFO  c.t.t.c.service.TcpClientService - Triggering immediate reconnection...
2026-06-19 18:05:34.252 [scheduling-1] INFO  c.t.t.c.service.TcpClientService - Connection status changed: ERROR -> CONNECTING
2026-06-19 18:05:34.403 [reconnection-thread] INFO  c.t.t.c.service.TcpClientService - Attempting to connect to localhost:5039 (attempt 1)
2026-06-19 18:05:34.420 [reconnection-thread] INFO  c.t.t.c.service.TcpClientService - Successfully connected to server
2026-06-19 18:05:34.421 [reconnection-thread] INFO  c.t.t.c.service.TcpClientService - Connection status: CONNECTED
```

## Client Commands

### Check Connection Status
Type `status` in the client to view current connection status:

```
> status
Connection Status: CONNECTED
```

### Example Session
```
> status
Connection Status: CONNECTED

> probe:shallow
OK: Server is running

> m: 10 + 20
RESULT: 30

> status
Connection Status: CONNECTED

> exit
Exiting...
```

## Implementation Details

### Scheduling
- Uses Spring's `@Scheduled` annotation
- `@EnableScheduling` in `TcpClientApplication`
- Fixed delay scheduling: waits `tick-ms` after previous execution completes

### Thread Safety
- `AtomicReference<ConnectionStatus>` for thread-safe status updates
- Synchronized block for probe sending to avoid concurrent access
- Background reconnection in separate thread

### Synchronization
The monitoring probe is synchronized to prevent conflicts with user commands:

```java
synchronized (this) {
    // Send probe and read response
}
```

This ensures:
- User commands don't interfere with monitoring probes
- Monitoring probes don't interfere with user commands
- No race conditions on socket operations

## Configuration Recommendations

### Development
```yaml
tcp:
  client:
    monitoring:
      enabled: true
      tick-ms: 5000    # Less frequent probes
```

### Production
```yaml
tcp:
  client:
    monitoring:
      enabled: true
      tick-ms: 2000    # More frequent monitoring
```

### Testing
```yaml
tcp:
  client:
    monitoring:
      enabled: false    # Disable for testing
```

## Using Profiles

### Create a monitoring profile

**File: `application-monitor.yml`**
```yaml
tcp:
  client:
    monitoring:
      enabled: true
      tick-ms: 1000    # 1 second checks
```

### Run with profile
```bash
java -jar tcp-client.jar --spring.profiles.active=monitor
```

Or set environment variable:
```bash
export SPRING_PROFILES_ACTIVE=monitor
./runC
```

## API

### Get Connection Status
```java
@Autowired
private TcpClientService clientService;

ConnectionStatus status = clientService.getConnectionStatus();
```

### Status Values
```java
public enum ConnectionStatus {
    UNKNOWN,      // No connection attempt yet
    CONNECTING,   // Connection in progress
    CONNECTED,    // Successfully connected
    ERROR         // Error detected (transient)
}
```

## Troubleshooting

### Monitoring Not Running
- Check: `tcp.client.monitoring.enabled` is `true`
- Check: `@EnableScheduling` is present in application class
- Check logs for: "Connection Monitoring: ENABLED"

### Frequent Reconnections
- Increase `tick-ms` value
- Check server stability
- Check network connectivity
- Review logs for error patterns

### Status Always CONNECTING
- Server not running
- Wrong host/port configuration
- Firewall blocking connection
- Check server logs

## Performance Impact

### With Monitoring Enabled (tick-ms: 2000)
- **Network**: 1 small probe every 2 seconds (~10-20 bytes)
- **CPU**: Minimal (single scheduled task)
- **Memory**: Negligible (atomic reference + logs)

### Recommended Tick Values
- **High-frequency**: 1000ms (1 second) - for critical connections
- **Normal**: 2000ms (2 seconds) - balanced monitoring
- **Low-frequency**: 5000ms (5 seconds) - reduced overhead

## Summary

✅ **Optional feature** - disabled by default  
✅ **Configurable interval** - default 2000ms  
✅ **Atomic status tracking** - thread-safe  
✅ **Transient ERROR state** - immediate reconnect  
✅ **Background reconnection** - doesn't block user  
✅ **Synchronized probes** - no conflicts with commands  
✅ **Profile support** - enable via Spring profiles  
