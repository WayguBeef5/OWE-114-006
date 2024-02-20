package Module4.Part3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

    Socket server = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    final String ipAddressPattern = "connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})";
    final String localhostPattern = "connect\\s+(localhost:\\d{3,5})";
    boolean isRunning = false;
    private Thread inputThread;
    private Thread fromServerThread;
    private boolean isGuessing = false;

    public Client() {
        System.out.println("");
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            System.out.println("Client connected");
            listenForServerMessage();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    private boolean isConnection(String text) {
        return text.matches(ipAddressPattern) || text.matches(localhostPattern);
    }

    private boolean isQuit(String text) {
        return text.equalsIgnoreCase("quit");
    }

    private boolean processCommand(String text) {
        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            return true;
        } else if (isQuit(text)) {
            isRunning = false;
            return true;
        } else if (processNumberGuesser(text)) {
            return true;
        }
        return false;
    }

    private void listenForKeyboard() {
        inputThread = new Thread() {
            @Override
            public void run() {
                System.out.println("Listening for input");
                try (Scanner si = new Scanner(System.in)) {
                    String line = "";
                    isRunning = true;
                    while (isRunning) {
                        try {
                            System.out.println("Waiting for input");
                            line = si.nextLine();
                            if (!processCommand(line)) {
                                if (isConnected()) {
                                    out.writeObject(line);
                                } else {
                                    System.out.println("Not connected to the server");
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Connection dropped");
                            break;
                        }
                    }
                    System.out.println("Exited loop");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close();
                }
            }
        };
        inputThread.start();
    }

    private void startNumberGuesser() {
        isGuessing = true;
        System.out.println("Number guesser activated. Make your guess with '/guess <number>'.");
    }

    private void stopNumberGuesser() {
        isGuessing = false;
        System.out.println("Number guesser deactivated. Guesses will be treated as regular messages.");
    }

    private boolean processNumberGuesser(String text) {
        if (text.startsWith("/start")) {
            startNumberGuesser();
            return true;
        } else if (text.startsWith("/stop")) {
            stopNumberGuesser();
            return true;
        } else if (isGuessing && text.startsWith("/guess")) {
            try {
                int guessedNumber = Integer.parseInt(text.split(" ")[1]);
                System.out.println("Guess received: " + guessedNumber);
                // Add logic to handle the guess (compare with the actual number, provide feedback)
                // For now, it simply prints the guessed number.
                return true;
            } catch (NumberFormatException e) {
                System.out.println("Invalid guess format. Use '/guess <number>'.");
            }
        }
        return false;
    }

    private void listenForServerMessage() {
        fromServerThread = new Thread() {
            @Override
            public void run() {
                try {
                    String fromServer;
                    while (!server.isClosed() && !server.isInputShutdown()
                            && (fromServer = (String) in.readObject().toString()) != null) {
                        System.out.println(fromServer);
                    }
                    System.out.println("Loop exited");
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!server.isClosed()) {
                        System.out.println("Server closed the connection");
                    } else {
                        System.out.println("Connection closed");
                    }
                } finally {
                    close();
                    System.out.println("Stopped listening to server input");
                }
            }
        };
        fromServerThread.start();
    }

    public void start() throws IOException {
        listenForKeyboard();
    }

    private void close() {
        try {
            inputThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting input");
            e.printStackTrace();
        }
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting listener");
            e.printStackTrace();
        }
        try {
            System.out.println("Closing output stream");
            out.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened, so this exception is okay");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing input stream");
            in.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened, so this exception is okay");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing connection");
            server.close();
            System.out.println("Closed socket");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened, so this exception is okay");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
