# TCP Protocol Specification

## Overview
The TCP TGK server implements a simple text-based protocol over TCP/IP.

## Connection Details

### Server
- **Protocol**: TCP
- **Default Port**: 5039
- **Host**: Configurable (default: 0.0.0.0 - listens on all interfaces)
- **Connection Type**: Persistent connections supported

### Connection Flow
1. Client connects to server on port 5039
2. Client sends command (text string terminated with CRLF)
3. Server processes command
4. Server sends response (text string terminated with CRLF)
5. Connection can remain open for multiple commands or be closed

## Message Format

### Request Format
```
<command>CRLF
```

- **command**: ASCII text string
- **CRLF**: Carriage Return + Line Feed (`\r\n`)

### Response Format
```
<response>CRLF
```

- **response**: ASCII text string (can be multi-line)
- **CRLF**: Each line terminated with Carriage Return + Line Feed (`\r\n`)

## Protocol Characteristics

### Character Encoding
- **Encoding**: UTF-8
- **Line Terminator**: CRLF (`\r\n`)

### Message Framing
- Messages are delimited by CRLF
- Spring Integration's `ByteArrayCrLfSerializer` is used for serialization/deserialization
- Maximum message size: Default Spring Integration limits apply

### Connection Management
- **Server**: Accepts multiple concurrent connections
- **Client**: Can maintain persistent connection or use one-shot connections
- **Timeout**: Configurable (client default: 5 seconds read timeout)

### Command Processing
- Commands are processed **sequentially** per connection
- Server is **thread-safe** and can handle multiple connections concurrently
- Each connection is handled in its own thread

## Network Layer

### TCP Socket Configuration

#### Server
```java
TcpNetServerConnectionFactory connectionFactory = new TcpNetServerConnectionFactory(port);
connectionFactory.setSerializer(new ByteArrayCrLfSerializer());
connectionFactory.setDeserializer(new ByteArrayCrLfSerializer());
```

#### Client
```java
TcpNetClientConnectionFactory connectionFactory = new TcpNetClientConnectionFactory(host, port);
connectionFactory.setSerializer(new ByteArrayCrLfSerializer());
connectionFactory.setDeserializer(new ByteArrayCrLfSerializer());
connectionFactory.setSingleUse(false); // Persistent connections
```

### Socket Options
- **SO_TIMEOUT**: Configurable read timeout (client: 5000ms default)
- **TCP_NODELAY**: Enabled (no Nagle algorithm delay)
- **SO_KEEPALIVE**: Platform default

## Example Communication Session

### Single Command
```
Client -> Server: "probe:shallow\r\n"
Server -> Client: "OK: Server is running\r\n"
```

### Multi-line Response
```
Client -> Server: "probe:deep\r\n"
Server -> Client: "DEEP_PROBE:\r\n"
                  "Hostname: myserver\r\n"
                  "IP: 192.168.1.100\r\n"
                  "Available Processors: 8\r\n"
                  "Free Memory: 512 MB\r\n"
                  "Total Memory: 1024 MB\r\n"
                  "Max Memory: 4096 MB\r\n"
```

### Multiple Commands (Persistent Connection)
```
Client -> Server: "mgt:time\r\n"
Server -> Client: "SERVER_TIME: 2026-06-19 10:06:43\r\n"

Client -> Server: "m: 5 + 10\r\n"
Server -> Client: "RESULT: 15\r\n"

Client -> Server: "probe:shallow\r\n"
Server -> Client: "OK: Server is running\r\n"

[Connection closes]
```

## Error Handling

### Network Errors
- **Connection Refused**: Server not running or wrong port
- **Connection Timeout**: Server not responding
- **Connection Reset**: Server closed connection unexpectedly

### Protocol Errors
- **Malformed Command**: Server returns error message
- **Unknown Command**: Server returns: `ERROR: Unknown command. Use probe:, mgt:, or m:`

### Response Format for Errors
```
ERROR: <error description>
```

## Testing with Command-Line Tools

### Using netcat (nc)
```bash
# Connect to server
nc localhost 5039

# Type commands and press Enter
probe:shallow
# Server responds: OK: Server is running

m: 5 + 10
# Server responds: RESULT: 15
```

### Using telnet
```bash
# Connect to server
telnet localhost 5039

# Type commands and press Enter
mgt:info
# Server responds with server information
```

## Security Considerations

**Note**: This is a basic implementation without authentication or encryption.

For production use, consider:
- TLS/SSL encryption
- Authentication mechanism
- Rate limiting
- Input validation and sanitization
- Network firewalls and access control
