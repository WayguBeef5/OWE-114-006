package Module4.Part3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket client;
    private boolean isRunning = false;
    private ObjectOutputStream out;
    private Server server;

    public ServerThread(Socket myClient, Server server) {
        this.client = myClient;
        this.server = server;
    }

    public void disconnect() {
        isRunning = false;
        cleanup();
    }

    public boolean send(String message) {
        try {
            out.writeObject(message);
            return true;
        } catch (IOException e) {
            System.out.println("Error sending message to client (most likely disconnected)");
            cleanup();
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {
            this.out = out;
            isRunning = true;
            String fromClient;
            while (isRunning && (fromClient = (String) in.readObject()) != null) {
                server.broadcast(fromClient, this.getId());
            }
        } catch (Exception e) {
            System.out.println("Client disconnected");
        } finally {
            isRunning = false;
            cleanup();
        }
    }

    private void cleanup() {
        try {
            client.close();
        } catch (IOException e) {
            System.out.println("Client already closed");
        }
    }
}
