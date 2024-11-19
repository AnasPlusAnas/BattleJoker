import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class HowToPlayWindow {

    @FXML
    Image arrowKeysImg;

    public void show() throws IOException {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("howToPlay.fxml"));
        Parent root = loader.load();

        // Create a new Stage (window)
        Stage howTostage = new Stage();
        howTostage.initModality(Modality.APPLICATION_MODAL); // Make it modal
        howTostage.setTitle("How to Play");
       // arrowKeysImg.
                howTostage.setScene(new Scene(root));

        // Show the window
        howTostage.show();
    }
}