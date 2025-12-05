import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    private String username;  // User's chat nickname
    private int port;  // Port number this node listens on
    private ConcurrentHashMap<InetSocketAddress, Socket> peerSockets = new ConcurrentHashMap<>(); // Active connections to peers
    private ConcurrentHashMap<InetSocketAddress, PrintWriter> peerWriters = new ConcurrentHashMap<>();  // Output streams for sending to peers
    private ConcurrentHashMap<InetSocketAddress, LocalDateTime> connectionTimes = new ConcurrentHashMap<>(); // Track when peers connected
    private volatile boolean running = true; // Controls thread execution (volatile for thread safety)
private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // ======================================== MESSAGE HISTORY STORAGE ========================================
    private List<String> messageHistory = new ArrayList<>();  // Stores all sent and received messages
    private static final int MAX_HISTORY_SIZE = 1000;  // Maximum number of messages to store
    
    public Node(String username, int port) {
        this.username = username;
        this.port = port;
    }

    // ======================================== ADD MESSAGE TO HISTORY ========================================
    /**
     * Adds a message to the local message history.
     * Stores both sent and received messages with timestamps.
     * 
     * @param message The message to add to history
     * @param isSent True if message was sent, false if received
     */
    private void addToHistory(String message, boolean isSent) {
String timestamp = LocalDateTime.now().format(timeFormatter);
        String direction = isSent ? "SENT" : "RECV";
        String historyEntry = String.format("[%s] [%s] %s", timestamp, direction, message);
        
        messageHistory.add(historyEntry);
        
        // Keep history size manageable
        if (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.remove(0);  // Remove oldest message
        }
    }

    // ======================================== 1. CONNECT TO OTHER PEERS ========================================
    /**
     * Establishes a persistent connection to another peer.
     * Called when user types: /connect <IP>:<PORT>
     * 
     * @param ip The IP address of the peer to connect to
     * @param port The port number of the peer to connect to
     */
    public void addPeer(String ip, int port) {
        InetSocketAddress peerAddress = new InetSocketAddress(ip, port);
        
        // Prevent duplicate connections to the same peer
        if (peerSockets.containsKey(peerAddress)) {
            System.out.println("Already connected to: " + ip + ":" + port);
            return;
        }

        try {
            System.out.println("Connecting to " + ip + ":" + port + "...");
            
            // Create client socket to connect to the peer
            Socket socket = new Socket(ip, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            // Store the connection for future communication
            peerSockets.put(peerAddress, socket);
            peerWriters.put(peerAddress, out);
            
            // Record connection time
            connectionTimes.put(peerAddress, LocalDateTime.now());
            
            // Notify the peer that we've connected
            out.println("*** " + username + " has connected ***");
            
            // Add connection message to history
            addToHistory("Connected to " + ip + ":" + port, true);
            
            // Start a dedicated thread to listen for messages from this peer
            new Thread(() -> listenToPeer(socket, peerAddress)).start();
            
            System.out.println("Connected to: " + ip + ":" + port);
            
        } catch (IOException e) {
            System.out.println("Failed to connect to " + ip + ":" + port);
        }
    }

    // ======================================== 2. RECEIVE MESSAGES FROM PEERS ========================================
    /**
     * Listens for incoming messages from a specific peer.
     * Runs in a separate thread for each connected peer.
     * 
     * @param socket The socket connection to the peer
     * @param peerAddress The address of the peer
     */
    private void listenToPeer(Socket socket, InetSocketAddress peerAddress) {
        try {
            // Create input stream reader for the socket
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            
            // Continuously read messages until connection closes or node stops
            while (running && (message = in.readLine()) != null) {
                // Add to history (RECEIVED message)
                addToHistory(message, false);
                
                // Display received message
                System.out.println("\n" + message);
                // Re-display prompt for user input
                System.out.print(username + "> ");
            }
            
        } catch (IOException e) {
            // Connection was closed (normal when peer disconnects)
        } finally {
            // Clean up when connection ends
            removePeer(peerAddress);
            System.out.println("\nPeer disconnected: " + peerAddress);
            System.out.print(username + "> ");
        }
    }

    // ======================================== 3. MANAGE PEER CONNECTIONS ========================================
    /**
     * Removes a peer from the connection list and closes the socket.
     * Called when a peer disconnects or connection fails.
     * 
     * @param peerAddress The address of the peer to remove
     */
    private void removePeer(InetSocketAddress peerAddress) {
        try {
            // Close the socket if it exists
            if (peerSockets.containsKey(peerAddress)) {
                peerSockets.get(peerAddress).close();
            }
        } catch (IOException e) {
            // Ignore errors during cleanup
        }
        // Remove from connection tracking maps
        peerSockets.remove(peerAddress);
        peerWriters.remove(peerAddress);
        // Also remove connection time record
        connectionTimes.remove(peerAddress);
    }

    // ======================================== 4. SEND MESSAGES TO ALL PEERS ========================================
    /**
     * Broadcasts a message to all connected peers.
     * Called when user types a regular message (not a command).
     * 
     * @param message The message text to broadcast
     */
    public void sendMessage(String message) {
        // Ignore empty messages
        if (message.trim().isEmpty()) {
            return;
        }

        // Check if there are any peers to send to
        if (peerWriters.isEmpty()) {
            System.out.println("\n? No peers connected to broadcast");
            System.out.print(username + "> ");
            return;
        }

        // Format message with username
        String msgToSend = "[" + username + "]: " + message;
        
        // Add to history (SENT message)
        addToHistory(msgToSend, true);
        
        // Display the message locally (echo)
        System.out.println("\n" + msgToSend);
        
        // Send to all connected peers
        for (PrintWriter out : peerWriters.values()) {
            out.println(msgToSend);
        }
        
        // Re-display prompt
        System.out.print(username + "> ");
    }

    // ======================================== 5. ACCEPT INCOMING CONNECTIONS ========================================
    /**
     * Starts a server socket to accept incoming connections from other peers.
     * Runs in a separate thread to avoid blocking user input.
     */
    public void startListening() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println(username + " is listening on port " + port);
                
                // Continuously accept new connections
                while (running) {
                    // This blocks until a peer connects
                    Socket socket = serverSocket.accept();
                    
                    // Get the address of the connecting peer
                    InetSocketAddress peerAddress = 
                        new InetSocketAddress(socket.getInetAddress().getHostAddress(), socket.getPort());
                    
                    // Store the connection
                    peerSockets.put(peerAddress, socket);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    peerWriters.put(peerAddress, out);
                    
                    // Record connection time for incoming peers
                    connectionTimes.put(peerAddress, LocalDateTime.now());
                    
                    // Add connection message to history
                    addToHistory("New connection from " + peerAddress.getHostName(), false);
                    
                    // Start listening for messages from this new peer
                    new Thread(() -> listenToPeer(socket, peerAddress)).start();
                    
                    // Notify user of new connection
                    System.out.println("\nNew connection from: " + peerAddress.getHostName());
                    System.out.print(username + "> ");
                }
                
            } catch (IOException e) {
                System.out.println("Error listening on port " + port);
            }
        }).start();
    }

    // ======================================== 6. SHOW CONNECTED PEERS ========================================
    /**
     * Shows all currently connected peers with their connection status and duration.
     * Called when user types: /list
     */
    public void showConnectedPeers() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("CONNECTED PEERS LIST");
        System.out.println("=".repeat(50));
        
        if (peerSockets.isEmpty()) {
            System.out.println("No peers connected.");
        } else {
            int count = 1;
            LocalDateTime now = LocalDateTime.now();
            
            for (InetSocketAddress peerAddress : peerSockets.keySet()) {
                Socket socket = peerSockets.get(peerAddress);
                LocalDateTime connectTime = connectionTimes.get(peerAddress);
                
                // Calculate connection duration
                String duration = "Unknown";
                if (connectTime != null) {
                    try {
                        Duration connectionDuration = Duration.between(connectTime, now);
                        long minutes = connectionDuration.toMinutes();
                        long seconds = connectionDuration.getSeconds() % 60;
                        duration = String.format("%02d:%02d", minutes, seconds);
                    } catch (Exception e) {
                        duration = "Error";
                    }
                }
                
                // Determine connection status with emojis
                String status = "✅ Online";
                if (socket.isClosed()) {
                    status = "❌ offline";
                } else if (!socket.isConnected()) {
                    status = "🟡 Lost";
                }
                
                // Display peer information in formatted columns
                System.out.printf("%2d. %-25s %-20s %-10s%n",
                    count++,
                    peerAddress.getHostName() + ":" + peerAddress.getPort(),
                    "Connected for " + duration,
                    status);
            }
            System.out.println("-".repeat(50));
            System.out.println("Total peers: " + peerSockets.size());
        }
        
        System.out.print(username + "> ");
    }

    // ======================================== 7. SHOW MESSAGE HISTORY ========================================
    /**
     * Displays the local message history (sent and received messages).
     * Called when user types: /history
     */
    public void showMessageHistory() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("MESSAGE HISTORY (Last " + messageHistory.size() + " messages)");
        System.out.println("=".repeat(70));
        
        if (messageHistory.isEmpty()) {
            System.out.println("No messages yet. Start chatting!");
        } else {
            // Show last 20 messages by default (or adjust as needed)
            int startIndex = Math.max(0, messageHistory.size() - 20);
            
            for (int i = startIndex; i < messageHistory.size(); i++) {
                String message = messageHistory.get(i);
                System.out.println(message);
            }
            
            System.out.println("-".repeat(70));
            System.out.println("Showing " + (messageHistory.size() - startIndex) + " of " + messageHistory.size() + " total messages");
        }
        
        System.out.print(username + "> ");
    }

    // ======================================== 8. CLEAR MESSAGE HISTORY ========================================
    /**
     * Clears the local message history.
     * Called when user types: /clearhistory
     */
    public void clearMessageHistory() {
        messageHistory.clear();
        System.out.println("\nMessage history cleared.");
        System.out.print(username + "> ");
    }

    // ======================================== 9. SAVE HISTORY TO FILE ========================================
    /**
     * Saves the message history to a file.
     * Called when user types: /savehistory <filename>
     * 
     * @param filename The name of the file to save to
     */
    public void saveHistoryToFile(String filename) {
        try (PrintWriter fileWriter = new PrintWriter(new FileWriter(filename))) {
            fileWriter.println("Chat History for: " + username);
            fileWriter.println("Saved on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            fileWriter.println("=".repeat(50));
            
            for (String message : messageHistory) {
                fileWriter.println(message);
            }
            
            System.out.println("\nHistory saved to: " + filename + " (" + messageHistory.size() + " messages)");
        } catch (IOException e) {
            System.out.println("\nError saving history: " + e.getMessage());
        }
        System.out.print(username + "> ");
    }

    // ======================================== 10. CLEAN SHUTDOWN ========================================
    /**
     * Gracefully shuts down all connections and stops listening threads.
     * Should be called before exiting the application.
     */
    public void shutdown() {
        running = false; // Signal threads to stop
        for (Socket socket : peerSockets.values()) {
            try {
                socket.close(); // Close all peer connections
            } catch (IOException e) {
                // Ignore errors during shutdown
            }
        }
    }
} 
