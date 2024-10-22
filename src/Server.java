import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

class Server implements Serializable {
    private int port;
    
    //! callbacks to the controller for different events
    private Consumer<String> addMessage;
    private Runnable refresh;
    
    //! Keep track of all the players through playerID
    private ConnectionThread connectionThread;
    private ObservableList<Client> clients = FXCollections.observableList(new ArrayList<Client>());
    
    Server(int port, Consumer<String> addMessage, Runnable refresh) {
        this.port = port;
        this.addMessage = addMessage;
        this.refresh = refresh;
        
        try {
            connectionThread = new ConnectionThread();
        } catch (Exception e) {
            System.err.println("Do nothing");
        }
        
    }
    
    void start() {
        connectionThread.start();
    }
    
    void stop() {
        try {
            connectionThread.shouldContinue = false;
            connectionThread.server.close();
        } catch (Exception e) {
            System.err.println("Error while closing the ServerSocket");
        }
        
        clients.forEach(client -> {
            client.send("data--close");
            client.shouldContinue = false;
            
            try {
                if (!client.socket.isClosed())
                    client.socket.close();
                
            } catch (Exception e) {
                System.err.println("Error while closing socket");
            }
        });
    }
    
    ObservableList<Client> getClients() {
        return clients;
    }
    
    public class ConnectionThread extends Thread {
        private ServerSocket server;
        private boolean shouldContinue;
        
        ConnectionThread() throws IOException {
            server = new ServerSocket(port);
            shouldContinue = true;
        }
        
        @Override
        public void run() {
            
            // makes sure the loop breaks properly
            while (shouldContinue) {
                
                try {
                    UUID id = UUID.randomUUID();
                    
                    while (Actions.getPlayerWith(id, clients) != null) {
                        id = UUID.randomUUID();
                    }
                    
                    Client client = new Client(server.accept(), id);
                    refresh.run();
                    
                    String clientList = "list--";
                    for (Client inList : clients) {
                        clientList = clientList.concat(inList.identifier.toString() + ":" + (inList.inGameWith == null ? "No" : "Yes") + ":" + inList.wins + "++");
                    }
                    client.send(clientList);
                    
                    
                    clients.add(client);
                    // TODO add other callback accepts to fully update gui
                    // addMessage.accept("Player " + id.toString() + " connected!");
                    
                    //* Player connects to the server
                    for (Client player : clients) {
                        player.send("data--connect--" + client.identifier.toString());
                    }
                    
                    
                    client.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public class Client extends Thread {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        
        private UUID identifier;
        private boolean shouldContinue;
        
        private UUID inGameWith;
        private Playable played;
        
        private int wins;
        
        Client(Socket s, UUID id) throws Exception {
            //* initializing data members
            socket = s;
            identifier = id;
            shouldContinue = true;
            inGameWith = null;
            played = null;
            wins = 0;
            
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                out.writeUTF("uuid--" + identifier.toString());
                out.flush();
            } catch (Exception e) {
                // makes sure that thread isn't run if it fails
                e.printStackTrace();
                shouldContinue = false;
                throw e;
            }
        }
        
        @Override
        public void run() {
            handleComm();
        }
        
        synchronized void handleComm() {
            
            while (true) {
                // checks if thread should be running
                if (!shouldContinue)
                    break;
                
                try {
                    if (in.available() > 0) {
                        refresh.run();
                        String[] data = in.readUTF().split("--");
                        System.out.println(Arrays.toString(data));
                        
                        switch (data[0]) {
                            case "leave":
                                clients.remove(this);
                                socket.close();
                                
                                //* Player disconnected
                                clients.forEach(player -> player.send("data--leave--" + identifier.toString()));
                                break;
                            case "challenge":
                                if (data[1].equals("accept")) {
                                    UUID acceptFrom = UUID.fromString(data[2]);
                                    Client other = Actions.getPlayerWith(acceptFrom, clients);
                                    
                                    if (other != null && other.inGameWith == null && inGameWith == null) {
                                        other.inGameWith = identifier;
                                        inGameWith = other.identifier;
                                        
                                        other.played = null;
                                        played = null;
                                        
                                        send("action--gameStart");
                                        other.send("action--gameStart");
                                        
                                        //* Two players are in game so others know that they can't be played
                                        clients.forEach(player -> player.send("data--starting--" + identifier.toString() + "--" + other.identifier.toString()));
                                    }
                                } else {
                                    UUID challengeTo = UUID.fromString(data[1]);
                                    Client other = Actions.getPlayerWith(challengeTo, clients);
                                    
                                    if (other != null) {
                                        other.send("action--challenged--" + identifier);
                                    }
                                }
                                break;
                            case "move":
                                if (inGameWith == null)
                                    break;
                                
                                Client other = Actions.getPlayerWith(inGameWith, clients);
                                
                                if (other == null) {
                                    inGameWith = null;
                                } else {
                                    played = Playable.valueOf(data[1]);
                                    if (other.played != null) {
                                        other.send("action--play--" + played.toString());
                                        send("action--play--" + other.played.toString());
                                        
                                        int decision = played.beats(other.played);
                                        if (decision == 1) {
                                            other.send("loser");
                                            send("winner");
                                            
                                            clients.forEach(player -> player.send("data--won--" + identifier.toString()));
                                            wins++;
                                        } else if (decision == -1) {
                                            send("loser");
                                            other.send("winner");
                                            
                                            clients.forEach(player -> player.send("data--won--" + other.identifier.toString()));
                                            other.wins++;
                                        }
                                        
                                        inGameWith = null;
                                        played = null;
                                        other.inGameWith = null;
                                        other.played = null;
                                        
                                        Thread.sleep(3000);
                                        other.send("action--reset");
                                        send("action--reset");
                                        clients.forEach(client -> client.send("data--ending--" + identifier + "--" + other.identifier));
                                    }
                                }
                                
                                break;
                        }
                    }
                } catch (Exception e) {
                    clients.remove(this);
                    clients.forEach(player -> player.send("data--leave--" + identifier.toString()));
                    break;
                }
                
                
            }
        }
        
        public UUID getIdentifier() {
            return identifier;
        }
        
        public UUID getInGameWith() {
            return inGameWith;
        }
        
        public Playable getPlayed() {
            return played;
        }
        
        public int getWins() {
            return wins;
        }
        
        void send(String data) {
            try {
                out.writeUTF(data);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
                clients.remove(this);
                
                try {
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Already Closed");
                }
                shouldContinue = false;
                for (Client player : clients) {
                    player.send("data--leave--" + identifier.toString());
                }
            }
        }
    }
}