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
                    Client client = new Client(server.accept(), id);
                    clients.add(client);
                    
                    // TODO add other callback accepts to fully update gui
                    // addMessage.accept("Player " + id.toString() + " connected!");
                    
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
        
        Client(Socket s, UUID id) throws Exception {
            //* initializing data members
            socket = s;
            identifier = id;
            shouldContinue = true;
            inGameWith = null;
            played = null;
            
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                out.writeUTF(identifier.toString());
                out.flush();
            } catch (Exception e) {
                // makes sure that thread isn't run if it fails
                shouldContinue = false;
                throw e;
            }
        }
        
        @Override
        public void run() {
            while (true) {
                // checks if thread should be running
                if (!shouldContinue)
                    break;
                
                
                try {
                    
                    String[] data = in.readUTF().split("--");
                    System.out.println(Arrays.toString(data));
                    
                    switch (data[0]) {
                        case "leave":
                            clients.remove(this);
                            socket.close();
                            
                            clients.forEach(player -> player.send("action--leave--" + identifier.toString()));
                            break;
                        case "challenge":
                            if (data[1].equals("accept")) {
                                UUID acceptFrom = UUID.fromString(data[2]);
                                Client other = Actions.getPlayerWith(acceptFrom, clients);
                                
                                if (other != null && other.inGameWith == null && inGameWith == null) {
                                    other.inGameWith = identifier;
                                    identifier = other.identifier;
                                    send("action--gameStart");
                                    other.send("action--gameStart");
                                }
                                
                            } else if (data[1].equals("deny")) {
                                UUID denyTo = UUID.fromString(data[2]);
                            } else {
                                UUID challengeTo = UUID.fromString(data[1]);
                            }
                            break;
                        case "move":
                            //
                            break;
                        default:
                            refresh.run();
                            break;
                    }
                    
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
                
                refresh.run();
            }
        }
        
        UUID getIdentifier() {
            return identifier;
        }
        
        void send(String data) {
            try {
                clients.remove(this);
                out.writeUTF(data);
                out.flush();
                socket.close();
                shouldContinue = false;
            } catch (Exception e) {
                System.out.println("Already closed");
            }
            
            for (Client player : clients) {
                player.send("action--leave--" + identifier.toString());
            }
        }
    }
}