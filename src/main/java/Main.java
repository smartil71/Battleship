import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/FXML/mm.fxml"));
        primaryStage.setScene(new Scene(root, 430, 925));
        primaryStage.setTitle("Battleship");
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}
