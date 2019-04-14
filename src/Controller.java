import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.UUID;

public class Controller extends Application {
    
    private Stage myStage;
    
    //* all the scenes declaration
    private Scene offline, online;
    private TableView<Server.Client> tableView;
    
    //* networking parts
    private Server server;
    private int port;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        myStage = primaryStage;
        
        primaryStage.setTitle("RPSLS Server");
        primaryStage.setScene(offline);
        primaryStage.show();
    }
    
    @Override
    public void init() throws Exception {
        port = 5087;
        
        _init_online();
        _init_offline();
    }
    
    @Override
    public void stop() throws Exception {
    
    }
    
    private void _init_offline() {
        BorderPane pane = new BorderPane();
        
        pane.setCenter(new VBox() {{
            getChildren().addAll(
                    new TextField() {{
                        setMaxWidth(300);
                        setPromptText("Enter the port");
                        textProperty().addListener(((observable, oldValue, newValue) -> {
                            if (!newValue.matches("\\d*") || (!newValue.equals("") && new Integer(newValue) > 65535)) {
                                setText(oldValue);
                            } else {
                                
                                
                                if (newValue.equals(""))
                                    port = 5087;
                                else
                                    port = new Integer(getText());
                            }
                        }));
                    }},
                    new Button("Turn on") {{
                        setOnAction(event -> {
                            server = new Server(port, str -> {
                            
                            }, () -> Platform.runLater(() -> {
                                tableView.refresh();
                            }));
                            server.start();
                            tableView.setItems(server.getClients());
                            
                            myStage.setScene(online);
                        });
                    }}
            );
            
            setSpacing(30);
            setAlignment(Pos.CENTER);
        }});
        
        offline = new Scene(pane, 400, 400) {{
            getStylesheets().add(getClass().getResource("/stylesheets/main.css").toExternalForm());
        }};
    }
    
    private void _init_online() {
        BorderPane pane = new BorderPane();
        
        tableView = new TableView<Server.Client>();
        
        TableColumn<Server.Client, UUID> uuid = new TableColumn<Server.Client, UUID>("UUID") {{
            setCellValueFactory(new PropertyValueFactory<Server.Client, UUID>("identifier"));
            setMinWidth(300);
        }};
        
        
        TableColumn<Server.Client, UUID> inGameWith = new TableColumn<Server.Client, UUID>("Playing a game with") {{
            setCellValueFactory(new PropertyValueFactory<Server.Client, UUID>("inGameWith"));
            setMinWidth(300);
        }};
        
        
        TableColumn<Server.Client, Playable> played = new TableColumn<Server.Client, Playable>("Played") {{
            setCellValueFactory(new PropertyValueFactory<Server.Client, Playable>("played"));
            setMinWidth(90);
        }};
    
        TableColumn<Server.Client, Integer> wins = new TableColumn<Server.Client, Integer>("Wins") {{
            setCellValueFactory(new PropertyValueFactory<Server.Client, Integer>("wins"));
            setMinWidth(90);
        }};
        
        tableView.setItems(FXCollections.observableArrayList());
        tableView.getColumns().add(uuid);
        tableView.getColumns().add(inGameWith);
        tableView.getColumns().add(played);
        tableView.getColumns().add(wins);
        
        pane.setCenter(tableView);
        
        online = new Scene(pane, 800, 800) {{
            getStylesheets().add(getClass().getResource("/stylesheets/main.css").toExternalForm());
        }};
    }
}