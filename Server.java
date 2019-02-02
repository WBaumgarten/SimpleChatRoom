import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author 20289472
 */
public class Server {

    private static int uniqueID;
    private final int port;
    private boolean stillRunning;
    private static final String notif = " *** ";
    public static ArrayList<ClientThread> clientList;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;

    public Server(int port) {
        this.port = port;
        clientList = new ArrayList<>();
    }

    synchronized static ArrayList<ClientThread> getClientList() {
        return clientList;
    }

    public void start() {
        stillRunning = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (stillRunning) {
                System.out.println("Server waiting for clients on port " + port);
                
                Socket socket = serverSocket.accept();

                if (!stillRunning) {
                    break;
                }

                ClientThread clientThread = new ClientThread(socket, uniqueID++);

                boolean nameExists = false;
                for (ClientThread curClient : clientList) {
                    if (curClient.getUsername().equals(clientThread.getUsername())) {
                        nameExists = true;
                        break;
                    }
                }

                if (nameExists) {
                    clientThread.writeMsg("false");
                    clientThread.close();
                } else {
                    Server.broadcast(" *** " + clientThread.getUsername() + " has joined the chatroom. *** ");
                    clientList.add(clientThread);
                    clientThread.writeMsg("true");                    
                    clientThread.start();
                }

            }

            try {
                serverSocket.close();
                for (ClientThread curClient : clientList) {
                    curClient.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing the server and clients.");
            }

        } catch (IOException e) {
            System.err.println("Error while trying to create new server socket.");
        }
    }

    synchronized static void removeClient(int id) {
        String disconnectedClient = "";
        for (int i = 0; i < clientList.size(); i++) {
            ClientThread curClient = clientList.get(i);
            if (curClient.id == id) {
                disconnectedClient = curClient.getUsername();
                clientList.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " has lef the chatroom." + notif);
    }

    public static synchronized boolean broadcast(String message) {

        // to check if message is private i.e. client to client message
        String[] w = message.split(" ", 3);
        
        boolean isPrivate = false;
        if (w[1].charAt(0) == '@') {
            isPrivate = true;
        }

        // if private message, send message to mentioned username only
        if (isPrivate == true) {
            String sourceUser = w[0].substring(0, w[0].length() - 1);            
                    
            String tocheck = w[1].substring(1, w[1].length());
            int sent = 0;
            message = w[0] + w[2];
            String messageLf = message + "\n";
            boolean found = false;
            for (int y = clientList.size(); --y >= 0;) {
                ClientThread ct1 = clientList.get(y);
                                
                String check = ct1.getUsername();
                
                if (check.equals(sourceUser)) {
                    if (!ct1.writeMsg(messageLf)) {
                        clientList.remove(y);
                        System.out.println("Disconnected Client " + ct1.username + " removed from list.\n");
                    }
                    sent++;
                    if (sent == 2) {
                        break;
                    }
                }
                    
                
                if (check.equals(tocheck)) {
                    if (!ct1.writeMsg(messageLf)) {
                        clientList.remove(y);
                        System.out.println("Disconnected Client " + ct1.username + " removed from list.\n");
                    }
                    
                    found = true;
                    sent++;
                    if (sent == 2) {
                        break;
                    }
                }

            }
            // mentioned user not found, return false
            if (found != true) {
                return false;
            }
        } // if message is a broadcast message
        else {
            String messageLf = message + "\n";
            System.out.print(messageLf);
            // we loop in reverse order in case we would have to remove a Client
            // because it has disconnected
            for (int i = clientList.size(); --i >= 0;) {
                ClientThread ct = clientList.get(i);
                // try to write to the Client if it fails remove it from the list
                if (!ct.writeMsg(messageLf)) {
                    clientList.remove(i);
                    System.out.println("Disconnected Client " + ct.username + " removed from list.\n");
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        
        Server server = new Server(portNumber);
        server.start();
    }
}
