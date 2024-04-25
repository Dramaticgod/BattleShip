package BattleshipClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;

public class BattleshipGame extends Application {
    private TextArea messageArea = new TextArea();
    private TextField inputField = new TextField();
    private Button sendButton = new Button("Send");
    private Client client;
    private Grid playerGrid;
    private Grid enemyGrid;
    private TextField[][] playerCells = new TextField[10][10];
    private TextField[][] enemyCells = new TextField[10][10];
    private String playerIdentifier;  // Player ID: P1 or P2
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        client = new Client("127.0.0.1", 5555, messageArea, this);
        new Thread(client).start();

        playerGrid = new Grid(10, 10, 5);
        enemyGrid = new Grid(10, 10, 5);
        BorderPane root = new BorderPane();
        GridPane playerBoard = createBoard(true);
        GridPane enemyBoard = createBoard(false);

        inputField.setPromptText("Enter your move");
        messageArea.setEditable(false);
        ScrollPane messageScrollPane = new ScrollPane(messageArea);
        messageScrollPane.setFitToWidth(true);

        root.setTop(inputField);
        root.setCenter(playerBoard);
        root.setRight(enemyBoard);
        root.setBottom(messageScrollPane);
        root.setLeft(sendButton);

        sendButton.setOnAction(e -> client.sendMessage(inputField.getText()));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Battleship Game: Waiting for player identity...");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane createBoard(boolean isPlayerBoard) {
        GridPane grid = new GridPane();
        // Set style for the grid to make it more visually distinct
        grid.setStyle("-fx-grid-lines-visible: true");

        // Adding column headers for numbers 1-10


        for (int col = -1; col < 10; col++) {
            Label label = new Label(Integer.toString(col));
            label.setPrefWidth(30);  // Set preferred width for consistency
            label.setMinWidth(30);

            label.setAlignment(Pos.CENTER);
            if(col == -1) {
                Label temp =  new Label(" ");
                temp.setPrefWidth(30);  // Set preferred width for consistency
                temp.setMinWidth(30);
                grid.add(temp, col+1, 0);  // Position label above the column
            }
            else {
                grid.add(label, col+1, 0);
            }
        }

        // Adding row headers for letters A-J
        for (int row = 0; row < 10; row++) {
            Label label = new Label(Character.toString((char)('A' + row)));
            label.setPrefWidth(30);  // Set preferred width for consistency
            label.setMinWidth(30);
            label.setAlignment(Pos.CENTER);
            grid.add(label, 0, row + 1);  // Position label beside the row
        }

