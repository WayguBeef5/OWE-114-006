package Module4.Part3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unused")
public class Server {
    private int port = 3001;
    private List<ServerThread> clients = new ArrayList<>();

    private boolean isNumberGuesserActive = false;
    private int hiddenNumber;

    public Server() {
        // Initialize other variables or settings here
    }

    private void start(int port) {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket incomingClient = serverSocket.accept();
                System.out.println("Client connected");
                ServerThread sClient = new ServerThread(incomingClient, this);
                clients.add(sClient);
                sClient.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("Closing server socket");
        }
    }

    // ... (rest of your code)

    public static void main(String[] args) {
        System.out.println("Starting Server");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }

    /**
     * @param fromClient
     * @param id
     */
    public void broadcast(String fromClient, long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'broadcast'");
    }
}
