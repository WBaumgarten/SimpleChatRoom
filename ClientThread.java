import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientThread extends Thread {

    Socket socket;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    int id;
    String username;
    ChatMessage chatMsg;

    public ClientThread(Socket socket, int uniqueID) {
        id = uniqueID;
        this.socket = socket;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            username = (String) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Exception creating new Input/Output streams: " + e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void run() {
        boolean stillRunning = true;

        while (stillRunning) {
            try {
                chatMsg = (ChatMessage) inputStream.readObject();
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                System.err.println(username + "Class not found: " + e);
                break;
            }

            String msg = chatMsg.getMessage();

            switch (chatMsg.getType()) {
                case ChatMessage.MESSAGE:
                    boolean confirmation = Server.broadcast(username + ": " + msg);
                    if (confirmation == false) {
                        String errMsg = " *** Sorry. No such user exists. *** ";
                        writeMsg(errMsg);
                    }
                    break;

                case ChatMessage.LOGOUT:
                    stillRunning = false;
                    break;

                case ChatMessage.WHOISIN:
                    writeMsg("List of users connected: \n");
                    int i = 1;
                    for (ClientThread curClient : Server.clientList) {
                        writeMsg("- " + curClient.username);
                    }
                    break;
            }

        }
        Server.removeClient(id);
        close();
    }

    public boolean writeMsg(String msg) {
        if (!socket.isConnected()) {
            close();
            return false;
        }

        try {
            outputStream.writeObject(msg);
        } catch (IOException e) {
            System.err.println("Error sending message to " + username + "\n" + e.toString());
        }
        return true;
    }

    public void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }

        try {
            if (inputStream != null) {
                inputStream.close();
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

}
