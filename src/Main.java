import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = sc.nextLine();

        System.out.print("Enter your port number (>1024): ");
        int port = Integer.parseInt(sc.nextLine());

        Node node = new Node(username, port);
        node.startListening();

        // Small delay to ensure server is listening
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        System.out.println("\n" + "=".repeat(60));
        System.out.println("P2P CHAT SYSTEM - " + username + " on port " + port);
        System.out.println("=".repeat(60));
        System.out.println("\nAvailable Commands:");
        System.out.println("  /connect <IP>:<PORT>    - Connect to a peer");
        System.out.println("  /list                   - Show all connected peers");
        System.out.println("  /history                - Show message history");
        System.out.println("  /clearhistory           - Clear message history");
        System.out.println("  /savehistory <file>     - Save history to file");
        System.out.println("  /quit                   - Exit the chat");
        System.out.println("\n  Type any message to broadcast to all peers");
        System.out.println("-".repeat(60));

        while (true) {
            System.out.print(username + "> ");
            String input = sc.nextLine();

            if (input.equalsIgnoreCase("/quit")) {
                System.out.println("Goodbye! Shutting down...");
                node.shutdown();
                break;
            } else if (input.equalsIgnoreCase("/list")) {
                node.showConnectedPeers();
            } else if (input.equalsIgnoreCase("/history")) {
                node.showMessageHistory();
            } else if (input.equalsIgnoreCase("/clearhistory")) {
                node.clearMessageHistory();
            } else if (input.startsWith("/savehistory")) {
                String[] parts = input.split(" ");
                if (parts.length == 2) {
                    node.saveHistoryToFile(parts[1]);
                } else {
                    System.out.println("Usage: /savehistory chat_history/<filename>");
                    System.out.print(username + "> ");
                }
            } else if (input.startsWith("/connect")) {
                try {
                    String[] parts = input.split(" ");
                    if (parts.length == 2) {
                        String[] address = parts[1].split(":");
                        if (address.length == 2) {
                            node.addPeer(address[0], Integer.parseInt(address[1]));
                        } else {
                            System.out.println("Invalid format. Use: /connect IP:PORT");
                            System.out.print(username + "> ");
                        }
                    } else {
                        System.out.println("Invalid format. Use: /connect IP:PORT");
                        System.out.print(username + "> ");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    System.out.print(username + "> ");
                }
            } else {
                node.sendMessage(input);
            }
        }

        sc.close();
        System.exit(0);
    }
}