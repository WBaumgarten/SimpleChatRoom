import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    private final String notif = " *** ";
    private ObjectOutputStream clientOut;
    private ObjectInputStream clientIn;
    private Socket socket;
    private final String server;
    private String username;
    private final int port;

    public String getUsername() {
        return username;
    }

    public void setUsername(String name) {
        this.username = name;
    }

    public boolean checkConnection() {
        return socket.isConnected();
    }

    public Client(String server, int port, String username) {
        this.server = server;
        this.username = username;
        this.port = port;
    }

    public boolean start() {
        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            ClientUI.printToOut(notif + "Error connecting to server on port number: "+port +"\nTry checking if the port number is correct." + notif);
            return false;
        }

        try {
            clientIn = new ObjectInputStream(socket.getInputStream());
            clientOut = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            ClientUI.printToOut(notif + "Exception creating new Input/output Streams" + notif);
            return false;
        }

        try {
            clientOut.writeObject(username);
        } catch (IOException e) {
            ClientUI.printToOut(notif + "Error during login" + notif);
            disconnect();
            return false;
        }

        return true;
    }

    public void startListeningFromServer() {
        new ListenFromServer().start();
    }

    public void sendMessage(ChatMessage msg) {
        try {
            clientOut.writeObject(msg);
        } catch (IOException e) {
            ClientUI.printToOut(notif + "Exception writing to server." + notif);
        }
    }

    public String readMessage() {
        try {
            String msg = (String) clientIn.readObject();
            return msg;
        } catch (IOException | ClassNotFoundException ex) {
            return "Error reading in messages.";
        }
    }

    /*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect
     */
    public void disconnect() {
        
        sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
        try {
            if (clientIn != null) {
                clientIn.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        try {
            if (clientOut != null) {
                clientOut.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }

    }

    /*
	 * a class that waits for the message from the server
     */
    class ListenFromServer extends Thread {

        // add mutex locks for threads to have sole acces to sending stream
        @Override
        public void run() {
            while (true) {
                try {
                    // read the message form the input datastream
                    String msg = (String) clientIn.readObject();
                    ClientUI.printToOut(msg);
                } catch (IOException e) {
                    ClientUI.printToOut(notif + "Server has closed the connection." + notif);
                    break;
                } catch (ClassNotFoundException e2) {
                    System.err.println(e2);
                }
            }
        }
    }

}
