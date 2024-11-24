import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;

import java.io.IOException;

public class WaitOrLeaveDialog {

    @FXML
    GridPane getNameDialogPane;

    Stage stage;

    @FXML
    Button waitButton;

    @FXML
    Button leaveButton;

    GameWindow gameWindow;

/*
    public WaitOrLeaveDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("waitOrLeave.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

//        ipField.setText("127.0.0.1");
//        portField.setText("1234");

        getNameDialogPane.setPadding(new Insets(10, 10, 10, 10));

        waitButton.setOnMouseClicked(this::OnButtonClickWait);
        leaveButton.setOnMouseClicked(this::OnButtonClickLeave);

        stage.showAndWait();
    }
    */

    public WaitOrLeaveDialog(GameWindow gameWindow) throws IOException {
        this.gameWindow = gameWindow;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("waitOrLeave.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

//        ipField.setText("127.0.0.1");
//        portField.setText("1234");

        //getNameDialogPane.setPadding(new Insets(10, 10, 10, 10));

        waitButton.setOnMouseClicked(this::OnButtonClickWait);
        leaveButton.setOnMouseClicked(this::OnButtonClickLeave);

        stage.showAndWait();
    }

/*
    public void show() throws IOException {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("waitOrLeave.fxml"));
        Parent root = loader.load();


        // Create a new Stage (window)
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL); // Make it modal
        stage.setTitle("Battle Joker");
        // arrowKeysImg.
        stage.setScene(new Scene(root));

        //waitButton.setOnAction(event -> OnButtonClickWait());
        //leaveButton.setOnAction(event -> OnButtonClickWait());

        waitButton.setOnMouseClicked(this::OnButtonClickWait);
        leaveButton.setOnMouseClicked(this::OnButtonClickLeave);

        // Show the window
        stage.show();
    }
*/
    @FXML
    void OnButtonClickWait(Event event) {
        gameWindow.log("Client Action: Clicked Wait Button in Wait Or Leave Dialog");
        gameWindow.clearPlayerStat();
        gameWindow.setClickedWaitButton(true);
        stage.close();
    }

    @FXML
    void OnButtonClickLeave(Event event) {
        gameWindow.log("Client Action: Clicked Leave Button in Wait Or Leave Dialog");
        gameWindow.quit();
        //stage.close();
    }
}


