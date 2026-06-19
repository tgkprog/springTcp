# Usage Examples

This document provides practical examples of using the TCP TGK server and client.

## Starting the Applications

### Terminal 1: Start the Server
```bash
cd tcp-server
mvn spring-boot:run
```

Expected output:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

...
TCP Server started on port 5039
```

### Terminal 2: Start the Client
```bash
cd tcp-client
mvn spring-boot:run
```

Expected output:
```
TCP Client Started. Type commands or 'exit' to quit.
Examples:
  probe:shallow
  probe:deep
  mgt:info
  mgt:time
  mgt:capabilities
  m: 5 + 10

> 
```

## Example Sessions

### Example 1: Basic Health Check
```
> probe:shallow
OK: Server is running

> 
```

### Example 2: Detailed System Information
```
> probe:deep
DEEP_PROBE:
Hostname: my-server
IP: 192.168.1.100
Available Processors: 8
Free Memory: 512 MB
Total Memory: 1024 MB
Max Memory: 4096 MB

> 
```

### Example 3: Server Information
```
> mgt:info
SERVER_INFO:
Name: TCP TGK Server
Version: 1.0.0
Java Version: 17.0.9
OS: Linux 5.15.0

> 
```

### Example 4: Get Server Time
```
> mgt:time
SERVER_TIME: 2026-06-19 10:06:43

> 
```

### Example 5: List Capabilities
```
> mgt:capabilities
CAPABILITIES:
1. Probe Commands:
   - probe:shallow - Basic server health check
   - probe:deep - Detailed system information
2. Management Commands:
   - mgt:info - Server information
   - mgt:time - Current server time
   - mgt:capabilities - List all capabilities
3. Math Server:
   - m: <expression> - Calculate arithmetic expressions
   - Supported operations: +, -, *, /, %, ^ (power)

> 
```

### Example 6: Simple Math Operations
```
> m: 5 + 10
RESULT: 15

> m: 100 - 25
RESULT: 75

> m: 7 * 8
RESULT: 56

> m: 20 / 4
RESULT: 5

> m: 17 % 5
RESULT: 2

> m: 2 ^ 8
RESULT: 256

> 
```

### Example 7: Decimal Results
```
> m: 10 / 3
RESULT: 3.3333333333333335

> m: 5.5 + 2.3
RESULT: 7.8

> 
```

### Example 8: Error Handling
```
> m: 10 / 0
ERROR: Division by zero

> m: abc + 5
ERROR: Invalid number format

> m: 5 & 10
ERROR: Unknown operator '&'. Supported: +, -, *, /, %, ^

> unknown:command
ERROR: Unknown command. Use probe:, mgt:, or m:

> 
```

### Example 9: Exit Client
```
> exit
Exiting...
```

## Using with Command-Line Tools

### Using netcat
```bash
# Terminal 1: Start server
cd tcp-server
mvn spring-boot:run

# Terminal 2: Connect with netcat
nc localhost 5039
probe:shallow
OK: Server is running

m: 5 + 10
RESULT: 15

^C  # Ctrl+C to disconnect
```

### Using telnet
```bash
telnet localhost 5039
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.

mgt:time
SERVER_TIME: 2026-06-19 10:06:43

^]  # Ctrl+] then 'quit' to exit
telnet> quit
```

### Using curl (for single commands)
```bash
# Note: curl doesn't work well with persistent TCP connections
# Use nc or telnet for interactive sessions

# One-shot command with nc
echo "probe:shallow" | nc localhost 5039
```

## Scripting Examples

### Bash Script: Health Check
```bash
#!/bin/bash
# health-check.sh

RESPONSE=$(echo "probe:shallow" | nc -w 1 localhost 5039)

if [[ $RESPONSE == *"OK"* ]]; then
    echo "Server is healthy"
    exit 0
else
    echo "Server is down"
    exit 1
fi
```

### Bash Script: Math Calculator
```bash
#!/bin/bash
# calculator.sh

if [ $# -ne 3 ]; then
    echo "Usage: $0 <num1> <operator> <num2>"
    echo "Example: $0 5 + 10"
    exit 1
fi

COMMAND="m: $1 $2 $3"
RESPONSE=$(echo "$COMMAND" | nc -w 1 localhost 5039)
echo "$RESPONSE"
```

Usage:
```bash
chmod +x calculator.sh
./calculator.sh 5 + 10
# Output: RESULT: 15

./calculator.sh 2 ^ 10
# Output: RESULT: 1024
```

### Python Script: Interactive Client
```python
#!/usr/bin/env python3
import socket

def send_command(host, port, command):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((host, port))
        s.sendall((command + "\r\n").encode('utf-8'))
        
        response = b""
        s.settimeout(1.0)
        try:
            while True:
                data = s.recv(1024)
                if not data:
                    break
                response += data
        except socket.timeout:
            pass
        
        return response.decode('utf-8').strip()

# Example usage
if __name__ == "__main__":
    host = "localhost"
    port = 5039
    
    commands = [
        "probe:shallow",
        "mgt:time",
        "m: 5 + 10",
        "m: 2 ^ 16"
    ]
    
    for cmd in commands:
        print(f"Command: {cmd}")
        response = send_command(host, port, cmd)
        print(f"Response: {response}")
        print()
```

## Monitoring and Testing

### Load Testing (Simple)
```bash
#!/bin/bash
# load-test.sh

for i in {1..100}; do
    (echo "probe:shallow" | nc -w 1 localhost 5039) &
done
wait
echo "Completed 100 concurrent requests"
```

### Continuous Health Monitoring
```bash
#!/bin/bash
# monitor.sh

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    RESPONSE=$(echo "probe:shallow" | nc -w 1 localhost 5039)
    echo "[$TIMESTAMP] $RESPONSE"
    sleep 5
done
```

## Troubleshooting

### Connection Refused
```bash
> probe:shallow
Error: Connection refused

# Solution: Make sure server is running
cd tcp-server
mvn spring-boot:run
```

### Timeout
```bash
> probe:shallow
Error: Read timed out

# Solution: Check if server is responsive
# Try increasing timeout in client configuration
```

### Invalid Response
```bash
> probe:shallow
ERROR: Unknown command. Use probe:, mgt:, or m:

# Solution: Check command syntax
# Correct format: probe:shallow (not "probe shallow")
```
