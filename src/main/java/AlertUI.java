import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

public class AlertUI {

    public Stage stage;

    public void display(String title, String msg){
        //sets stage size + title
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setMinWidth(300);
        stage.initModality(Modality.APPLICATION_MODAL);

        //msg label
        Label label = new Label();
        label.setText(msg);

        //Close button
        Button closeButt = new Button();
        closeButt.setText("Close");
        closeButt.setOnAction(e -> stage.close());

        //put em all together
        VBox mas = new VBox();
        mas.getChildren().addAll(label, closeButt);
        mas.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mas);
        stage.setScene(scene);
        stage.showAndWait();
    }

    public void alertWait(String title, String msg){
        stage = new Stage();
        stage.setTitle(title);
        stage.setMinWidth(200);
        stage.setMinHeight(100);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);

        //Sets alert label
        Label lbl = new Label();
        lbl.setText(msg);

        //put em all together
        VBox mas = new VBox();
        mas.getChildren().addAll(lbl);
        mas.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mas);
        stage.setScene(scene);
        stage.showAndWait();
    }
}
