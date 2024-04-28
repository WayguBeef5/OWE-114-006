package Project.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.RoomResultsPayload;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

public enum Client {
    INSTANCE;

    Socket server = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    final String ipAddressPattern = "/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})";
    final String localhostPattern = "/connect\\s+(localhost:\\d{3,5})";
    boolean isRunning = false;
    private Thread inputThread;
    private Thread fromServerThread;
    private String clientName = "";

    private static final String CREATE_ROOM = "/createroom";
    private static final String JOIN_ROOM = "/joinroom";
    private static final String LIST_ROOMS = "/listrooms";
    private static final String LIST_USERS = "/users";
    private static final String DISCONNECT = "/disconnect";

    // Constants for new commands
    private static final String HELLO_COMMAND = "/hello";
    private static final String FLIP_COMMAND = "/flip";
    private static final String ROLL_COMMAND = "/roll";

    // client id, is the key, client name is the value
    private ConcurrentHashMap<Long, String> clientsInRoom = new ConcurrentHashMap<Long, String>();
    private long myClientId = Constants.DEFAULT_CLIENT_ID;
    private Logger logger = Logger.getLogger(Client.class.getName());

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
         // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine
        // if the server had a problem
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }
    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            logger.info("Client connected");
            listenForServerMessage();
            sendConnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }
    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an ip address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return
     */

    private boolean isConnection(String text) {
         // https://www.w3schools.com/java/java_regex.asp

        return text.matches(ipAddressPattern) || text.matches(localhostPattern);
    }

    private boolean isQuit(String text) {
        return text.equalsIgnoreCase("/quit");
    }

    private boolean isName(String text) {
        if (text.startsWith("/name")) {
            String[] parts = text.split(" ");
            if (parts.length >= 2) {
                clientName = parts[1].trim();
                logger.info("Name set to " + clientName);
            }
            return true;
        }
        return false;
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if a text was a command or triggered a command
     */

    private boolean processClientCommand(String text) throws IOException {
        if (isConnection(text)) {
            if (clientName.isBlank()) {
                logger.warning("You must set your name before you can connect via: /name your_name");
                return true;
            }
             // replaces multiple spaces with single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            return true;
        } else if (isQuit(text)) {
            isRunning = false;
            return true;
        } else if (isName(text)) {
            return true;
        } else if (text.startsWith(CREATE_ROOM)) {

            try {
                String roomName = text.replace(CREATE_ROOM, "").trim();
                sendCreateRoom(roomName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.startsWith(JOIN_ROOM)) {

            try {
                String roomName = text.replace(JOIN_ROOM, "").trim();
                sendJoinRoom(roomName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.startsWith(LIST_ROOMS)) {

            try {
                String searchQuery = text.replace(LIST_ROOMS, "").trim();
                sendListRooms(searchQuery);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.equalsIgnoreCase(LIST_USERS)) {
            logger.info("Users in Room: ");
            clientsInRoom.forEach(((t, u) -> {
                logger.info(String.format("%s - %s", t, u));
            }));
            return true;
        } else if (text.equalsIgnoreCase(DISCONNECT)) {
            try {
                sendDisconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } 
        // New Code Begins where i added everything asked in the instructions such as the flip, roll, and hello command.
        // owe
        // 4-28-24
        
        else if (text.equalsIgnoreCase(HELLO_COMMAND)) {
            try {
                sendHello();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.equalsIgnoreCase(FLIP_COMMAND)) {
            try {
                sendFlip();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.startsWith(ROLL_COMMAND)) {
            String rollString = text.replace("/roll", "").trim();
            try {
                int result = roll(rollString);
                sendRoll(result);
            } catch (IOException e) {
                sendMessage("Wrong Format. Use : '/roll 123' or '/roll 456'.");
            }
            return true;
        }
        return false;
    }
     // Send methods

    private void sendDisconnect() throws IOException {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.DISCONNECT);
        out.writeObject(cp);
    }

    private void sendCreateRoom(String roomName) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CREATE_ROOM);
        p.setMessage(roomName);
        out.writeObject(p);
    }

    private void sendJoinRoom(String roomName) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.JOIN_ROOM);
        p.setMessage(roomName);
        out.writeObject(p);
    }

    private void sendListRooms(String searchString) throws IOException {
        // Updated after video to use RoomResultsPayload so we can (later) use a limit
        // value
        RoomResultsPayload p = new RoomResultsPayload();
        p.setMessage(searchString);
        p.setLimit(10);
        out.writeObject(p);
    }

    private void sendConnect() throws IOException {
        ConnectionPayload p = new ConnectionPayload(true);
        p.setClientName(clientName);
        out.writeObject(p);
    }

    private void sendMessage(String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
         // no need to send an identifier, because the server knows who we are
        // p.setClientName(clientName);
        out.writeObject(p);
    }

     // end send methods

    private boolean processCommand(String text) throws IOException {
        if (text.equalsIgnoreCase(HELLO_COMMAND)) {
            try {
                sendHello();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.equalsIgnoreCase(FLIP_COMMAND)) {
            try {
                sendFlip();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (text.startsWith(ROLL_COMMAND)) {
            String rollString = text.replace("/roll", "").trim();
            try {
                int result = roll(rollString);
                sendRoll(result);
            } catch (IOException e) {
                sendMessage("Wrong Format. Use : '/roll x' or '/roll xdy'.");
            }
            return true;
        }
        return false;
    }

    private void sendHello() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage("Hello from the client!");
        out.writeObject(p);
    }

    private void sendFlip() throws IOException {
        String result = Math.random() < 0.5 ? "Heads" : "Tails";
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage("Coin flip result: " + result);
        out.writeObject(p);
    }

    private int roll(String rollString) {
        return 0;
    }

    private void sendRoll(int result) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage("Dice roll result: " + result);
        out.writeObject(p);
    }

    //  New code ends here with the implementions has some of the existing code prof gave us 
    //  owe
    // 4/28/2024


    private void listenForKeyboard() {
        inputThread = new Thread() {
            @Override
            public void run() {
                logger.info("Listening for input");
                try (Scanner si = new Scanner(System.in);) {
                    String line = "";
                    isRunning = true;
                    while (isRunning) {
                        try {
                            logger.info("Waiting for input");
                            line = si.nextLine();
                            if (!processClientCommand(line) && !processCommand(line)) {
                                if (isConnected()) {
                                    if (line != null && line.trim().length() > 0) {
                                        sendMessage(line);
                                    }
                                } else {
                                    logger.warning("Not connected to server");
                                }
                            }
                        } catch (Exception e) {
                            logger.severe("Connection dropped");
                            break;
                        }
                    }
                    logger.info("Exited loop");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close();
                }
            }
        };
        inputThread.start();
    }

    private void listenForServerMessage() {
        fromServerThread = new Thread() {
            @Override
            public void run() {
                try {
                    Payload fromServer;
                    while (!server.isClosed() && !server.isInputShutdown()
                            && (fromServer = (Payload) in.readObject()) != null) {
                        logger.info("Debug Info: " + fromServer);
                        processPayload(fromServer);
                    }
                    logger.info("Loop exited");
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!server.isClosed()) {
                        logger.severe("Server closed connection");
                    } else {
                        logger.severe("Connection closed");
                    }
                } finally {
                    close();
                    logger.info("Stopped listening to server input");
                }
            }
        };
        fromServerThread.start();
    }

    private void addClientReference(long id, String name) {
        if (!clientsInRoom.containsKey(id)) {
            clientsInRoom.put(id, name);
        }
    }

    private void removeClientReference(long id) {
        if (clientsInRoom.containsKey(id)) {
            clientsInRoom.remove(id);
        }
    }

    private String getClientNameFromId(long id) {
        if (clientsInRoom.containsKey(id)) {
            return clientsInRoom.get(id);
        }
        if (id == Constants.DEFAULT_CLIENT_ID) {
            return "[Room]";
        }
        return "[name not found]";
    }

    private void processPayload(Payload p) {
        String message;
        switch (p.getPayloadType()) {
            case CLIENT_ID:
                if (myClientId == Constants.DEFAULT_CLIENT_ID) {
                    myClientId = p.getClientId();
                    addClientReference(myClientId, ((ConnectionPayload) p).getClientName());
                    logger.info(TextFX.colorize("My Client Id is " + myClientId, Color.GREEN));
                } else {
                    logger.info(TextFX.colorize("Setting client id to default", Color.RED));
                }
                break;
            case CONNECT:
            case DISCONNECT:
                ConnectionPayload cp = (ConnectionPayload) p;
                message = TextFX.colorize(String.format("*%s %s*",
                        cp.getClientName(),
                        cp.getMessage()), Color.YELLOW);
                logger.info(message);
            case SYNC_CLIENT:
                ConnectionPayload cp2 = (ConnectionPayload) p;
                if (cp2.getPayloadType() == PayloadType.CONNECT || cp2.getPayloadType() == PayloadType.SYNC_CLIENT) {
                    addClientReference(cp2.getClientId(), cp2.getClientName());
                } else if (cp2.getPayloadType() == PayloadType.DISCONNECT) {
                    removeClientReference(cp2.getClientId());
                }
                break;
            case JOIN_ROOM:
                clientsInRoom.clear();
                break;
            case MESSAGE:
                message = TextFX.colorize(String.format("%s: %s",
                        getClientNameFromId(p.getClientId()),
                        p.getMessage()), Color.BLUE);
                System.out.println(message);
                break;
            case LIST_ROOMS:
                try {
                    RoomResultsPayload rp = (RoomResultsPayload) p;
                    if (rp.getMessage() != null && !rp.getMessage().isBlank()) {
                        message = TextFX.colorize(rp.getMessage(), Color.RED);
                        logger.info(message);
                    }
                    List<String> rooms = rp.getRooms();
                    System.out.println(TextFX.colorize("Room Results", Color.CYAN));
                    for (int i = 0; i < rooms.size(); i++) {
                        String msg = String.format("%s %s", (i + 1), rooms.get(i));
                        System.out.println(TextFX.colorize(msg, Color.CYAN));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    public void start() throws IOException {
        listenForKeyboard();
    }

    private void close() {
        myClientId = Constants.DEFAULT_CLIENT_ID;
        clientsInRoom.clear();
        try {
            inputThread.interrupt();
        } catch (Exception e) {
            logger.severe("Error interrupting input");
            e.printStackTrace();
        }
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            logger.severe("Error interrupting listener");
            e.printStackTrace();
        }
        try {
            logger.info("Closing output stream");
            out.close();
        } catch (NullPointerException ne) {
            logger.severe("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            logger.info("Closing input stream");
            in.close();
        } catch (NullPointerException ne) {
            logger.severe("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            logger.info("Closing connection");
            server.close();
            logger.severe("Closed socket");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            logger.warning("Server was never opened so this exception is ok");
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
             // if start is private, it's valid here since this main is part of the class

            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    String getClientName() {
     
        throw new UnsupportedOperationException("Unimplemented method 'getClientName'");
    }
}
