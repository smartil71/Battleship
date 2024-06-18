import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.Random;

//Coordinate class for entire game board
public class Coordinate extends Rectangle {
    //x coord y coord
    private int row;
    private int col;
    private boolean ship = false;
    private boolean fired = false;

    //Constructor
    public Coordinate(int r, int c){
        //call Rectangle class constructor
        super(30,30);
        setArcHeight(10);
        setArcWidth(10);
        //set colors
        setFill(Color.DARKBLUE);
        setStroke(Color.WHITE);
        //set coordinates
        this.row = r;
        this.col = c;
    }

    //Creates ship at Coordinate
    public void buildShip() {
        //chooses random (x, y) coordinate
        Random rand = new Random();
        int xCoord = rand.nextInt(9);
        int yCoord = rand.nextInt(9);

        //2nd random y coord for buildShip chance
        int y2Coord = rand.nextInt((21 - 11) + 1) + 11;

        //40 chances to place a ship at xCoord, yCoord
        for(int i = 0; i < 20; i++){
            for(int j = 0; j < 20; j++){
                if(getRow() == xCoord || getCol() == yCoord){
                    ship = true;
                }

                if(getRow() == xCoord || getCol() == y2Coord){
                    ship = true;
                }
            }
        }
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    //Used for identifying ship nodes
    public boolean isShip() {
        return ship;
    }

    public void setShip(boolean temp) {
        ship = temp;
    }

    //Returns row and col coords in the form of "r, c"
    public String getCoords(){
        return getRow() + ", " + getCol();
    }

    //Check if coordinate was already fired at
    public boolean wasFired(){
        if(getFill() == Color.WHITE || getFill() == Color.RED){
            fired = true;
        }
        //updates fired var
        //still can be hit
        else {
            fired = false;
        }
        return fired;
    }
}
