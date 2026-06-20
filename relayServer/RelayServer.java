import java.io.*;
import java.net.*;

/**
 * A transparent TCP Relay Server (Pipe Server) that listens on a local port
 * and forwards all traffic bidirectionally to a remote VPS IP and port.
 * Built using Java 26 and Virtual Threads.
 */
public class RelayServer {
    private static final int BUFFER_SIZE = 8192;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java RelayServer <listen-port> <vps-ip> <vps-port>");
            System.err.println("Example: java RelayServer 5038 38.242.235.170 5039");
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
        
        try (Socket vpsSocket = new Socket()) {
            // Set socket options for low latency and quick detection of drops
            clientSocket.setTcpNoDelay(true);
            clientSocket.setKeepAlive(true);

            System.out.printf("[RELAY][%s] Connecting to remote VPS %s:%d...%n", clientAddress, vpsHost, vpsPort);
            // Connect to VPS with a timeout of 5 seconds
            vpsSocket.connect(new InetSocketAddress(vpsHost, vpsPort), 5000);
            vpsSocket.setTcpNoDelay(true);
            vpsSocket.setKeepAlive(true);
            System.out.printf("[RELAY][%s] Bidirectional pipe established with remote VPS.%n", clientAddress);

            // Create two virtual threads for bidirectional data forwarding
            Thread clientToVpsThread = Thread.startVirtualThread(() -> pipe(clientSocket, vpsSocket, "Client -> VPS", clientAddress));
            Thread vpsToClientThread = Thread.startVirtualThread(() -> pipe(vpsSocket, clientSocket, "VPS -> Client", clientAddress));

            // Wait for both forwarding loops to complete
            clientToVpsThread.join();
            vpsToClientThread.join();

        } catch (IOException e) {
            System.err.printf("[RELAY][%s] Connection setup failed: %s%n", clientAddress, e.getMessage());
        } catch (InterruptedException e) {
            System.err.printf("[RELAY][%s] Session interrupted: %s%n", clientAddress, e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            closeQuietly(clientSocket);
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
}
