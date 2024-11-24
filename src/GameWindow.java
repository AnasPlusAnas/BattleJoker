import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class GameWindow {
    private static GameWindow instance;
    final String imagePath = "images/";
    final String[] symbols = {"bg", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "Joker"};
    final Image[] images = new Image[symbols.length];
    private final GameEngine gameEngine;
    @FXML
    MenuBar menuBar;
    @FXML
    Pane boardPane;
    @FXML
    Canvas canvas;
    @FXML
    HBox playerContainer;
    @FXML
    MenuItem menuItemInstructions;
    @FXML
    MenuItem startButton;


    Stage stage;
    AnimationTimer animationTimer;
    private boolean isGameOver;
    private final ArrayList<Player> playerArrayList = new ArrayList<>();
    private ArrayList<Player> lastPlayerArrayList = new ArrayList<>();
    private int currentPlayerCount = 0;
    private boolean isGameStart;
    private boolean isAwaitingPlayer = false;
    private boolean clickedWaitButton = false;
    private String playerName="";

    private GameWindow(Stage stage, String ip, int port, String playerName) throws IOException {
        loadImages();

        // Initialize GameEngine with the provided IP and port
        gameEngine = GameEngine.getInstance(ip, port);
        // nameLabel.setText(playerName);
        gameEngine.sendPlayerName(playerName);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        this.stage = stage;
        this.playerName = playerName;

        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        stage.widthProperty().addListener(w -> onWidthChangedWindow(((ReadOnlyDoubleProperty) w).getValue()));
        stage.heightProperty().addListener(h -> onHeightChangedWindow(((ReadOnlyDoubleProperty) h).getValue()));
        stage.setOnCloseRequest(event -> quit());

        menuItemInstructions.setOnAction(event -> handleInstructions());
        //20241122 Melody updated - Start
        startButton.setOnAction(event -> startGame());
        //20241122 Melody updated - End
        stage.show();

        //TODO: Handle awaiting player
        isGameStart = gameEngine.checkGameStart();
        boolean isAwaitingPlayer = gameEngine.checkIsAwaiting();

        initCanvas();
        gameStart();


/*
            if(!isGameStart){
                gameEngine.getPlayerListRefresh();
            }
*/
        gameEngine.getPlayerListRefresh();
        if((isGameStart && isAwaitingPlayer) || playerArrayList.size() > 4){
            WaitOrLeaveDialog waitOrLeaveDialog = new WaitOrLeaveDialog(this);
        }

    }



    public static GameWindow getInstance(Stage stage, String ip, int port, String playerName) throws IOException {
        if (instance == null) {
            instance = new GameWindow(stage, ip, port, playerName);
        }
        return instance;
    }

    public static GameWindow getInstance() throws IOException {
        if (instance == null) {
            return null;
        }
        return instance;
    }

    //20241122 Melody updated - Start
    public void startGame(){
        //initCanvas();
        gameEngine.gameStart();
        gameEngine.getPlayerListRefresh();
        //gameStart();
    }

    /*
    public boolean checkGameStart(){
        gameEngine.
    }

     */

    //20241122 Melody updated - End

    public void handleInstructions() {
        HowToPlayWindow howToPlayWindow = new HowToPlayWindow();
        try {
            howToPlayWindow.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setIsGameOver(boolean isOver) {
        isGameOver = isOver;
    }

    private void gameStart() {
        animationTimer.start();
        isGameOver = false;
    }

    private void loadImages() throws IOException {
        for (int i = 0; i < symbols.length; i++)
            images[i] = new Image(Files.newInputStream(Paths.get(imagePath + symbols[i] + ".png")));
    }

    public void insertPlayerStat(Player player) {
        Platform.runLater(() -> {
            // Iterate through the children of playerContainer
            for (int i = 0; i < playerContainer.getChildren().size(); i++) {
                VBox statContainer = (VBox) playerContainer.getChildren().get(i);
                Label nameLabel = null;
                Label scoreLabel = null;
                Label levelLabel = null;
                Label comboLabel = null;
                Label moveCountLabel = null;

                // Find all relevant labels in the statContainer
                for (Node child : statContainer.getChildren()) {
                    if (child instanceof Label) {
                        Label label = (Label) child;
                        if (label.getText().equals(player.getPlayerName())) {
                            nameLabel = label;
                        } else if (label.getText().startsWith("Score")) {
                            scoreLabel = label;
                        } else if (label.getText().startsWith("Level")) {
                            levelLabel = label;
                        } else if (label.getText().startsWith("Combo")) {
                            comboLabel = label;
                        } else if (label.getText().startsWith("# of Moves")) {
                            moveCountLabel = label;
                        }
                    }
                }

                // If the nameLabel was found, update the player's stats
                if (nameLabel != null) {
                    // Update the labels with the player's stats
                    if (scoreLabel != null) {
                        scoreLabel.setText("Score: " + player.getScore());
                    }
                    if (levelLabel != null) {
                        levelLabel.setText("Level: " + player.getLevel());
                    }
                    if (comboLabel != null) {
                        comboLabel.setText("Combo: " + player.getCombo());
                    }
                    if (moveCountLabel != null) {
                        moveCountLabel.setText("# of Moves: " + player.getNumberOfMoves());
                    }
                    Player pToUpdate = playerArrayList.stream().filter(p -> Objects.equals(p.getPlayerName(), player.getPlayerName())).findFirst().get();

                    pToUpdate.setScore(player.getScore());
                    pToUpdate.setCombo(player.getCombo());
                    pToUpdate.setNumberOfMoves(player.getNumberOfMoves());
                    pToUpdate.setLevel(player.getLevel());

                    updatePlayerTurn(player);

                    return; // Player found and updated, exit method
                }
            }

            VBox statContainer = new VBox();

            // name
            Label nameLabel = new Label(player.getPlayerName());
            nameLabel.setTextAlignment(TextAlignment.CENTER);
            nameLabel.setFont(Font.font("Impact", 20.0));

            // score
            Label scoreLabel = new Label("Score: " + player.getScore());
            scoreLabel.setTextAlignment(TextAlignment.CENTER);

            // level
            Label levelLabel = new Label("Level: " + player.getLevel());
            levelLabel.setTextAlignment(TextAlignment.CENTER);

            // combo
            Label comboLabel = new Label("Combo: " + player.getCombo());
            comboLabel.setTextAlignment(TextAlignment.CENTER);

            // move count
            Label moveCountLabel = new Label("# of Moves: " + player.getNumberOfMoves());
            moveCountLabel.setTextAlignment(TextAlignment.CENTER);

            // add all labels in the vbox
            statContainer.getChildren().addAll(nameLabel, scoreLabel, levelLabel, comboLabel, moveCountLabel);
            statContainer.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));

            playerContainer.getChildren().add(statContainer);
            if(!player.isAwaitingPlayer()){
                boolean isExistPlayer = false;
                for(int i = 0; i < playerArrayList.size(); i++){
                    Player tempPlayer = playerArrayList.get(i);
                    if(tempPlayer.getPlayerName().equalsIgnoreCase(player.getPlayerName())){
                        isExistPlayer = true;
                        break;
                    }
                }
                if(!isExistPlayer){
                    playerArrayList.add(player);
                }

            }
            if(playerArrayList.size() > currentPlayerCount){
                currentPlayerCount = playerArrayList.size();
            }
            updatePlayerTurn(player);
        });
    }

    public void removePlayerStat(String playerName) {
        Player playerToRemove = playerArrayList.stream().filter(player -> player.getPlayerName().equals(playerName)).findFirst().get();
        playerArrayList.remove(playerToRemove);

        for (int i = 0; i < playerContainer.getChildren().size(); i++) {
            VBox statContainer = (VBox) playerContainer.getChildren().get(i);

            // Find all relevant labels in the statContainer
            for (Node child : statContainer.getChildren()) {
                if (child instanceof Label) {
                    Label label = (Label) child;
                    if (label.getText().equals(playerName)) {
                        // remove the child from the parent
                        Platform.runLater(() -> playerContainer.getChildren().remove(statContainer));
                    }
                }
            }
        }
    }

    public void clearPlayerStat() {
        for(int i = 0; i < playerArrayList.size(); i++){
            Player player = playerArrayList.get(i);
            removePlayerStat(player.getPlayerName());
        }

        for(int i = 0; i < playerArrayList.size(); i++){
            playerArrayList.remove(i);
        }

        playerContainer.getChildren().clear();

        gameEngine.getPlayerListRefresh();
    }

    public void refreshPlayerStat(){
        for(int i = 0; i < playerArrayList.size(); i++){
            updatePlayerTurn(playerArrayList.get(i));
        }
    }

    public void updatePlayerTurn(Player player) {
        Platform.runLater(() -> {
            for (int i = 0; i < playerContainer.getChildren().size(); i++) {
                VBox statContainer = (VBox) playerContainer.getChildren().get(i);

                // Find all relevant labels in the statContainer
                for (Node child : statContainer.getChildren()) {
                    if (child instanceof Label) {
                        Label label = (Label) child;
                        if (label.getText().equals(player.getPlayerName())) {
                            // remove the child from the parent
                            if (player.isMyTurn()) {
                                // Highlight this player's turn
                                label.setStyle("-fx-font-weight: bold; -fx-text-fill: green;"); // Example style for current player
                            } else {
                                // Reset style for other players
                                label.setStyle("-fx-font-weight: normal; -fx-text-fill: black;");
                            }
                        }
                    }
                }
            }
        });
    }

    private void initCanvas() {
        canvas.setOnKeyPressed(event -> {
            if(isAwaitingPlayer){
                return;
            }
            switch (event.getCode()) {
                case UP:
                    gameEngine.moveMerge("UP");
                    break;
                case DOWN:
                    gameEngine.moveMerge("DOWN");
                    break;
                case LEFT:
                    gameEngine.moveMerge("LEFT");
                    break;
                case RIGHT:
                    gameEngine.moveMerge("RIGHT");
                    break;
                case N:
                    gameEngine.moveMerge("N");
                    break;
                case Z:
                    gameEngine.moveMerge("Z");
                    break;
            }
            //gameEngine.moveMerge(event.getCode().toString());

            // scoreLabel.setText("Score: " + gameEngine.getScore());
            // levelLabel.setText("Level: " + gameEngine.getLevel());
            // comboLabel.setText("Combo: " + gameEngine.getCombo());
            // moveCountLabel.setText("# of Moves: " + gameEngine.getMoveCount());
        });

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();

                if (isGameOver) {
                    log("Client log: Game Over!");
                    animationTimer.stop();

                    Platform.runLater(() -> {
                        try {
                            //for(int i = 0; i < playerArrayList.size(); i++){
                              //  if(playerArrayList.get(i).getPlayerName().equalsIgnoreCase(playerName) && !clickedWaitButton){
                                    new ScoreboardWindow(playerArrayList);
                              //  }
                          //  }

                            //if(playerArrayList.size() > 4) {

                            //}


                            gameEngine.gameRestart();
                            gameStart();
                            try{
                                Thread.sleep(100);
                            }catch (Exception e){
                                //Do nothing
                            }
                            //gameEngine.getPlayerListRefresh();

                            clickedWaitButton = false;
                            refreshPlayerStat();
                            clearPlayerStat();

                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }

                //updatePlayerTurn();
            }
        };
        canvas.requestFocus();
    }

    private void render() {

        double w = canvas.getWidth();
        double h = canvas.getHeight();

        double sceneSize = Math.min(w, h);
        double blockSize = sceneSize / GameEngine.SIZE;
        double padding = blockSize * .05;
        double startX = (w - sceneSize) / 2;
        double startY = (h - sceneSize) / 2;
        double cardSize = blockSize - (padding * 2);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double y = startY;
        int v;

        // Draw the background and cards from left to right, and top to bottom.
        for (int i = 0; i < GameEngine.SIZE; i++) {
            double x = startX;
            for (int j = 0; j < GameEngine.SIZE; j++) {
                gc.drawImage(images[0], x, y, blockSize, blockSize); // Draw the background

                v = gameEngine.getValue(i, j);

                //if (v > 0) // if a card is in the place, draw it
                if (v >= 0 && v < images.length) {
                    gc.drawImage(images[v], x + padding, y + padding, cardSize, cardSize);
                }else{
                    log("Client error: gc.drawImage.images[v] out of bound, i=" + i + ", j=" + j + ", v=" + v);
                }

                x += blockSize;
            }
            y += blockSize;
        }
    }

    void onWidthChangedWindow(double w) {
        double width = w - boardPane.getBoundsInParent().getMinX();
        boardPane.setMinWidth(width);
        canvas.setWidth(width);
        render();
    }

    void onHeightChangedWindow(double h) {
        double height = h - boardPane.getBoundsInParent().getMinY() - menuBar.getHeight();
        boardPane.setMinHeight(height);
        canvas.setHeight(height);
        render();
    }

    void quit() {
        //log("Bye bye");
        log("Client log: Disconnected");
        stage.close();
        System.exit(0);
    }

    public void setName(String name) {
        // nameLabel.setText(name);
        // gameEngine.setPlayerName(name);
    }

    public void setIsAwaitingPlayer(boolean isAwaitingPlayer){
        this.isAwaitingPlayer = isAwaitingPlayer;
    }

    public boolean getIsGameStarted(){
        return this.isGameStart;
    }

    //2024-11-23 Melody update - Start

    private String getCurrentDateTimeStr(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTime = sdf.format(new Date());
        return dateTime;
    }

    public void setClickedWaitButton(boolean clickedWaitButton){
        this.clickedWaitButton = clickedWaitButton;
    }

    public void log(String msg){
        gameEngine.log(msg);
    }

    public void log(char msg){
        gameEngine.log(msg);
    }
    //2024-11-23 Melody update - End


}
