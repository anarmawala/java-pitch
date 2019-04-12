import javafx.collections.ObservableList;

import java.util.UUID;

class Actions {
    
    static Server.Client getPlayerWith(UUID id, ObservableList<Server.Client> clients) {
        for (Server.Client client : clients) {
            if (client.getIdentifier().toString().equals(id.toString()))
                return  client;
        }
        
        return null;
    }
    
    static boolean bothPlayed(Server.Client a1, Server.Client b2) {
        return a1.getPlayed() != null && b2.getPlayed() != null;
    }
    
}
