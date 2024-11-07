import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;

public class GameEngine {
    // server stuff
    Thread receiverThread; // child thread for receiving data sent from the server
    Socket clientSocket;
    DataInputStream dataInStream;
    DataOutputStream dataOutStream;

    // game setting
//    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
//    Random random = new Random(0);

    private static GameEngine instance;
//    private boolean gameOver;

//    private String playerName;
//    private int level = 1;
//    private int score;
//    private int combo;
//    private int totalMoveCount;
//    private int numOfTilesMoved;

//    private final Map<String, Runnable> actionMap = new HashMap<>();

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

    public void sendPlayerName(String playerName) {
        try {
            dataOutStream.writeUTF(playerName);
            dataOutStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startReceiverThread() {
        receiverThread = new Thread(()->{
            try{
                while(true){ // handle it later
                    char data = (char)dataInStream.read(); // get direction
                    System.out.println(data);

                    switch(data){
                        case 'A': // server sent an array
                            receiveArray(dataInStream); // use receiveArray to receive dataInputStream
                            break;
                        case 'P': // server sent arrayList of players
                            receivePlayerList(dataInStream);
                            break;
                        default:
                            System.out.println(data);
                    }
                }
            } catch(IOException ex){ // handle it later
                ex.printStackTrace();
            }
        });
        receiverThread.start();
    }

    public void receivePlayerList(DataInputStream in) throws IOException {
    }

    public void receiveArray(DataInputStream in) throws IOException {
        int size = in.readInt();
        for(int i = 0; i<size; i++){
            board[i] = in.readInt();
        }
    }

    public static GameEngine getInstance(String ip, int port) {
        if (instance == null) {
            instance = new GameEngine(ip, port);
        }
        return instance;
    }

//    /**
//     * Generate a new random value and determine the game status.
//     * @return true if the next round can be started, otherwise false.
//     */
//    private boolean nextRound() {
//        if (isFull()) return false;
//        int i;
//
//        // randomly find an empty place
//        do {
//            i = random.nextInt(SIZE * SIZE);
//        } while (board[i] > 0);
//
//        // randomly generate a card based on the existing level, and assign it to the select place
//        board[i] = random.nextInt(level) / 4 + 1;
//        return true;
//    }

//    /**
//     * @return true if all blocks are occupied.
//     */
//    private boolean isFull() {
//        for (int v : board)
//            if (v == 0) return false;
//        return true;
//    }

    /**
     * Move and combine the cards based on the input direction
     * @param dir
     */
    public void moveMerge(String dir) {
        System.out.println(dir);

        // send the data to the server
        try {
            dataOutStream.write(dir.charAt(0));
            dataOutStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        synchronized (board) {
//            if (actionMap.containsKey(dir)) {
//                combo = numOfTilesMoved = 0;
//
//                // go to the hash map, find the corresponding method and call it
//                actionMap.get(dir).run();
//
//                // calculate the new score
//                score += combo / 5 * 2;
//
//                // determine whether the game is over or not
//                if (numOfTilesMoved > 0) {
//                    totalMoveCount++;
//                    gameOver = level == LIMIT || !nextRound();
//                } else
//                    gameOver = isFull();
//
//                // update the database if the game is over
//                if (gameOver) {
//                    try {
//                        Database.putScore(playerName, score, level);
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
//        }
    }

//    /**
//     * move the values downward and merge them.
//     */
//    private void moveDown() {
//        for (int i = 0; i < SIZE; i++)
//            moveMerge(SIZE, SIZE * (SIZE - 1) + i, i);
//    }
//
//    /**
//     * move the values upward and merge them.
//     */
//    private void moveUp() {
//        for (int i = 0; i < SIZE; i++)
//            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i);
//    }
//
//    /**
//     * move the values rightward and merge them.
//     */
//    private void moveRight() {
//        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
//            moveMerge(1, SIZE - 1 + i, i);
//    }
//
//    /**
//     * move the values leftward and merge them.
//     */
//    private void moveLeft() {
//        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
//            moveMerge(-1, i, SIZE - 1 + i);
//    }

//    /**
//     * Move and merge the values in a specific row or column. The moving direction and the specific row or column is determined by d, s, and l.
//     * @param d - move distance
//     * @param s - the index of the first element in the row or column
//     * @param l - the index of the last element in the row or column.
//     */
//    private void moveMerge(int d, int s, int l) {
//        int v, j;
//        for (int i = s - d; i != l - d; i -= d) {
//            j = i;
//            if (board[j] <= 0) continue;
//            v = board[j];
//            board[j] = 0;
//            while (j + d != s && board[j + d] == 0)
//                j += d;
//
//            if (board[j + d] == 0) {
//                j += d;
//                board[j] = v;
//            } else {
//                while (j != s && board[j + d] == v) {
//                    j += d;
//                    board[j] = 0;
//                    v++;
//                    score++;
//                    combo++;
//                }
//                board[j] = v;
//                if (v > level) level = v;
//            }
//            if (i != j)
//                numOfTilesMoved++;
//
//        }
//    }

    public int getValue(int r, int c) {
        synchronized (board) {
            return board[r * SIZE + c];
        }
    }
//
//    public boolean isGameOver() {
//        return gameOver;
//    }
//
//    public void setPlayerName(String name) {
//        playerName = name;
//    }
//
//    public int getScore() {
//        return score;
//    }
//
//    public int getCombo() {
//        return combo;
//    }
//
//    public int getLevel() {
//        return level;
//    }
//
//    public int getMoveCount() {
//        return totalMoveCount;
//    }
}
