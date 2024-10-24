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

public class GetNameDialog {
    @FXML
    TextField nameField, ipField, portField;

    @FXML
    Label errorLabel;

    @FXML
    Button goButton;

    @FXML
    GridPane getNameDialogPane;

    Stage stage;
    String playername;
    String ip;
    String port;

    public GetNameDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("getNameUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        getNameDialogPane.setPadding(new Insets(10, 10, 10, 10));

        goButton.setOnMouseClicked(this::OnButtonClick);

        stage.showAndWait();
    }

    @FXML
    void OnButtonClick(Event event) {
        playername = nameField.getText().trim();
        ip = ipField.getText().trim();
        port = portField.getText().trim();

        if (!playername.isEmpty() && !ip.isEmpty() && !port.isEmpty()) {
            // add regex to check if ip is a valid ipv4 address
            if (!ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                errorLabel.setText("Invalid IP address");
                return;
            }

            // Validate if port is an integer
            try {
                int portInt = Integer.parseInt(port);
                // Optionally check if the port is within a valid range (e.g., 1-65535)
                if (portInt < 1 || portInt > 65535) {
                    errorLabel.setText("Port must be between 1 and 65535.");
                    return;
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Invalid port number.");
                return;
            }

            stage.close();
        }
        errorLabel.setText("One or more input(s) are empty.");
    }

    public String getPlayername() {
        return playername;
    }
}
