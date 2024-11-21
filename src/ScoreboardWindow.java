import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.event.Event;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class ScoreboardWindow {
    Stage stage;

    @FXML
    ListView<String> scoreList;

    @FXML
    Button showTop10;


    public ScoreboardWindow(ArrayList<Player> playerList) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("scoreUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        setFont(14);
        scoreList.getItems().clear();
        showRecentGame(playerList);
        // updateList();
        showTop10.setOnMouseClicked(this::OnButtonClick);

        stage.showAndWait();
    }

    private void OnButtonClick(Event event) {
        updateList();
        showTop10.setVisible(false);
    }

    public void showRecentGame(ArrayList<Player> playerList) {
        try {
            ObservableList<String> items = FXCollections.observableArrayList();
            playerList.forEach(data->{
                String scoreStr = String.format("%s (%s)", data.getScore(), data.getLevel());
                items.add(String.format("%10s | %10s", data.getPlayerName(), scoreStr));
            });
            scoreList.setItems(items);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }


    private void setFont(int fontSize) {
        scoreList.setCellFactory(param -> {
            TextFieldListCell<String> cell = new TextFieldListCell<>();
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                cell.setFont(Font.font("Courier New", fontSize));
            } else if (osName.contains("mac")) {
                cell.setFont(Font.font("Menlo", fontSize));
            } else {
                cell.setFont(Font.font("Monospaced", fontSize));
            }
            return cell;
        });
    }

    private void updateList() {
        try {
            ObservableList<String> items = FXCollections.observableArrayList();
            Database.getScores().forEach(data->{
                String scoreStr = String.format("%s (%s)", data.get("score"), data.get("level"));
                items.add(String.format("%10s | %10s | %s", data.get("name"), scoreStr, data.get("time").substring(0, 16)));
            });
            scoreList.setItems(items);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
