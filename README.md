# TCP TGK - TCP Server and Client

A Spring Boot-based TCP server and client application with support for probe, management, and math commands.

## Features

- **Probe Commands**: Health checks (shallow and deep)
- **Management Commands**: Server info, time, and capabilities
- **Math Server**: Arithmetic operations (+, -, *, /, %, ^)
- **Java 17**: Modern Java features
- **Spring Boot**: Easy configuration and deployment
- **Maven**: Multi-module project structure

## Quick Start

### Using Build Scripts (Recommended)

```bash
# 1. Build the project
./bld

# 2. Start server (in one terminal)
./runS

# 3. Start client (in another terminal)
./runC
```

See [SCRIPTS.md](SCRIPTS.md) for more details on the build and run scripts.

### Using Maven Directly

#### Build
```bash
mvn clean install
```

#### Run Server
```bash
cd tcp-server
mvn spring-boot:run
```

#### Run Client
```bash
cd tcp-client
mvn spring-boot:run
```

## Project Structure

```
tcp-tgk-parent/
├── pom.xml                 # Parent POM
├── bld                     # Build script
├── runS                    # Run server script
├── runC                    # Run client script
├── tcp-server/             # Server module
│   ├── pom.xml
│   └── src/main/java/
├── tcp-client/             # Client module
│   ├── pom.xml
│   └── src/main/java/
└── docs/                   # Documentation
    ├── README.md           # Overview and setup
    ├── COMMANDS.md         # Command specifications
    ├── PROTOCOL.md         # TCP protocol details
    └── EXAMPLES.md         # Usage examples
```

## Documentation

- [SCRIPTS.md](SCRIPTS.md) - Build and run scripts guide
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [FINAL_CONFIG.md](FINAL_CONFIG.md) - Complete configuration reference

See the [docs](docs/) folder for detailed documentation:
- [README.md](docs/README.md) - Overview and setup
- [COMMANDS.md](docs/COMMANDS.md) - Command specifications
- [PROTOCOL.md](docs/PROTOCOL.md) - TCP protocol details
- [EXAMPLES.md](docs/EXAMPLES.md) - Usage examples
- [CONNECTION_MONITORING.md](docs/CONNECTION_MONITORING.md) - Connection monitoring feature

## Configuration

### Server
Default port: 5039 (configurable in `tcp-server/src/main/resources/application.properties`)

### Client
Default connection: localhost:5039 (configurable in `tcp-client/src/main/resources/application.properties`)

## Requirements

- Java 17+
- Maven 3.6+

## License

This is a sample project for demonstration purposes.
