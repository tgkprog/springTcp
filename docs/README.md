# TCP TGK - TCP Server and Client Application

## Overview
TCP TGK is a Spring Boot-based TCP server and client application that provides three types of command services: probe, management, and math operations.

## Architecture
- **Java Version**: 17
- **Framework**: Spring Boot 3.2.0
- **Protocol**: TCP
- **Default Port**: 5039

## Project Structure
```
tcp-tgk-parent/
├── tcp-server/          # TCP Server module
│   └── src/main/java/com/tcp/tgk/server/
│       ├── TcpServerApplication.java
│       ├── config/
│       │   └── TcpServerConfig.java
│       ├── handler/
│       │   └── CommandHandler.java
│       └── service/
│           ├── ProbeService.java
│           ├── ManagementService.java
│           └── MathService.java
│
└── tcp-client/          # TCP Client module
    └── src/main/java/com/tcp/tgk/client/
        ├── TcpClientApplication.java
        ├── config/
        │   └── TcpClientConfig.java
        └── service/
            └── TcpClientService.java
```

## Building the Project

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build Commands
```bash
# Build all modules
mvn clean install

# Build server only
cd tcp-server
mvn clean install

# Build client only
cd tcp-client
mvn clean install
```

## Running the Applications

### Start the Server
```bash
cd tcp-server
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar tcp-server/target/tcp-server-1.0.0.jar
```

The server will start on port 5039 (default) and wait for connections.

### Start the Client
```bash
cd tcp-client
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar tcp-client/target/tcp-client-1.0.0.jar
```

The client will start an interactive command-line interface.

## Configuration

### Server Configuration
Edit `tcp-server/src/main/resources/application.properties`:
```properties
tcp.server.port=5039
```

### Client Configuration
Edit `tcp-client/src/main/resources/application.properties`:
```properties
tcp.client.host=localhost
tcp.client.port=5039
```

## Next Steps
- See [COMMANDS.md](COMMANDS.md) for detailed command specifications
- See [PROTOCOL.md](PROTOCOL.md) for protocol details
- See [EXAMPLES.md](EXAMPLES.md) for usage examples
