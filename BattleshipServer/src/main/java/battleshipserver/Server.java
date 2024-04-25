package  battleshipserver;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Server {
    private ServerSocket serverSocket;
    private Socket player1Socket;
    private Socket player2Socket;
    private ObjectOutputStream out1, out2;
    private ObjectInputStream in1, in2;
    private Grid player1Grid;
    private Grid player2Grid;
    private boolean player1Turn = true;

    public Server(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started. Waiting for players...");

        player1Socket = serverSocket.accept();
        out1 = new ObjectOutputStream(player1Socket.getOutputStream());
        in1 = new ObjectInputStream(player1Socket.getInputStream());
        out1.writeObject("you are P1");

        player2Socket = serverSocket.accept();
        out2 = new ObjectOutputStream(player2Socket.getOutputStream());
        in2 = new ObjectInputStream(player2Socket.getInputStream());
        out2.writeObject("you are P2");
        player1Grid = new Grid(10, 10, 5);
        player2Grid = new Grid(10, 10, 5);
        while((!player1Socket.isClosed()) && (!player2Socket.isClosed())) {

            setupShips(in1, out1, player1Grid, "Player 1");
            setupShips(in2, out2, player2Grid, "Player 2");

            startGame();
            out1.writeObject("reset");
            out2.writeObject("reset");
            cleanup();
        }
    }

    void cleanup(){
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                player1Grid.squares[row][col].hit = false;
                player1Grid.squares[row][col].ship = null;
                player1Grid.ships = new Ship[5];
                player1Grid.shipsAdded = 0;
                player2Grid.squares[row][col].hit = false;
                player2Grid.squares[row][col].ship = null;
                player2Grid.ships = new Ship[5];
                player2Grid.shipsAdded = 0;
            }
        }

    }
    private void setupShips(ObjectInputStream in, ObjectOutputStream out, Grid grid, String playerName) throws Exception {
        out.writeObject(playerName + ", enter coordinates for all ships. Format: A1,A3|B1,B4|C1,C5|D1,D2|E1");
        String coords = (String) in.readObject();
        String[] shipCoords = coords.split("\\|");
        if (shipCoords.length == 5) {
            for (String coordPair : shipCoords) {
                String[] parts = coordPair.split(",");
                if (parts.length == 2) {
                    Ship ship = grid.setShip(parts[0], parts[1]);
                    if (ship == null) {
                        out.writeObject("Invalid coordinates or overlap, please re-enter.");
                        setupShips(in, out, grid, playerName);
                        return;
                    }
                } else {
                    out.writeObject("Invalid input format, please use StartCoord,EndCoord.");
                    setupShips(in, out, grid, playerName);
                    return;
                }
            }
            out.writeObject(coords);
        } else {
            out.writeObject("Invalid number of ships, please re-enter.");
            setupShips(in, out, grid, playerName);
        }
    }

    private void startGame() {
        //int count = 0;
        try {
            boolean gameOver = false;
            while (!gameOver) {
                //if(count >= 3){break;}
                //count++;
                if (player1Turn) {
                    handleMove(in1, out1, out2, player2Grid, "P1");
                } else {
                    handleMove(in2, out2, out1, player1Grid, "P2");
                }
                // Check for game over condition here
                if (player1Grid.shipsRemaining() == 0 || player2Grid.shipsRemaining() == 0) {
                    gameOver = true;
                    String winner = player1Grid.shipsRemaining() == 0 ? "Player 2" : "Player 1";
                    broadcast(winner + " wins!", out1, out2);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMove(ObjectInputStream in, ObjectOutputStream out, ObjectOutputStream outOpponent, Grid opponentGrid, String currentPlayer) throws Exception {
        //out.writeObject(currentPlayer + " Turn ");
        String move = (String) in.readObject();
        Ship hitShip = opponentGrid.doHit(move);
        String result = hitShip != null ? "Hit" : "Miss";
        opponentGrid.printBoard();
        broadcast(currentPlayer + "," + move + "," + result, out, outOpponent);
        if (hitShip == null) {
            player1Turn = !player1Turn; // Toggle turn
        }
    }

    private void broadcast(String message, ObjectOutputStream out1, ObjectOutputStream out2) throws Exception {
        out1.writeObject(message);
        out2.writeObject(message);
    }

    public static void main(String[] args) {
        try {
            new Server(5555);
        } catch (Exception e) {
            System.out.println("Failed to start server: " + e.getMessage());
        }
    }
}