        // Create cells for the grid, offset by one to account for headers
        for (int row = 1; row <= 10; row++) {
            for (int col = 1; col <= 10; col++) {
                TextField cell = new TextField();
                cell.setEditable(false);
                cell.setPrefSize(30, 30);
                grid.add(cell, col, row);
                if (isPlayerBoard) {
                    playerCells[row - 1][col - 1] = cell;  // Adjust indexing to fill playerCells from [0][0]
                } else {
                    enemyCells[row - 1][col - 1] = cell;  // Adjust indexing to fill enemyCells from [0][0]
                }
            }
        }
        return grid;
    }
    public void updateGuiFromGrid() {
        Platform.runLater(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    updateCellAppearance(playerCells[i][j], playerGrid.squares[i][j]);
                    //updateCellAppearance(enemyCells[i][j], enemyGrid.squares[i][j]);
                }
            }
        });
    }

    void updateCellAppearance(TextField cell, Square square) {
        if (square.ship != null && square.hit) {
            cell.setStyle("-fx-background-color: red;");
        } else if (square.ship != null) {
            cell.setStyle("-fx-background-color: pink;");
        } else if (square.hit) {
            cell.setStyle("-fx-background-color: black;");
        } else {
            cell.setStyle("");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    class Client implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private TextArea messageArea;
        private BattleshipGame game;

        public Client(String serverAddress, int port, TextArea messageArea, BattleshipGame game) {
            this.messageArea = messageArea;
            this.game = game;
            try {
                socket = new Socket(serverAddress, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } catch (Exception e) {
                Platform.runLater(() -> messageArea.appendText("Error connecting to server: " + e.getMessage() + "\n"));
            }
        }

        public void run() {
            try {
                while (true) {
                    String serverMessage = (String) in.readObject();
                    processServerMessage(serverMessage);
                    Platform.runLater(() -> messageArea.appendText("Server: " + serverMessage + "\n"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> messageArea.appendText("Error: " + e.getMessage() + "\n"));
            } finally {
                close();
            }
        }

        private void processServerMessage(String message) {
            System.out.println("message : " + message);
            if (message.startsWith("you are P")) {
                playerIdentifier = message.substring(8).trim();
                Platform.runLater(() -> game.primaryStage.setTitle("Battleship Game: " + playerIdentifier));
            }else if (message.contains("|")) {
                handleShipPlacement(message);
            } else if (message.matches("P[1-2],\\s*([A-J](?:10|[0-9])),\\s*(HIT|MISS|WIN|Hit|Miss|Win)\\s*")) {
                //System.out.println("Triggering handleAction");
                handleAction(message);
            }
            else if(message.equals("reset")){
                cleanup();
            }
            game.updateGuiFromGrid();
        }

        void cleanup(){
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    enemyCells[row][col].setStyle("");
                    playerCells[row][col].setStyle("");
                    playerGrid.squares[row][col].hit = false;
                    playerGrid.squares[row][col].ship = null;
                    playerGrid.ships = new Ship[5];
                    playerGrid.shipsAdded = 0;
                    enemyGrid.squares[row][col].hit = false;
                    enemyGrid.squares[row][col].ship = null;
                    enemyGrid.ships = new Ship[5];
                    enemyGrid.shipsAdded = 0;
                }
            }

        }

        void handleShipPlacement(String message) {
            String[] shipCoords = message.split("\\|");
            if (validateShipCoordinates(shipCoords)) {
                for (String coordPair : shipCoords) {
                    String[] parts = coordPair.split(",");
                    if (parts.length == 2) {
                        playerGrid.setShip(parts[0], parts[1]);  // Set ship on the player grid
                    }
                }
                System.out.println("Ship placement confirmed");
                updateGuiFromGrid();  // Updates the GUI from the grid state
            } else {
                System.out.println("Invalid ship placement format or coordinates. Please re-enter.");
            }
        }

        // Validate ship coordinates
        private boolean validateShipCoordinates(String[] coords) {
            if (coords.length != 5) return false;  // Assuming exactly 5 ships need to be placed
            for (String coord : coords) {
                if (!coord.matches("[A-J](10|[1-9]),[A-J](10|[1-9])")) {
                    return false;
                }
            }
            return true;
        }

        private void handleAction(String message) {
            String[] parts = message.split(",");
            String currentPlayer = parts[0];
            String coordinates = parts[1];
            String result = parts[2];
            //System.out.println("coordinates.charAt(1) " + coordinates.charAt(1));
            //System.out.println("coordinates.charAt(0) " + coordinates.charAt(0));
            int number = coordinates.charAt(1) - '0';
            int letter = coordinates.toUpperCase().charAt(0) - 'A';

            //DEBUG
            //System.out.println("Local player: " + playerIdentifier);
            //System.out.println("CurrentPlayer from string parsing : " + currentPlayer);
            System.out.println("letter: " + letter + " number: "  + number );
            //System.out.println("CurrentPlayer from string parsing : " + result);
            if (Objects.equals(currentPlayer, playerIdentifier)) {
                Platform.runLater(() -> {
                    if (result.equalsIgnoreCase("Hit")) {
                        enemyCells[letter][number].setStyle("-fx-background-color: red;"); // Hit
                    } else if (result.equalsIgnoreCase("Miss")) {
                        enemyCells[letter][number].setStyle("-fx-background-color: black;"); // Miss
                    }
                });
            }
            else{
                Platform.runLater(() -> {
                    playerGrid.squares[letter][number].hit = true;
                });
            }

        }

        void sendMessage(String message) {
            try {
                out.writeObject(message);
                Platform.runLater(() -> {
                    //messageArea.appendText("You: " + message + "\n");
                    inputField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> messageArea.appendText("Failed to send message: " + e.getMessage() + "\n"));
            }
        }
        private void close() {
            try {
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (Exception e) {
                Platform.runLater(() -> messageArea.appendText("Error closing connection: " + e.getMessage() + "\n"));
            }
        }
    }
}
