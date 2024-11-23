import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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
                    char data = (char) dataInStream.read(); // get direction
                    System.out.println(data);

                    switch (data) {
                        case 'A': // server sent an array
                            receiveArray(dataInStream); // use receiveArray to receive dataInputStream
                            break;
                        case 'P': // server sent arrayList of players
                            receivePlayerList(dataInStream);
                            break;
                        case 'Q': // server sent remove player (Q = quit)
                            // check if the dateInStream is already receiving the data from the server
                            removePlayer(dataInStream);
                            break;
                        case 'F': // game is finished
                            System.out.println("GameEngine.startReceiverThread::GameOver");
                            if (getGameWindow()) {
                                gameWindow.setIsGameOver(true);
                            }
                            break;
                        default:
                            System.out.println(data);
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
            int len = in.readInt();
            byte[] data = new byte[len];
            in.read(data, 0, len);
            String playerName = new String(data, 0, len);

            System.out.println("GameEngine.removePlayer");

//            GameWindow win = null;
//            while (win == null) {
//                Thread.sleep(100);
//                win = GameWindow.getInstance();
//            }
            if (getGameWindow()) {
                gameWindow.removePlayerStat(playerName);
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
            int len = in.readInt();
            byte[] data = new byte[len];
            in.read(data, 0, len);
            String playerName = new String(data, 0, len);

            int level = in.readInt();
            int score = in.readInt();
            int combo = in.readInt();
            int numberOfMoves = in.readInt();
            boolean isMyTurn = in.readBoolean();
            boolean isHost = in.readBoolean();

            System.out.println("GameEngine.receivePlayerList");
            System.out.println("playerName = " + playerName);
            System.out.println("level = " + level);
            System.out.println("score = " + score);
            System.out.println("combo = " + combo);
            System.out.println("numberOfMoves = " + numberOfMoves);
            System.out.println("isMyTurn = " + isMyTurn);
            System.out.println("isHost = " + isHost);

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
        for (int i = 0; i < size; i++) {
            board[i] = in.readInt();
        }
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
        System.out.println(dir);

        //Player self = playersList.stream().filter(p -> p.getPlayerName().equals(myName)).findFirst().get();
        if(self == null){
            System.out.println("GameEngine.moveMerge:: Game haven't started.");
            return;
        }

        if (!self.isMyTurn()) {
            System.out.println("GameEngine.moveMerge:: IS MY TURN == false");
            return;
        }

        // send the data to the server
        try {
            dataOutStream.write(dir.charAt(0));
            dataOutStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        System.out.println("Try to start the game!");

        // send the data to the server
        try {
            dataOutStream.write('S');
            dataOutStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkGameStart() {
        System.out.println("Is Game Started?");
        boolean response = false;
        // send the data to the server
        try {
            //Before
            System.out.println("1"+dataInStream.toString());
            clearInputStream(dataInStream);
            //After
            System.out.println("2"+dataInStream.toString());
            dataOutStream.write('C');
            dataOutStream.flush();

            response = dataInStream.readBoolean();
            System.out.println("***"+response);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    private static void clearInputStream(DataInputStream in) throws IOException {
        while (in.available() > 0) {
            in.skipBytes(in.available());
        }
    }

    //20241122 Melody updated - End

    /*
    public void getCurrentPlayerCount() {
        System.out.println("Try to get current player count");
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
