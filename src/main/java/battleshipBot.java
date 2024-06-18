import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.layout.Background;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;

public class battleshipBot {
    @FXML
    private GridPane gameBoard;

    private BackgroundFill bgfill;
    private Button startGameButt;
    private Button joinButt;
    private Button hostButt;
    private Separator sep;
    private Coordinate coord;
    private AlertUI alert = new AlertUI();
    private MultiplayerClient pvp = new MultiplayerClient();
    private MultiplayerHost pvpHost;
    private List<Coordinate> freeSpaces;

    //Initializes PvE game
    @FXML
    public void initialize(){
        setPvEBoard();
    }

    //Sets up PvE board in the app
    public void setPvEBoard(){
        try {
            //Clears game GridPane when restarting game
            if(gameBoard.getChildren().size() > 0){
                gameBoard.getChildren().clear();
            }

            //Sets up 10x10 player board with Coordinate objects
            for (int i = 0; i < 10; i++){
                for (int j = 0; j < 10; j++){
                    coord = new Coordinate(i,j);
                    coord.setWidth(40);
                    coord.setHeight(40);
                    coord.setArcHeight(10);
                    coord.setArcWidth(10);
                    coord.setStroke(Color.WHITE);
                    coord.setFill(Color.DARKBLUE);
                    //add coord block to gameboard once set up
                    gameBoard.add(coord, i, j);
                    //clear coord to make new block
                    fire(coord);
                }
            }

            for (int i = 0; i < 10; i++){
                sep = new Separator();
                sep.setMinHeight(30);
                gameBoard.add(sep, i, 10);
            }

            //sets up computer board
            for (int i = 0; i < 10; i++){
                for (int j = 11; j < 21; j++){
                    coord = new Coordinate(i,j);
                    coord.setWidth(40);
                    coord.setHeight(40);
                    coord.setArcHeight(10);
                    coord.setArcWidth(10);
                    coord.setStroke(Color.WHITE);
                    coord.setFill(Color.DARKBLUE);
                    gameBoard.add(coord, i, j);
                }
            }

        }
        //Catches if board fails to set up game
        catch (NullPointerException e){
            System.out.println(e.getMessage());
        }

        //Button setup
        startGameButt = new Button();
        startGameButt.setText("Start");
        startGameButt.setPrefSize(50, 30);
        gameBoard.add(startGameButt,4, 22, 2, 30);

        joinButt = new Button();
        joinButt.setText("Join");
        joinButt.setPrefSize(50, 30);
        gameBoard.add(joinButt, 6, 22, 2, 30);

        hostButt = new Button();
        hostButt.setText("Host");
        hostButt.setPrefSize(50, 30);
        gameBoard.add(hostButt, 2, 22, 2, 30);

        //Starts/Restarts board for new game
        startGameButt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                restart();
                startGameButt.setDisable(true);
            }
        });

        bgfill = new BackgroundFill(Color.DIMGRAY, null, null);
        Background bg = new Background(bgfill);
        gameBoard.setBackground(bg);

        //Places ships randomly for all players
        buildShips();

        //Defines joinButt and hostButt ActionEvents
        joinGame(joinButt);
        hostGame(hostButt);
    }

    //Defines player action when firing at the bot
    //Defines hits and misses
    private void fire(Coordinate coord){
        coord.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //Sets coord to red if it was a ship and it was hit
                if(coord.getFill() == Color.DARKBLUE && coord.isShip()){
                    coord.setFill(Color.RED);
                }
                //Sets coord to lightgray if it is a ship
                //Bot makes a move right after
                else if(coord.getFill() == Color.DARKBLUE){
                    coord.setFill(Color.WHITE);
                    enemyMove();
                }
                //Alerts player if coord already fired at
                else if(coord.getFill() == Color.WHITE || coord.getFill()==Color.RED){
                    alert.display("WARNING", "Coordinate already fired at");
                }
                //Checks if anyone has won after each move
                checkWinner();
            }
        });
    }

    //Returns int of ships in fleet list
    private int getNumShips(List<Coordinate> fleet){
        int res = 0;
        ObservableList<Node> currBoard = gameBoard.getChildren();
        for(Node coord : currBoard){
            //Sift through currBoard list for Coordinate objects
            if((coord instanceof Coordinate)){
                if(((Coordinate) coord).isShip()){
                    res++;
                }
            }
        }
        return res;
    }

    //Places ships randomly on gameBoard for both bot and player
    private void buildShips(){
        //Populates currBoard with *everything* on app (including buttons and blocks)
        ObservableList<Node> currBoard = gameBoard.getChildren();
        for(Node coord : currBoard){
            //Sift through currBoard list for Coordinate objects
            if((coord instanceof Coordinate)){
                //Places ship at coord
                ((Coordinate) coord).buildShip();
                if(((Coordinate) coord).isShip()){
                    //Sets players ships on bot board as DARKGREEN to differentiate where player's ships are
                    //bot board is stored on indices [11, 21]
                    if(((Coordinate) coord).getCol() >= 11){
                        ((Coordinate) coord).setFill(Color.DARKGREY);
                    }
                }
            }
        }
    }

    //Restarts game
    @FXML
    private void restart(){
        setPvEBoard();
    }

    //Checks if anyone has won
    //Populates 4 different Lists of Coordinate objects based on Coordinate color
    //Compares lists and determines winner from data
    private void checkWinner(){
        ObservableList<Node> currBoard = gameBoard.getChildren();
        //Coordinate list of the enemy fleet that has not been hit yet
        List<Coordinate> enemyFleet = new ArrayList<>();
        //Coordinate list of the successful hits of the enemy
        List<Coordinate> enemySunkShips = new ArrayList<>();
        //Coordinate list of player fleet that has not been hit yet
        List<Coordinate> myFleet = new ArrayList<>();
        //Coordinate list of the successful hits of the player
        List<Coordinate> mySunkShips = new ArrayList<>();

        //populates enemyFleet, myFleet, enemyHits, and myHits based on Coordinate color on board
        for(Node coord : currBoard){
            //Sift through each element that is a Coordinate obj
            if(coord instanceof Coordinate){
                //Player board
                if(((Coordinate) coord).getCol() < 10){
                    //current coord is a ship
                    if(((Coordinate) coord).isShip()){
                        enemyFleet.add(((Coordinate) coord));
                        //Adds to hits if coord is a hit
                        if(((Coordinate) coord).getFill() == Color.RED){
                            enemySunkShips.add(((Coordinate) coord));
                        }
                    }
                }
                //Bot board
                if(((Coordinate) coord).getCol() >= 11){
                    //Current coord is a ship
                    if(((Coordinate) coord).isShip()){
                        myFleet.add((Coordinate) coord);
                        //Adds to hits if cord is a hit
                        if(((Coordinate) coord).getFill() == Color.RED){
                            mySunkShips.add(((Coordinate) coord));
                        }
                    }
                }
            }
        }

        //Compares the size of enemy ships remaining to the size of hits
        //If equal, it means player has hit every single ship
        if(enemyFleet.size() == enemySunkShips.size()){
            alert.display("Congratulations", "YOU'VE WON!!!\nENEMY FLEET DESTROYED");
            //Restarts game afterwards
            restart();
            startGameButt.setDisable(false);
        }

        //Compares the size of player ships remaining to the size of hits
        //If equal, it means player has lost and bot has sunk every ship
        if(myFleet.size() == mySunkShips.size()){
            alert.display("Sorry!", "YOU'VE LOST!!!\nYOUR FLEET WAS DESTROYED");
            //Restarts game afterwards
            restart();
            startGameButt.setDisable(false);
        }
    }

    //Bot chooses a random coordinate based on currentBoard
    //Updates board accordingly
    private void enemyMove() {
        //Creates Random randNum for bot to coordinate attack at
        Random randNum = new Random();
        List<Coordinate> blockList = new ArrayList<>();
        freeSpaces = new ArrayList<>();
        ObservableList<Node> currBoard = gameBoard.getChildren();

        //Add all Coordinates to blocks List
        for(Node coord : currBoard) {
            if (coord instanceof Coordinate) {
                blockList.add((Coordinate) coord);
            }
        }

        //For each block in blocks, if not a hit or ship, add to freeSpaces ArrayList
        for(Coordinate b : blockList){
            if(!b.wasFired() && b.getCol() >= 11){
                freeSpaces.add(b);
            }
       }

        //Selects random block to attack based from freeSpaces ArrayList
        int attackCoordinate = randNum.nextInt(freeSpaces.size() - 1);

        Coordinate coord = freeSpaces.get(attackCoordinate);

        //If coord is a player ship, set color to RED
        if(coord.getFill() == Color.DARKGREY){
            coord.setFill(Color.RED);
            //Make another move
            enemyMove();
        }
        //Set block to miss otherwise
        else if (coord.getFill() == Color.DARKBLUE){
            coord.setFill(Color.WHITE);
        }
        //Check winner
        checkWinner();
    }

    //Joins another player's hosted game
    //Loads PvP stage
    public void joinGame (Button vsPlayer) {
        vsPlayer.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                pvp.loadPVP();
                Stage stage = (Stage) vsPlayer.getScene().getWindow();
                stage.close();
            }
        });
    }

    //Loads host gui
    public void hostGame (Button butt){
        butt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                pvpHost = new MultiplayerHost();
                pvpHost.loadPVP();
                Stage stage = (Stage) butt.getScene().getWindow();
                stage.close();
            }
        });
    }

    //Loads main menu to choose to host, join, or bot battle
    public void loadMain(){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/mm.fxml"));
            Parent root = (Parent) fxmlLoader.load();

            Stage mainMenu = new Stage();
            mainMenu.setScene(new Scene(root, 430, 925));
            mainMenu.setTitle("Battleship");
            mainMenu.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
