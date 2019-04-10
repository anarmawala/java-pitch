import javafx.collections.ObservableList;

import java.util.UUID;

public class Actions {
    
    public static Server.Client getPlayerWith(UUID id, ObservableList<Server.Client> clients) {
        for (Server.Client client : clients) {
            if (client.getIdentifier() == id)
                return  client;
        }
        
        return null;
    }
    
}
