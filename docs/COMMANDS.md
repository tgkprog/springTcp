# Command Specifications

The TCP server supports three categories of commands: **Probe**, **Management**, and **Math**.

## 1. Probe Commands

Probe commands are used for server health checks and diagnostics.

### Format
```
probe:<type>
```

### Types

#### a) Shallow Probe
**Command**: `probe:shallow`

**Description**: Performs a basic server health check.

**Response**: 
```
OK: Server is running
```

**Use Case**: Quick connectivity and availability check.

#### b) Deep Probe
**Command**: `probe:deep`

**Description**: Provides detailed system information including hostname, IP address, processor count, and memory statistics.

**Response**: 
```
DEEP_PROBE:
Hostname: server-hostname
IP: 192.168.1.100
Available Processors: 8
Free Memory: 512 MB
Total Memory: 1024 MB
Max Memory: 4096 MB
```

**Use Case**: Detailed system diagnostics and monitoring.

---

## 2. Management Commands

Management commands provide server information and capabilities.

### Format
```
mgt:<type>
```

### Types

#### a) Request for Info
**Command**: `mgt:info`

**Description**: Returns server information including name, version, Java version, and operating system.

**Response**: 
```
SERVER_INFO:
Name: TCP TGK Server
Version: 1.0.0
Java Version: 17.0.x
OS: Linux 5.x.x
```

#### b) Time
**Command**: `mgt:time`

**Description**: Returns the current server time.

**Response**: 
```
SERVER_TIME: 2026-06-19 10:06:43
```

**Format**: yyyy-MM-dd HH:mm:ss

#### c) Capabilities
**Command**: `mgt:capabilities`

**Description**: Lists all available commands and their descriptions.

**Response**: 
```
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
```

---

## 3. Math Server Commands

Math server commands perform arithmetic calculations.

### Format
```
m: <number> <operator> <number>
```

### Supported Operators

| Operator | Operation       | Example     | Result      |
|----------|----------------|-------------|-------------|
| `+`      | Addition       | `m: 5 + 10` | RESULT: 15  |
| `-`      | Subtraction    | `m: 10 - 3` | RESULT: 7   |
| `*`      | Multiplication | `m: 4 * 5`  | RESULT: 20  |
| `/`      | Division       | `m: 20 / 4` | RESULT: 5   |
| `%`      | Modulo         | `m: 10 % 3` | RESULT: 1   |
| `^`      | Power          | `m: 2 ^ 8`  | RESULT: 256 |

### Examples

#### Addition
```
Command:  m: 5 + 10
Response: RESULT: 15
```

#### Subtraction
```
Command:  m: 100 - 25
Response: RESULT: 75
```

#### Division
```
Command:  m: 15 / 3
Response: RESULT: 5
```

#### Multiplication
```
Command:  m: 7 * 8
Response: RESULT: 56
```

#### Modulo
```
Command:  m: 17 % 5
Response: RESULT: 2
```

#### Power
```
Command:  m: 3 ^ 4
Response: RESULT: 81
```

### Error Handling

The math server handles errors gracefully:

**Division by Zero**:
```
Command:  m: 10 / 0
Response: ERROR: Division by zero
```

**Invalid Format**:
```
Command:  m: 5 +
Response: ERROR: Invalid expression format. Use: m: <number> <operator> <number>
```

**Invalid Number**:
```
Command:  m: abc + 5
Response: ERROR: Invalid number format
```

**Unknown Operator**:
```
Command:  m: 5 & 10
Response: ERROR: Unknown operator '&'. Supported: +, -, *, /, %, ^
```

---

## General Error Handling

If an unknown command is sent:
```
Command:  unknown:command
Response: ERROR: Unknown command. Use probe:, mgt:, or m:
```

## Command Format Rules

1. Commands are **case-insensitive** for the command type (probe, mgt, m)
2. Spaces around operators in math expressions are **required**
3. All commands end with a CRLF (`\r\n`)
4. Commands are processed synchronously - one command at a time per connection
