# TCP TGK

A Spring Integration TCP server / client pair with an optional Swing UI and a relay server for network-namespace testing.

---

## Quick Start

### 1. Build

```bash
./bld
```

Runs `mvn clean package -DskipTests` and produces:

```
tcp-server/target/tcp-server-1.0.0.jar
tcp-client/target/tcp-client-1.0.0.jar
```

### 2. Start the server

```bash
./runS
```

Listens on **port 5039** by default.

### 3. Start the client

```bash
# Swing UI mode
./runC

# Terminal (CLI) mode
./runC-terminal
```

---

## Environment Variables

The run scripts read these shell variables before starting the JVM.  
Set them in your `~/.bashrc`, `~/.profile`, or export them inline.

### `ip2` — remote server IP

`ip2` is a convenience shell variable that holds the IP address of the machine running the TCP server (e.g. a VPS or a second machine on the LAN).  
Both `runC` and `runR` fall back to `$ip2` when `TCP_CLIENT_HOST` is not set.

```bash
# In ~/.bashrc or ~/.profile
export ip2=192.168.1.50          # LAN example
# export ip2=203.0.113.10        # VPS / remote example
```

### Client variables (`runC` / `runC-terminal`)

| Variable | Default | Description |
|---|---|---|
| `TCP_CLIENT_HOST` | `$ip2` | IP/hostname of the TCP server |
| `TCP_CLIENT_PORT` | `5039` | Port of the TCP server |
| `TCP_CLIENT_TIMEOUT` | `147` (terminal) / `250` (UI) | Connection timeout in ms |
| `TCP_CLIENT_RETRY_WAIT` | `30` (terminal) / `270` (UI) | Wait between reconnect attempts (ms) |
| `TCP_CLIENT_MODE` | `terminal` or `ui` | `terminal`, `ui`, or `both` |

#### Inline override example

```bash
ip2=10.0.0.5 ./runC-terminal
# or
TCP_CLIENT_HOST=10.0.0.5 TCP_CLIENT_PORT=5039 ./runC-terminal
```

### Relay server variables (`runR`)

```bash
./runR [LISTEN_PORT] [VPS_IP] [VPS_PORT]
# defaults: 5038  $ip2  5039
```

| Variable / Arg | Default | Description |
|---|---|---|
| `LISTEN_PORT` (arg 1) | `5038` | Local port the relay listens on |
| `VPS_IP` (arg 2) | `$ip2` | Target server IP |
| `VPS_PORT` (arg 3) | `5039` | Target server port |
| `RELAY_CONNECT_TIMEOUT_MS` | `250` | TCP connect timeout (ms) |
| `RELAY_READ_TIMEOUT_MS` | `230` | Inactivity read timeout ms (`0` = infinite) |
| `RELAY_RECONNECT_WAIT_MS` | `270` | Wait between relay reconnect attempts (ms) |
| `RELAY_MAX_ATTEMPTS` | `9999` | Max reconnect attempts |

```bash
# Run relay inside its own network namespace
sudo ip netns exec netns_relay ./runR 5038 203.0.113.10 5039
```

---

## Commands & Sample Output

Once the terminal client is running you get a `>` prompt.
Type any command and press **Enter**.

### `fast` — quick health check

```
> fast
OK: Server is running
```

### `deep` — detailed system info

```
> deep
DEEP_PROBE:
Hostname: myserver
IP: 192.168.1.50
Available Processors: 4
Free Memory: 312 MB
Total Memory: 512 MB
Max Memory: 1024 MB
```

### `info` — server information

```
> info
SERVER_INFO:
Name: TCP TGK Server
Version: 1.0.0
Java Version: 21.0.3
OS: Linux 6.5.0
IP Addresses:
  - 127.0.0.1
  - 192.168.1.50
```

### `time` — current server time

```
> time
SERVER_TIME: 2026-06-21 16:20:05
```

### `help` — list all capabilities

```
> help
CAPABILITIES:
1. Probe Commands:
   - fast - Basic server health check
   - deep - Detailed system information
2. Management Commands:
   - info - Server information
   - time - Current server time
   - help - List all capabilities
3. Math Server:
   - m <expression> - Calculate arithmetic expressions
   - Supported operations: +, -, *, /, %, ^ (power)
```

### `m <num> <op> <num>` — math

Spaces between tokens are flexible — zero, one, or many spaces all work.

```
> m 5 + 10
RESULT: 15

> m 2 ^ 8
RESULT: 256

> m 10 / 3
RESULT: 3.3333333333333335

> m 10 / 0
ERROR: Division by zero

> m 7*6
RESULT: 42
```

### Built-in client commands (terminal mode only)

| Command | Output |
|---|---|
| `status` | `Connection Status: CONNECTED` |
| `state` | Detailed state-machine info |
| `exit` | Shuts the client down |

---

## Monitoring

Pass `--tcp.client.monitoring.enabled=true` to run periodic `fast` probes in the background.  
`--tcp.client.monitoring.tick-ms=<ms>` controls the interval (default 2000 ms).

Monitoring logs go to `logs/client-check.log`; application logs go to `logs/tcp-client.log`.

---

## Project Layout

```
tcp-tgk/
├── bld                  # Maven build script
├── runS                 # Start server
├── runC                 # Start client (Swing UI)
├── runC-terminal        # Start client (terminal / CLI)
├── runC2                # Start client inside netns_client namespace
├── runR                 # Start relay server
├── tcp-server/          # Spring Boot TCP server
│   └── src/main/java/…/
│       ├── handler/CommandHandler.java
│       └── service/{ProbeService,ManagementService,MathService}.java
├── tcp-client/          # Spring Boot TCP client
│   └── src/main/java/…/
│       ├── shell/TerminalRunner.java
│       ├── ui/SwingUiFrame.java
│       └── service/TcpClientService.java
└── relayServer/         # Plain-Java TCP relay (no Spring)
```

---

## License

```
Copyright 2026 Tushar Kapila — tgkprog — sel2in.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.
```
