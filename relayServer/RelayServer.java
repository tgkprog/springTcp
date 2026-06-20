import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * A transparent TCP Relay Server (Pipe Server) that listens on a local port
 * and forwards all traffic bidirectionally to a remote VPS IP and port.
 * Built using Java 26 and Virtual Threads.
 */
public class RelayServer {
    private static final int BUFFER_SIZE = 8192;

    // Configurable parameters via environment variables
    private static final int connectTimeout = getEnvInt("RELAY_CONNECT_TIMEOUT_MS", 5000);
    private static final int readTimeout = getEnvInt("RELAY_READ_TIMEOUT_MS", 0); // 0 means infinite
    private static final int reconnectWait = getEnvInt("RELAY_RECONNECT_WAIT_MS", 1000);
    private static final int maxAttempts = getEnvInt("RELAY_MAX_ATTEMPTS", 3);

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java RelayServer <listen-port> <vps-ip> <vps-port>");
            System.err.println("Example: java RelayServer 5038 $ip2 5039");
            System.exit(1);
        }

        int listenPort;
        try {
            listenPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid listen-port: " + args[0]);
            System.exit(1);
            return;
        }

        String vpsHost = args[1];
        
        int vpsPort;
        try {
            vpsPort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid vps-port: " + args[2]);
            System.exit(1);
            return;
        }

        System.out.printf("[RELAY] Starting Relay Server...%n");
        System.out.printf("[RELAY] Listening locally on port: %d%n", listenPort);
        System.out.printf("[RELAY] Forwarding to remote VPS:  %s:%d%n", vpsHost, vpsPort);
        System.out.printf("[RELAY] Connect Timeout (VPS):    %d ms%n", connectTimeout);
        System.out.printf("[RELAY] Read Timeout:             %d ms (%s)%n", readTimeout, readTimeout == 0 ? "infinite" : "active");
        System.out.printf("[RELAY] Reconnect Wait (VPS):     %d ms%n", reconnectWait);
        System.out.printf("[RELAY] Max Connect Attempts:     %d%n", maxAttempts);
        System.out.println("[RELAY] Press Ctrl+C to terminate.");

        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            // Enable reusing local port to avoid "address already in use" errors during quick restarts
            serverSocket.setReuseAddress(true);

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.printf("[RELAY] Accepted client connection from %s%n", clientSocket.getRemoteSocketAddress());
                    
                    // Spawn a virtual thread to handle the accepted client session
                    Thread.startVirtualThread(() -> handleSession(clientSocket, vpsHost, vpsPort));
                } catch (IOException e) {
                    System.err.printf("[RELAY] Error accepting client connection: %s%n", e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.printf("[RELAY] Server socket error: %s%n", e.getMessage());
        }
    }

    private static void handleSession(Socket clientSocket, String vpsHost, int vpsPort) {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        
        try {
            // Set socket options for low latency and quick detection of drops
            clientSocket.setTcpNoDelay(true);
            clientSocket.setKeepAlive(true);
            if (readTimeout > 0) {
                clientSocket.setSoTimeout(readTimeout);
            }
        } catch (SocketException e) {
            System.err.printf("[RELAY][%s] Failed to set client socket options: %s%n", clientAddress, e.getMessage());
        }

        Socket vpsSocket = null;
        boolean connected = false;
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            vpsSocket = new Socket();
            try {
                System.out.printf("[RELAY][%s] Connecting to remote VPS %s:%d (attempt %d/%d)...%n", 
                    clientAddress, vpsHost, vpsPort, attempt, maxAttempts);
                
                vpsSocket.connect(new InetSocketAddress(vpsHost, vpsPort), connectTimeout);
                vpsSocket.setTcpNoDelay(true);
                vpsSocket.setKeepAlive(true);
                if (readTimeout > 0) {
                    vpsSocket.setSoTimeout(readTimeout);
                }
                connected = true;
                break;
            } catch (IOException e) {
                System.err.printf("[RELAY][%s] Connection attempt %d failed: %s%n", clientAddress, attempt, e.getMessage());
                closeQuietly(vpsSocket);
                vpsSocket = null;
                if (attempt < maxAttempts) {
                    try {
                        System.out.printf("[RELAY][%s] Waiting %dms before retrying...%n", clientAddress, reconnectWait);
                        Thread.sleep(reconnectWait);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!connected || vpsSocket == null) {
            System.err.printf("[RELAY][%s] Failed to establish connection to VPS after %d attempts.%n", clientAddress, maxAttempts);
            closeQuietly(clientSocket);
            return;
        }

        final Socket finalVpsSocket = vpsSocket;
        try {
            System.out.printf("[RELAY][%s] Bidirectional pipe established with remote VPS.%n", clientAddress);

            // Create two virtual threads for bidirectional data forwarding
            Thread clientToVpsThread = Thread.startVirtualThread(() -> pipe(clientSocket, finalVpsSocket, "Client -> VPS", clientAddress));
            Thread vpsToClientThread = Thread.startVirtualThread(() -> pipe(finalVpsSocket, clientSocket, "VPS -> Client", clientAddress));

            // Wait for both forwarding loops to complete
            clientToVpsThread.join();
            vpsToClientThread.join();

        } catch (InterruptedException e) {
            System.err.printf("[RELAY][%s] Session interrupted: %s%n", clientAddress, e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            closeQuietly(clientSocket);
            closeQuietly(finalVpsSocket);
            System.out.printf("[RELAY][%s] Connection session closed.%n", clientAddress);
        }
    }

    private static void pipe(Socket sourceSocket, Socket destSocket, String direction, String clientAddress) {
        try (InputStream in = sourceSocket.getInputStream();
             OutputStream out = destSocket.getOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            // Sockets closing or reset are common during network drops or disconnects
            System.out.printf("[RELAY][%s] %s pipe closed: %s%n", clientAddress, direction, e.getMessage());
        } finally {
            // Ensure the other socket is also closed to break the opposite loop
            closeQuietly(destSocket);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private static int getEnvInt(String name, int defaultValue) {
        String val = System.getenv(name);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                System.err.printf("[RELAY] Warning: Invalid value for env %s: %s. Using default: %d%n", name, val, defaultValue);
            }
        }
        return defaultValue;
    }
}
