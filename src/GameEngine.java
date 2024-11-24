import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GameEngine {
    // game setting
    // public static final int LIMIT = 14;
    public static final int SIZE = 4;
    private static GameEngine instance;
    private static GameWindow gameWindow;
    final int[] board = new int[SIZE * SIZE];
//    ArrayList<Player> playersList = new ArrayList<>();
    // server stuff
    Thread receiverThread; // child thread for receiving data sent from the server
    Socket clientSocket;
    DataInputStream dataInStream;
    DataOutputStream dataOutStream;
    // Random random = new Random(0);
    Player self = null;
    private String myName = "";
    // private boolean gameOver;

    // private String playerName;
    // private int level = 1;
    // private int score;
    // private int combo;
    // private int totalMoveCount;
    // private int numOfTilesMoved;

    // private final Map<String, Runnable> actionMap = new HashMap<>();

    //Command
    private final char CHECK_IS_STARTED = 'C';
    private final char START_THE_GAME = 'S';
    private final char GET_ARRAY_FROM_SERVER = 'A';
    private final char GET_PLAYER_LIST_FROM_SERVER = 'P';
    private final char GET_REMOVE_PLAYER_FROM_SERVER = 'Q';
    private final char GET_GAMEOVER_FROM_SERVER = 'F';
    private final char CHECK_IS_AWAITING = 'W';
    private final char REQUEST_RESTART_THE_GAME = 'E';
    private final char ACTION_UP = 'U';
    private final char ACTION_DOWN = 'D';
    private final char ACTION_LEFT = 'L';
    private final char ACTION_RIGHT = 'R';
    //Command Map
    private final Map<Character, String> COMMAND_MAP = new HashMap<Character, String>() {{
        put(CHECK_IS_STARTED, "Check is the Game Started?");
        put(START_THE_GAME, "Start the game now");
        put(GET_ARRAY_FROM_SERVER,"Receive Board Array from the server");
        put(GET_PLAYER_LIST_FROM_SERVER,"Receive Player Array List from the server");
        put(GET_REMOVE_PLAYER_FROM_SERVER,"Receive Removed Player from the server");
        put(GET_GAMEOVER_FROM_SERVER,"Receive Gameover from the server");
        put(CHECK_IS_AWAITING,"Check is awaiting from the server");
        put(REQUEST_RESTART_THE_GAME,"Request the server to restart the game");
        put(ACTION_UP,"ACTION_UP");
        put(ACTION_DOWN,"ACTION_DOWN");
        put(ACTION_LEFT,"ACTION_LEFT");
        put(ACTION_RIGHT,"ACTION_RIGHT");
    }};


    private GameEngine(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            dataInStream = new DataInputStream(clientSocket.getInputStream());
            dataOutStream = new DataOutputStream(clientSocket.getOutputStream());

            startReceiverThread();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1); // should not just exit, maybe show a dialog to the user for more options
        }
    }

    public static GameEngine getInstance(String ip, int port) {
        if (instance == null) {
            instance = new GameEngine(ip, port);
        }
        return instance;
    }

    public void sendPlayerName(String playerName) {
        try {
            myName = playerName;
            // send array bytes not UTF as it can be a problem
            byte[] bytes = playerName.getBytes();
            dataOutStream.write(bytes);
            dataOutStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startReceiverThread() throws InterruptedException, IOException {
        receiverThread = new Thread(() -> {
            try {
                while (true) { // handle it later
                    char data = getCharFromServer(); // get direction

                    switch (data) {
                        case GET_ARRAY_FROM_SERVER: // server sent an array
                            receiveArray(dataInStream); // use receiveArray to receive dataInputStream
                            break;
                        case GET_PLAYER_LIST_FROM_SERVER: // server sent arrayList of players
                            receivePlayerList(dataInStream);
                            break;
                        case GET_REMOVE_PLAYER_FROM_SERVER: // server sent remove player (Q = quit)
                            // check if the dateInStream is already receiving the data from the server
                            removePlayer(dataInStream);
                            break;
                        case GET_GAMEOVER_FROM_SERVER: // game is finished
                            log("Client log: GameEngine.startReceiverThread::GameOver");
                            if (getGameWindow()) {
                                gameWindow.setIsGameOver(true);
                            }
                            break;
                        default:
                            //Nothing
                    }
                }
            } catch (IOException ex) { // handle it later
                ex.printStackTrace();
            }
        });
        receiverThread.start();
    }

    private boolean getGameWindow(){
        while (gameWindow == null) {
            try {
                Thread.sleep(100);
                gameWindow = GameWindow.getInstance();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private void removePlayer(DataInputStream in) {
        try {
            /*
            int len = in.readInt();
            byte[] data = new byte[len];
            in.read(data, 0, len);
            String playerName = new String(data, 0, len);
             */
            int len = readIntFromServer(in, "Player name length");
            String playerName = readStringFromServer(in,"playerName",new byte[len],len);

            //log("GameEngine.removePlayer");

//            GameWindow win = null;
//            while (win == null) {
//                Thread.sleep(100);
//                win = GameWindow.getInstance();
//            }
            if (getGameWindow()) {
                gameWindow.removePlayerStat(playerName);
                log("Client action: "+"Removed "+playerName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//    public Player addOrUpdatePlayer(String playerName, boolean isMyTurn, int level, int score, int numberOfMoves, int combo) {
//        // Loop through the players list to find the player
//        for (Player existingPlayer : playersList) {
//            if (existingPlayer.getPlayerName().equals(playerName)) {
//                // Player found, update their attributes
//                existingPlayer.setMyTurn(isMyTurn);
//                existingPlayer.setLevel(level);
//                existingPlayer.setScore(score);
//                existingPlayer.setNumberOfMoves(numberOfMoves);
//                existingPlayer.setCombo(combo);
//                return existingPlayer; // Exit the method after updating
//            }
//        }
//
//        // If the player does not exist, create and add the new player
//        Player player = new Player(playerName, null, level, score, numberOfMoves, combo);
//        player.setMyTurn(isMyTurn);
//        // playersList.add(player);
//        return player;
//    }

    public void receivePlayerList(DataInputStream in) {
        try {
            //int len = in.readInt();
            int len = readIntFromServer(in, "Player name length");
            if(len > 1000){
                try{
                    Thread.sleep(1000);
                } catch (Exception e) {
                    //Do nothing
                }
                //clearInputStream(in);
                getPlayerListRefresh();
                return;
            }
            //byte[] data = new byte[len];
            //in.read(data, 0, len);
            //String playerName = new String(data, 0, len);
            String playerName = readStringFromServer(in,"playerName",new byte[len],len);

            /*
            int level = in.readInt();
            int score = in.readInt();
            int combo = in.readInt();
            int numberOfMoves = in.readInt();
            boolean isMyTurn = in.readBoolean();
            boolean isHost = in.readBoolean();
            */
            int level = readIntFromServer(in,"level");
            int score = readIntFromServer(in,"score");
            int combo = readIntFromServer(in,"combo");
            int numberOfMoves = readIntFromServer(in,"numberOfMoves");
            boolean isMyTurn = readBooleanFromServer(in,"isMyTurn");
            boolean isHost = readBooleanFromServer(in,"isHost");
            boolean isAwaitingPlayer = readBooleanFromServer(in,"isAwaitingPlayer");

            if(level > 1000 || score > 1000 || combo > 1000 || numberOfMoves > 1000){
                clearInputStream(in);
                getPlayerListRefresh();
                return;
            }

            /*
            log("GameEngine.receivePlayerList");
            log("playerName = " + playerName);
            log("level = " + level);
            log("score = " + score);
            log("combo = " + combo);
            log("numberOfMoves = " + numberOfMoves);
            log("isMyTurn = " + isMyTurn);
            log("isHost = " + isHost);
            */

            Player player = new Player(playerName, null, level, score, numberOfMoves, combo);
            player.setMyTurn(isMyTurn);
            player.setHost(isHost);
//            playersList.add(player);

            if (playerName.equals(myName)) {
                if (self == null) {
                    self = player;
                } else {
                    self.setLevel(level);
                    self.setScore(score);
                    self.setNumberOfMoves(numberOfMoves);
                    self.setCombo(combo);
                    self.setMyTurn(isMyTurn);
                    self.setAwaitingPlayer(isAwaitingPlayer);
                }
                if(gameWindow != null) {
                    gameWindow.setIsAwaitingPlayer(isAwaitingPlayer);
                }
            }

            // Player player = addOrUpdatePlayer(playerName, isMyTurn, level, score, numberOfMoves, combo);

//            GameWindow win = null;
//            while (win == null) {
//                Thread.sleep(100);
//                win = GameWindow.getInstance();
//            }
            if (getGameWindow()) {
                gameWindow.insertPlayerStat(player);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void receiveArray(DataInputStream in) throws IOException {
        int size = in.readInt();
        if(size >= 16){
            log("DEBUG BOARD SIZE: "+size);
        }
        String boardStr = "[ ";
        for (int i = 0; i < size; i++) {
            board[i] = in.readInt();
            boardStr += board[i]+" ";
        }
        boardStr += "]";
        log("Received: Board = "+boardStr);
    }

    // /**
    // * Generate a new random value and determine the game status.
    // * @return true if the next round can be started, otherwise false.
    // */
    // private boolean nextRound() {
    // if (isFull()) return false;
    // int i;
    //
    // // randomly find an empty place
    // do {
    // i = random.nextInt(SIZE * SIZE);
    // } while (board[i] > 0);
    //
    // // randomly generate a card based on the existing level, and assign it to the
    // select place
    // board[i] = random.nextInt(level) / 4 + 1;
    // return true;
    // }

    // /**
    // * @return true if all blocks are occupied.
    // */
    // private boolean isFull() {
    // for (int v : board)
    // if (v == 0) return false;
    // return true;
    // }

    /**
     * Move and combine the cards based on the input direction
     *
     * @param dir
     */
    public void moveMerge(String dir) {
        //log(dir);

        //Player self = playersList.stream().filter(p -> p.getPlayerName().equals(myName)).findFirst().get();
        if(self == null){
            log("Client log: GameEngine.moveMerge:: Game haven't started.");
            return;
        }

        if (!self.isMyTurn()) {
            log("Client log: GameEngine.moveMerge::self.isMyTurn() == false");
            return;
        }

        // send the data to the server

        //log("Sending: "+ dir.charAt(0));
        sendToServer(dir.charAt(0));
        //dataOutStream.write(dir.charAt(0));
        //dataOutStream.flush();


        // synchronized (board) {
        // if (actionMap.containsKey(dir)) {
        // combo = numOfTilesMoved = 0;
        //
        // // go to the hash map, find the corresponding method and call it
        // actionMap.get(dir).run();
        //
        // // calculate the new score
        // score += combo / 5 * 2;
        //
        // // determine whether the game is over or not
        // if (numOfTilesMoved > 0) {
        // totalMoveCount++;
        // gameOver = level == LIMIT || !nextRound();
        // } else
        // gameOver = isFull();
        //
        // // update the database if the game is over
        // if (gameOver) {
        // try {
        // Database.putScore(playerName, score, level);
        // } catch (Exception ex) {
        // ex.printStackTrace();
        // }
        // }
        // }
        // }
    }

    // /**
    // * move the values downward and merge them.
    // */
    // private void moveDown() {
    // for (int i = 0; i < SIZE; i++)
    // moveMerge(SIZE, SIZE * (SIZE - 1) + i, i);
    // }
    //
    // /**
    // * move the values upward and merge them.
    // */
    // private void moveUp() {
    // for (int i = 0; i < SIZE; i++)
    // moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i);
    // }
    //
    // /**
    // * move the values rightward and merge them.
    // */
    // private void moveRight() {
    // for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
    // moveMerge(1, SIZE - 1 + i, i);
    // }
    //
    // /**
    // * move the values leftward and merge them.
    // */
    // private void moveLeft() {
    // for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
    // moveMerge(-1, i, SIZE - 1 + i);
    // }

    // /**
    // * Move and merge the values in a specific row or column. The moving direction
    // and the specific row or column is determined by d, s, and l.
    // * @param d - move distance
    // * @param s - the index of the first element in the row or column
    // * @param l - the index of the last element in the row or column.
    // */
    // private void moveMerge(int d, int s, int l) {
    // int v, j;
    // for (int i = s - d; i != l - d; i -= d) {
    // j = i;
    // if (board[j] <= 0) continue;
    // v = board[j];
    // board[j] = 0;
    // while (j + d != s && board[j + d] == 0)
    // j += d;
    //
    // if (board[j + d] == 0) {
    // j += d;
    // board[j] = v;
    // } else {
    // while (j != s && board[j + d] == v) {
    // j += d;
    // board[j] = 0;
    // v++;
    // score++;
    // combo++;
    // }
    // board[j] = v;
    // if (v > level) level = v;
    // }
    // if (i != j)
    // numOfTilesMoved++;
    //
    // }
    // }

    public int getValue(int r, int c) {
        synchronized (board) {
            String boardStr = "DEBUG Board = [ ";
            if(board[r * SIZE + c] >= 16){
                for(int i = 0; i < board.length; i++){
                    boardStr += board[i]+" ";
                }
                boardStr += "]";
                log("***DEBUG BOARD VALUE = "+boardStr);
            }
            return board[r * SIZE + c];
        }
    }
    //
    // public boolean isGameOver() {
    // return gameOver;
    // }
    //
    // public void setPlayerName(String name) {
    // playerName = name;
    // }
    //
    // public int getScore() {
    // return score;
    // }
    //
    // public int getCombo() {
    // return combo;
    // }
    //
    // public int getLevel() {
    // return level;
    // }
    //
    // public int getMoveCount() {
    // return totalMoveCount;
    // }

    //20241122 Melody updated - Start
    public void gameStart() {
        // send the data to the server

            //dataOutStream.write('S');
            //dataOutStream.flush();
        sendToServer(START_THE_GAME);

    }

    public boolean checkGameStart() {
        // send the data to the server
        clearInputStream(dataInStream);
        sendToServer(CHECK_IS_STARTED);
        boolean response = readBooleanFromServer(dataInStream,"isGameStarted");
        return response;
    }

    public boolean checkIsAwaiting() {
        // send the data to the server
        clearInputStream(dataInStream);
        sendToServer(CHECK_IS_AWAITING);
        boolean response = readBooleanFromServer(dataInStream,"isAwaiting");
        return response;
    }



    public void getPlayerListRefresh() {
        // send the data to the server
        sendToServer(GET_PLAYER_LIST_FROM_SERVER);
    }

    private void clearInputStream(DataInputStream in){

            try {
                while (in.available() > 0) {
                    in.skipBytes(in.available());
                }
            } catch (IOException e) {
            throw new RuntimeException(e);
        }


        /*
        try {
            // 跳過所有剩餘的字節
            in.skip(in.available());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
*/

    }

    private void clearInputStream(){
        try {
            dataInStream.close();
            dataInStream = new DataInputStream(clientSocket.getInputStream());
        }catch (Exception e){
            //Do nothing
        }
    }

    private void sendToServer(char msg){
        try {
            log("Sending: "+msg+" ("+COMMAND_MAP.get(msg)+")");
            dataOutStream.write(msg);
            dataOutStream.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+COMMAND_MAP.get(msg)+")");
        }
    }

    private char getCharFromServer(){
        char result = '\0';
        try {
            result = (char) dataInStream.read();
            log("Received: "+result+" ("+COMMAND_MAP.get(result)+")");
            return result;
        }catch (IOException e){
            log("Failed to get char from server");
        }
        return result;
    }

    private int readIntFromServer(DataInputStream in, String dataName){
        int result = -1;
        try {
            result = in.readInt();
            log("Received: "+result+" ("+dataName+")");
            return result;
        }catch (IOException e){
            log(getErrorMsgByData(dataName));
        }
        return result;
    }

    private boolean readBooleanFromServer(DataInputStream in, String dataName){
        boolean result = false;
        try {
            result = in.readBoolean();
            log("Received: "+result+" ("+dataName+")");
            return result;
        }catch (IOException e){
            log(getErrorMsgByData(dataName));
        }
        return result;
    }

    private String readStringFromServer(DataInputStream in, String dataName,byte[] data, int len){
        String result = "";
        try {
            //in.read(data, 0, len);
            in.readFully(data, 0, len);
            //result = new String(data, 0, len);
            result = new String(data, 0, len, StandardCharsets.UTF_8);
            log("Received: "+result+" ("+dataName+")");
            return result;
        }catch (IOException e){
            log(getErrorMsgByData(dataName));
        }
        return result;
    }



    public void gameRestart() {
        //log("Try to restart the game!");

        // send the data to the server

        sendToServer(REQUEST_RESTART_THE_GAME);
            //dataOutStream.write('E');
            //dataOutStream.flush();

    }

    public void log(String msg){
        // Get current datetime
        String dateTime = getCurrentDateTimeStr();
        // Print log
        String logMessage = dateTime + " - " + msg;
        System.out.println(logMessage);
    }

    public void log(char msg){
        log(""+msg);
    }

    private String getCurrentDateTimeStr(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTime = sdf.format(new Date());
        return dateTime;
    }

    private String getErrorMsgByData(String dataName){
        return "Failed to get "+dataName+" from server";
    }

    //20241122 Melody updated - End

    /*
    public void getCurrentPlayerCount() {
        log("Try to get current player count");
        // send the data to the server
        try {
            dataOutStream.write('P');
            dataOutStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    */
}
