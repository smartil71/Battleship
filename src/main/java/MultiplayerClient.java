import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class MultiplayerClient {

    @FXML
    private GridPane pvpGameBoard;

    private BackgroundFill bgfill;
    private Button connectButt;
    private Button leaveButt;
    private Separator sep;
    private Coordinate coord;
    private AlertUI alert = new AlertUI();
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private String message;
    private List<Coordinate> playerShips;
    private List<String> shipPositions;
    private List<String> coordinates;
    private List<String> enemyShips = new ArrayList<>();

    //Loads client-side mode for Player v. Player
    //Similar to loadMain() in battleshipBot class
    public void loadPVP(){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/PVP.fxml"));
            Parent root = (Parent) fxmlLoader.load();

            Stage pvpStage = new Stage();
            pvpStage.setScene(new Scene(root, 430, 925));
            pvpStage.setTitle("Battleship: Multiplayer");
            pvpStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Initializes PvP game on port 4000
    @FXML
    public void initialize(){
        //Set up PvP board for both players
        setPvPBoard();
        //Editable
        int gamePort = 4000;

        serverSocket = null;
        try {
            serverSocket = new ServerSocket(gamePort);
            System.out.println("Server on Port: " + gamePort + "\n");
        }
        catch (IOException e) {
            System.out.println("Problem connecting to the port: " + e.getMessage());
            e.printStackTrace();
        }

        //Initializes playerShips if successful connection
        playerShips = buildShips();
    }

    //Sets up boards for both players
    //Same algorithm as setPVEBoard() in pve
    public void setPvPBoard(){
        pvpGameBoard.getChildren().clear();

        //Sets up host board
        for (int i = 0; i < 10; i++){
            for (int j = 0; j<10; j++){
                coord = new Coordinate(i,j);
                coord.setWidth(40);
                coord.setHeight(40);
                coord.setArcHeight(10);
                coord.setArcWidth(10);
                coord.setStroke(Color.WHITE);
                coord.setFill(Color.DARKBLUE);
                //add coord block to pvpGameBoard once set up
                pvpGameBoard.add(coord, i, j);
            }
        }

        for (int i = 0; i < 10; i++){
            sep = new Separator();
            sep.setMinHeight(20);
            pvpGameBoard.add(sep, i, 10);
        }

        //Sup up other player's board
        for (int i = 0; i < 10; i++){
            for (int j = 11; j < 21; j++){
                coord = new Coordinate(i,j);
                coord.setWidth(40);
                coord.setHeight(40);
                coord.setArcHeight(10);
                coord.setArcWidth(10);
                coord.setStroke(Color.WHITE);
                coord.setFill(Color.DARKBLUE);
                //add coord block to pvpGameBoard once set up
                pvpGameBoard.add(coord, i, j);

            }
        }

        connectButt = new Button();
        connectButt.setText("Join");
        connectButt.setPrefSize(50, 30);
        pvpGameBoard.add(connectButt, 6, 22, 2, 30);

        leaveButt = new Button();
        leaveButt.setText("Leave");
        leaveButt.setPrefSize(50, 30);
        pvpGameBoard.add(leaveButt, 2, 22, 2, 30);

        //Connects player to host player
        //Throws ConnectException if failed
        //Populates player's gameBoard to other player's gameBoard
        connectButt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //Creates thread
                Thread t1 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Sends player's ship information to other player
                            sendCoordsToEnemy();

                            //Receives other player's ship information from other player
                            List<String> enemy = getShipCoords();
                            enemyShips.addAll(enemy);
                            
                            //Populate players ships coordinates
                            coordinates = new ArrayList<>();
                            setMultiplayerEnemyCoords(enemy);
                            //Converts player's ships Coordinate information/coordinates to string and sends to other player
                            for(Coordinate ships : playerShips){
                                String pos = ships.getCoords();
                                String nextPos = syncCoordinate(pos);
                                coordinates.add(nextPos);
                            }
                            setMultiplayerMyCoords(coordinates);
                        }
                });
            t1.start();
            }
        });

        //Leaves multiplayer game and returns to bot battle
        //Closes client socket
        leaveButt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    serverSocket.close();
                    closeWindow();
                    System.out.println("Battleship Server has Closed...\nReturning to main menu...");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                
                //Creates new battleshipBot to battle after leaving pvp
                battleshipBot controller = new battleshipBot();
                controller.loadMain();
                Stage stage = (Stage) leaveButt.getScene().getWindow();
                stage.close();
            }
        });

        bgfill = new BackgroundFill(Color.DIMGRAY, null, null);
        Background bg = new Background(bgfill);
        pvpGameBoard.setBackground(bg);
    }
    
    //Sends player's ship coords to enemy for game logic purposes
    public void sendCoordsToEnemy(){
        String serverHostName = "localhost";
        int port = 3000;
        clientSocket = null;

        //initialize clientSocket
        try{
            clientSocket = new Socket(serverHostName, port);
        } 
        //Cant connect to server
        catch (UnknownHostException e){
            System.out.println("Could not connect to the server: " + e.getMessage());
        }
        //ping issue
        catch (IOException e){
            System.out.println("Could not establish a stable connection with the server");
        }

        //After connection secured
        //Send player ship info to enemy
        try{
            OutputStream os = clientSocket.getOutputStream();
            PrintStream printStream = new PrintStream(os);
            
            //for each player ship in playerShips
            for (int i = 0; i < playerShips.size(); i++){
                Coordinate coord = playerShips.get(i);
                String coordString =  coord.getCoords();
                printStream.println(coordString);
            }
        } 
        catch (IOException e){
            System.out.println("Could not establish a stable connection with the server, try again: " + e.getMessage());
        }
    }

    //Complement to sendCoordsToEnemy()
    //Receives enemyPlayer's ship coords for game logic purposes
    //Returns ArrayList of Strings of ship coords
    public List<String> getShipCoords(){
        Socket enemyPlayer = null;

        try{
            enemyPlayer = serverSocket.accept();
        }
        catch (UnknownHostException e){
            System.out.println("Could not connect to the server -> " + e.getMessage());
        }
        catch (IOException e){
            System.out.println("Could not establish a stable connection with the server");
        }

        //After connection secured
        //Send ship positions
        try{
            InputStream is = enemyPlayer.getInputStream();
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader buffer = new BufferedReader(reader);
            String msg;
            shipPositions = new ArrayList<>();

            System.out.println("getting enemy ships\n");
            while ((msg = buffer.readLine()) != null){
                System.out.println("Data received from Player 2: \n\t" + msg);
                shipPositions.add(msg);
                //wait
                breakLoop(buffer);
                if(!buffer.ready()){
                    break;
                }
            }
        }
        catch (IOException e){
                System.out.println("Could not establish a stable connection with the server, try again: " + e.getMessage());
        }
        return shipPositions;
    }

    //Similar to battleshipBot
    //Defines hits and misses
    //Communicates with enemyPlayer to update both boards
    public void fire(Coordinate coord){
        getEnemyData();
            coord.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    //Sets coord to red if it was a ship and it was hit
                    if (coord.getFill() == Color.DARKBLUE && coord.isShip()) {
                        coord.setFill(Color.RED);
                        //Sends data to player 2
                        sendMoveToEnemy(coord);
                        //Check winner
                        checkWinner(enemyShips);
                        //Alert player when other player's turn
                        alert.alertWait("Host's Turn", "Waiting for Host to make turn");
                    }
                    //Sets coord to lightgray if it is a ship
                    //Player 2 makes a move right after
                    else if (coord.getFill() == Color.DARKBLUE) {
                        coord.setFill(Color.WHITE);
                        //Sends data to player 2
                        sendMoveToEnemy(coord);
                        //Check winner
                        checkWinner(enemyShips);
                        //Alert player when other player's turn
                        alert.alertWait("Host's Turn", "Waiting for Host to make turn");
                    }
                    //Edge case where coordinate is already selected
                    else if (coord.getFill() == Color.WHITE || coord.getFill() == Color.RED) {
                        alert.display("WARNING", "Coordinate already selected");
                    }
                }
            });
    }

    //Similar to battleshipBot buildShips()
    //Players get to choose their ships
    //Sends ship data to opposing player for game logic
    //Returns List of Coordinate objects of player's information
    public List<Coordinate> buildShips(){
        ObservableList<Node> currBoard = pvpGameBoard.getChildren();
        List<Coordinate> myShips = new ArrayList<>();
        
        //For each Coordinate coord on currBoard
        for(Node coord : currBoard){
            if(coord instanceof Coordinate){
                //Player1 board
                if(((Coordinate) coord).getCol() < 10){
                    Coordinate currCoordinate = (Coordinate) coord;
                    currCoordinate.setOnMouseClicked(new EventHandler<MouseEvent>() {
                         @Override
                         public void handle(MouseEvent event) {
                             //Limits how many ships player can add to board
                             if(myShips.size() >= 15){
                                 currCoordinate.setOnMouseClicked(null);
                                 alert.display("WARNING", "Your fleet is full! Play the game!!!");
                             }
                             else{
                                 //If player tries to add ship to occupied block
                                 if (myShips.contains(currCoordinate)){
                                     alert.display("WARNING", "This block has already has a ship");
                                 }
                                 //Add ship to coord
                                 else {
                                     myShips.add(currCoordinate);
                                     ((Coordinate) coord).setFill(Color.DARKGREY);
                                 }
                             }
                         }
                     });
                }
            }
        }
       return myShips;
    }

    //Checks if anyone has won
    //Populates 4 different Lists of Coordinate objects based on Coordinate color
    //Compares lists and determines winner from data
    public void checkWinner(List<String> enemy){
        ObservableList<Node> currBoard = pvpGameBoard.getChildren();
        //Coordinate list of the enemy fleet that has not been hit yet
        List<Coordinate> enemyCoordinates = new ArrayList<>();
        //Coordinate list of the successful hits on the enemy
        List<Coordinate> updateEnemyCoordinates = new ArrayList<>();
        //Coordinate list of player fleet that has not been hit
        List<Coordinate> myFleet = new ArrayList<>();
        //Coordinate list of the successful hits on the player
        List<Coordinate> hitShips = new ArrayList<>();

        //populates enemyFleet, myFleet, enemyHits, and myHits based on Coordinate color on board
        for(Node coord : currBoard){
            //Sift through all Coordinate objs
            if (coord instanceof Coordinate){
                //Player 1 board
                if (((Coordinate) coord).getCol() < 10){
                    for (int i = 0; i < enemy.size(); i++){
                        //If current Coordinate coord is an enemy ship
                        if (((Coordinate) coord).getCoords().equals(enemy.get(i))){
                            enemyCoordinates.add(((Coordinate) coord));
                            //If current Coordinate coord is an already hit Coordinate
                            if (((Coordinate) coord).getFill() == Color.RED){
                                updateEnemyCoordinates.add(((Coordinate) coord));
                            }
                        }
                    }
                }
                
                //Player 2 board
                if (((Coordinate) coord).getCol() >= 11){
                    for (int i = 0; i< coordinates.size(); i++){
                        //If current Coordinate coord is a player ship
                        if (((Coordinate) coord).getCoords().equals(coordinates.get(i))){
                            myFleet.add((Coordinate) coord);
                            //If current Coordinate coord is an already hit Coordinate
                            if (((Coordinate) coord).getFill() == Color.RED){
                                hitShips.add(((Coordinate) coord));
                            }
                        }
                    }
                }
            }
        }

        //Compares the size of enemy ships remaining to the size of hits
        //If equal, it means player has hit every single ship
        if(enemyCoordinates.size() == updateEnemyCoordinates.size()){
            alert.display("Congratulations", "YOU'VE WON!!!\nENEMY FLEET DESTROYED");
            alert.stage.close();
        }

        //Compares the size of player ships remaining to the size of hits
        //If equal, it means player has lost and bot has sunk every ship
        if(myFleet.size() == hitShips.size()){
            alert.display("Sorry!", "YOU'VE LOST!!!\nALL YOUR FLEET WAS DESTROYED");
            alert.stage.close();
        }
    }

    //Breaks loops prematurely for efficient socket-socket communication
    private void breakLoop(BufferedReader in) throws IOException {
        long timeSpent = System.currentTimeMillis();
        while(System.currentTimeMillis()-timeSpent < 1000){
            //Breaks loop while timeSpent < 1000
            if(in.ready()){
                break;
            }
        }
    }

    //Converts a Coordinate obj position on board to String position on the enemy's board
    //Syncs on other player's board depending on what coordinate was selected
    private String syncCoordinate (String coord){
        String coordinate;
        switch(coord){
            case "0, 0":
                coordinate = "0, 11";
                break;
            case "1, 0":
                coordinate = "1, 11";
                break;
            case "2, 0":
                coordinate = "2, 11";
                break;
            case "3, 0":
                coordinate = "3, 11";
                break;
            case "4, 0":
                coordinate = "4, 11";
                break;
            case "5, 0":
                coordinate = "5, 11";
                break;
            case "6, 0":
                coordinate = "6, 11";
                break;
            case "7, 0":
                coordinate = "7, 11";
                break;
            case "8, 0":
                coordinate = "8, 11";
                break;
            case "9, 0":
                coordinate = "9, 11";
                break;
            //--------------------2nd row--------------------
            case "0, 1":
                coordinate = "0, 12";
                break;
            case "1, 1":
                coordinate = "1, 12";
                break;
            case "2, 1":
                coordinate = "2, 12";
                break;
            case "3, 1":
                coordinate = "3, 12";
                break;
            case "4, 1":
                coordinate = "4, 12";
                break;
            case "5, 1":
                coordinate = "5, 12";
                break;
            case "6, 1":
                coordinate = "6, 12";
                break;
            case "7, 1":
                coordinate = "7, 12";
                break;
            case "8, 1":
                coordinate = "8, 12";
                break;
            case "9, 1":
                coordinate = "9, 12";
                break;
            //--------------------3rd row--------------------
            case "0, 2":
                coordinate = "0, 13";
                break;
            case "1, 2":
                coordinate = "1, 13";
                break;
            case "2, 2":
                coordinate = "2, 13";
                break;
            case "3, 2":
                coordinate = "3, 13";
                break;
            case "4, 2":
                coordinate = "4, 13";
                break;
            case "5, 2":
                coordinate = "5, 13";
                break;
            case "6, 2":
                coordinate = "6, 13";
                break;
            case "7, 2":
                coordinate = "7, 13";
                break;
            case "8, 2":
                coordinate = "8, 13";
                break;
            case "9, 2":
                coordinate = "9, 13";
                break;
            //--------------------4th row--------------------
            case "0, 3":
                coordinate = "0, 14";
                break;
            case "1, 3":
                coordinate = "1, 14";
                break;
            case "2, 3":
                coordinate = "2, 14";
                break;
            case "3, 3":
                coordinate = "3, 14";
                break;
            case "4, 3":
                coordinate = "4, 14";
                break;
            case "5, 3":
                coordinate = "5, 14";
                break;
            case "6, 3":
                coordinate = "6, 14";
                break;
            case "7, 3":
                coordinate = "7, 14";
                break;
            case "8, 3":
                coordinate = "8, 14";
                break;
            case "9, 3":
                coordinate = "9, 14";
                break;
            //--------------------5th row--------------------
            case "0, 4":
                coordinate = "0, 15";
                break;
            case "1, 4":
                coordinate = "1, 15";
                break;
            case "2, 4":
                coordinate = "2, 15";
                break;
            case "3, 4":
                coordinate = "3, 15";
                break;
            case "4, 4":
                coordinate = "4, 15";
                break;
            case "5, 4":
                coordinate = "5, 15";
                break;
            case "6, 4":
                coordinate = "6, 15";
                break;
            case "7, 4":
                coordinate = "7, 15";
                break;
            case "8, 4":
                coordinate = "8, 15";
                break;
            case "9, 4":
                coordinate = "9, 15";
                break;
            //--------------------6th row--------------------
            case "0, 5":
                coordinate = "0, 16";
                break;
            case "1, 5":
                coordinate = "1, 16";
                break;
            case "2, 5":
                coordinate = "2, 16";
                break;
            case "3, 5":
                coordinate = "3, 16";
                break;
            case "4, 5":
                coordinate = "4, 16";
                break;
            case "5, 5":
                coordinate = "5, 16";
                break;
            case "6, 5":
                coordinate = "6, 16";
                break;
            case "7, 5":
                coordinate = "7, 16";
                break;
            case "8, 5":
                coordinate = "8, 16";
                break;
            case "9, 5":
                coordinate = "9, 16";
                break;
            //--------------------7th row--------------------
            case "0, 6":
                coordinate = "0, 17";
                break;
            case "1, 6":
                coordinate = "1, 17";
                break;
            case "2, 6":
                coordinate = "2, 17";
                break;
            case "3, 6":
                coordinate = "3, 17";
                break;
            case "4, 6":
                coordinate = "4, 17";
                break;
            case "5, 6":
                coordinate = "5, 17";
                break;
            case "6, 6":
                coordinate = "6, 17";
                break;
            case "7, 6":
                coordinate = "7, 17";
                break;
            case "8, 6":
                coordinate = "8, 17";
                break;
            case "9, 6":
                coordinate = "9, 17";
                break;
            //--------------------8th row--------------------
            case "0, 7":
                coordinate = "0, 18";
                break;
            case "1, 7":
                coordinate = "1, 18";
                break;
            case "2, 7":
                coordinate = "2, 18";
                break;
            case "3, 7":
                coordinate = "3, 18";
                break;
            case "4, 7":
                coordinate = "4, 18";
                break;
            case "5, 7":
                coordinate = "5, 18";
                break;
            case "6, 7":
                coordinate = "6, 18";
                break;
            case "7, 7":
                coordinate = "7, 18";
                break;
            case "8, 7":
                coordinate = "8, 18";
                break;
            case "9, 7":
                coordinate = "9, 18";
                break;
            //--------------------9th row--------------------
            case "0, 8":
                coordinate = "0, 19";
                break;
            case "1, 8":
                coordinate = "1, 19";
                break;
            case "2, 8":
                coordinate = "2, 19";
                break;
            case "3, 8":
                coordinate = "3, 19";
                break;
            case "4, 8":
                coordinate = "4, 19";
                break;
            case "5, 8":
                coordinate = "5, 19";
                break;
            case "6, 8":
                coordinate = "6, 19";
                break;
            case "7, 8":
                coordinate = "7, 19";
                break;
            case "8, 8":
                coordinate = "8, 19";
                break;
            case "9, 8":
                coordinate = "9, 19";
                break;
            //--------------------10th row--------------------
            case "0, 9":
                coordinate = "0, 20";
                break;
            case "1, 9":
                coordinate = "1, 20";
                break;
            case "2, 9":
                coordinate = "2, 20";
                break;
            case "3, 9":
                coordinate = "3, 20";
                break;
            case "4, 9":
                coordinate = "4, 20";
                break;
            case "5, 9":
                coordinate = "5, 20";
                break;
            case "6, 9":
                coordinate = "6, 20";
                break;
            case "7, 9":
                coordinate = "7, 20";
                break;
            case "8, 9":
                coordinate = "8, 20";
                break;
            case "9, 9":
                coordinate = "9, 20";
                break;
            //Default, players make choice outside of boundaries
            default:
                System.out.println("Coordinates outside Battleship boundaries");
                coordinate = null;
        }
        return coordinate;
    }

    //Helper function to identify Coordinate objs that are ships for pvp communication
    private void setMultiplayerEnemyCoords(List<String> ships){
        ObservableList<Node> currBoard = pvpGameBoard.getChildren();
        //Each Coordinate child of the current gameBoard
        for (Node currNode : currBoard){
            if (currNode instanceof Coordinate){
                if (((Coordinate) currNode).getCol() < 10){
                    ((Coordinate) currNode).setFill(Color.DARKBLUE);

                    //Fire at currNode (cast as Coordinate obj) to check if Ship type
                    fire((Coordinate) currNode);
                    for (String pos : ships){
                        //update Coordinate to ship
                        if (pos.equals(((Coordinate) currNode).getCoords())){
                            ((Coordinate) currNode).setShip(true);
                        }
                    }
                }
            }
        }
    }

    //Updates board colors based on moves
    //DARKGREY == your fleet
    //RED == hit ship
    //DARKBLUE == water
    //WHITE == miss
    private void updateEnemyBoard (String str){
        ObservableList<Node> currBoard = pvpGameBoard.getChildren();
        //Each Coordinate child of the current gameBoard
        for (Node currNode : currBoard){
            if (currNode instanceof Coordinate){
                //Each Coordinate of the enemy board
                if (((Coordinate) currNode).getCol() >= 11){
                    //enemyShip == coords of BLock
                    if (str.equals(((Coordinate) currNode).getCoords())){
                        //Updates ship blocks from DARKGREY to RED
                        if (((Coordinate) currNode).getFill() == Color.DARKGREY){
                            ((Coordinate) currNode).setFill(Color.RED);
                        }
                        //Updates water blocks from DARKBLUE to WHITE
                        else if (((Coordinate) currNode).getFill() == Color.DARKBLUE){
                            ((Coordinate) currNode).setFill(Color.WHITE);
                        }
                    }
                }
            }
        }
    }

    //Updates players ships for enemy board
    private void setMultiplayerMyCoords(List<String> myFleet){
        for (String s : myFleet){
            System.out.println("Your ship found at: " + s + "\n");
        }

        ObservableList<Node> currBoard = pvpGameBoard.getChildren();
        //Each Coordinate on Board
        for (Node currNode : currBoard){
            if (currNode instanceof Coordinate){
                for (String pos : myFleet){
                    //Bottom board
                    if (((Coordinate) currNode).getCol() >= 11) {
                        //update Coordinate color to represent ship
                        if (pos.equals(((Coordinate) currNode).getCoords())) {
                            ((Coordinate) currNode).setFill(Color.DARKGREY);
                        }
                    }
                }
            }
        }
    }

    //Sends player move to enemy
    private void sendMoveToEnemy(Coordinate coord){
        //create client thread to send
        new Thread(new Runnable() {
            @Override
            public void run() {
                //servername localhost
                //port editable
                clientSocket = null;
                String serverHostName = "localhost";
                int port = 3000;

                //find client port
                try{
                    clientSocket = new Socket(serverHostName, port);
                }
                catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                try{
                    OutputStream os = clientSocket.getOutputStream();
                    PrintStream ps = new PrintStream(os);
                    String coordinate = coord.getCoords();
                    String newPos = syncCoordinate(coordinate);
                    ps.println(newPos);

                    //close socket after data sent
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Could not establish a stable connection with the server: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
}

    //Complement method to sendMoveToEnemy
    //does the opposite
    private void getEnemyData(){
        //create client thread to receive
        new Thread(new Runnable() {
        @Override
        public void run() {
            //clientside socket to communicate
            Socket readSocket = null;
            try{
                readSocket = serverSocket.accept();
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
            //Check if host has closed the game and return players to bot main menu
            catch (SocketException e){
                System.out.println("Host has left the game");
                try {
                    readSocket.close();
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try{
                InputStream inStream = readSocket.getInputStream();
                InputStreamReader readStream = new InputStreamReader(inStream);
                BufferedReader buffer = new BufferedReader(readStream);
                //Continue sending data until break
                while ((message = buffer.readLine()) != null){
                    updateEnemyBoard(message);
                    breakLoop(buffer);
                    if (!buffer.ready()){
                        break;
                    }
                }
                //close thread, data is no longer being read
                closeWindow();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }).start();
}

    //kills Alert window
    private void closeWindow(){
        Runnable closeWindow = new Runnable() {
            @Override
            public void run() {
                alert.stage.close();
            }
        };
        Platform.runLater(closeWindow);
    }
}