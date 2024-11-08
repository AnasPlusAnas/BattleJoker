import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class JokerServer {
    private final ArrayList<Socket> clientList = new ArrayList<>(); // remember all the client
    private final ArrayList<Player> playerList = new ArrayList<>(); // remember all the client

    // client
    // game setting
    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    Random random = new Random(0);

    private static GameEngine instance;
    private boolean gameOver;

    private String playerName;
    private int level = 1;
    private int score;
    private int combo;
    private int totalMoveCount;
    private int numOfTilesMoved;
    private Player currentPlayer = null;

    final int[] clonedBoard = new int[SIZE * SIZE]; // for undo method
    private final Map<String, Runnable> actionMap = new HashMap<>();

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public JokerServer(int port) throws IOException {
        // define a hash map to contain the links from the actions to the corresponding
        // methods
        actionMap.put("U", this::moveUp);
        actionMap.put("D", this::moveDown);
        actionMap.put("L", this::moveLeft);
        actionMap.put("R", this::moveRight);

        // start the first round
        nextRound();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) { // Allow multiple connections
                Socket clientSocket = serverSocket.accept(); // client successfully connect to the server

                synchronized (clientList) {
                    clientList.add(clientSocket); // add the client socket into the clientList
                    System.out.println("clientSocket:" + clientSocket.toString());
                    System.out.println("clientSocket:" + clientSocket.toString());
                }

                Thread t = new Thread(() -> {
                    try {
                        serve(clientSocket); // try if generate exception (client disconnect to the server)
                    } catch (IOException e) {
                        // e.printStackTrace();
                        System.out.println("The connection was dropped by the client! "
                                + clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort());

                        synchronized (clientList) {
                            clientList.remove(clientSocket); // remove the client from the socketList

                            Player playerToRemove = playerList.stream().filter(p -> p.getSocket() == clientSocket)
                                    .findFirst()
                                    .orElse(null);

                            synchronized (playerList) {
                                playerList.remove(playerToRemove);

                                try {
                                    sendRemovedPlayer(playerToRemove);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }
                    }
                });
                t.start();
            }
        }
    }

    private void serve(Socket clientSocket) throws IOException {
        // proof client connect successfully
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());

        // start receiving the moves from client. (GameEngine.java, moveMerge())
        BufferedInputStream bufferedInputStream = new BufferedInputStream(clientSocket.getInputStream());
        DataInputStream inputStream = new DataInputStream(bufferedInputStream);
        // client side sends array of bytes, so we need to read the array of bytes and
        // convert it to string
        byte[] bytes = new byte[1024];
        int len = inputStream.read(bytes);
        String playerName = new String(bytes, 0, len);

        // create a new player object and add it to the playerList
        Player player = new Player(playerName, clientSocket, 0, 0, 0, 0);

        // add the player to the playerList
        synchronized (playerList) {
            playerList.add(player);
        }


        // send out the updated version of puzzle board  to the client
        DataOutputStream _out = new DataOutputStream(clientSocket.getOutputStream());
        synchronized (clientList) {
            sendPuzzle(_out);
            sendPlayerList();
        }

        while (true) {
            char direction = (char) inputStream.read();
            System.out.println(
                    clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort() + "= " + direction + "\n*** "+ clientSocket);

            currentPlayer = getCurrentPlayerBySocket(clientSocket);

            if(currentPlayer == null){
                continue;
            }

            if(Character.toLowerCase(direction) != 'u' &&
                    Character.toLowerCase(direction) != 'd' &&
                    Character.toLowerCase(direction) != 'l' &&
                    Character.toLowerCase(direction) != 'r' &&
                    Character.toLowerCase(direction) != 'n') { // for undo
                continue;
            }


            if(Character.toLowerCase(direction) == 'n') {
                if(currentPlayer.getUndoFlag()) {
                    undoPuzzle();
                    currentPlayer.setUndoFlag(false);
                }
            } else{
                cloneBoard(); // cloned the board before move
                moveMerge("" + direction);
            }




            // if nextRound = true then the game is still not over, else the game is over
            // gameOver = !nextRound();

            // update the player stats
            player.setNumberOfMoves(player.getNumberOfMoves() + 1);

            // send the move back to other clients connected
            // assert clientList != null;
            synchronized (clientList) {
                for (Socket socket : clientList) {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // get the output stream from
                                                                                           // the socket
                    out.write(direction); // send data to the outputSteam
                    out.flush(); // force to send out the data immediately

                    sendPuzzle(out);
                }
            }

            updatePlayerStat(player);
        }
    }

    private void sendRemovedPlayer(Player player) throws IOException {
        for (Socket client : clientList) {
            System.out.println("JokerServer.sendRemovedPlayer");
            System.out.println("player = " + player.getPlayerName());

            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            out.write('R');
            byte[] bytes = player.getPlayerName().getBytes();
            out.writeInt(bytes.length);
            out.write(bytes);

            out.flush();
        }
    }

    private void updatePlayerStat(Player player) {
        synchronized (playerList) {
            for (Player p : playerList) {
                if (p.getPlayerName().equals(player.getPlayerName())) {
                    p.setLevel(player.getLevel());
                    p.setScore(player.getScore());
                    p.setNumberOfMoves(player.getNumberOfMoves());
                    p.setCombo(player.getCombo());
                }
            }

            // send the updated player list to all clients
            try {
                sendPlayerList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPlayerList() throws IOException {
        for (Socket client : clientList) {
            System.out.println("JokerServer.sendPlayerList");

            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            for (Player player : playerList) {
                out.write('P');
                byte[] bytes = player.getPlayerName().getBytes();
                out.writeInt(bytes.length);
                out.write(bytes);

                out.writeInt(player.getLevel());
                out.writeInt(player.getScore());
                out.writeInt(player.getCombo());
                out.writeInt(player.getNumberOfMoves());
                out.flush();
            }
        }
    }

    public void undoPuzzle(){
        //board = clonedBoard.clone();
        System.arraycopy(clonedBoard,0,board,0, board.length);
    }
    public void cloneBoard(){
        //clonedBoard = board.clone();
        System.arraycopy(board,0,clonedBoard,0, board.length);
    }
    public void sendPuzzle(DataOutputStream out) throws IOException { // handle later
        out.write('A'); // going to send out an array to client
        out.writeInt(board.length); // size of array
        for (int i : board) {
            out.writeInt(i); // send the value to the array
        }
        out.flush(); // force java to send the data out
        // try {
        // Thread.sleep(1000);
        // } catch (InterruptedException e) {
        // throw new RuntimeException(e);
        // }
    }

    private void moveMerge(String dir) {
        synchronized (board) {
            if (actionMap.containsKey(dir)) {
                combo = numOfTilesMoved = 0;

                // go to the hash map, find the corresponding method and call it
                actionMap.get(dir).run();

                // calculate the new score
                score += combo / 5 * 2;

                // determine whether the game is over or not
                if (numOfTilesMoved > 0) {
                    totalMoveCount++;
                    gameOver = level == LIMIT || !nextRound();
                } else
                    gameOver = isFull();

                // update the database if the game is over
                if (gameOver) {
                    try {
                        Database.putScore(playerName, score, level);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Generate a new random value and determine the game status.
     * 
     * @return true if the next round can be started, otherwise false.
     */
    private boolean nextRound() {
        if (isFull())
            return false;
        int i;

        // randomly find an empty place
        do {
            i = random.nextInt(SIZE * SIZE);
        } while (board[i] > 0);

        // randomly generate a card based on the existing level, and assign it to the
        // select place
        board[i] = random.nextInt(level) / 4 + 1;
        return true;
    }

    /**
     * @return true if all blocks are occupied.
     */
    private boolean isFull() {
        for (int v : board)
            if (v == 0)
                return false;
        return true;
    }

    /**
     * move the values downward and merge them.
     */
    private void moveDown() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(SIZE, SIZE * (SIZE - 1) + i, i);
    }

    /**
     * move the values upward and merge them.
     */
    private void moveUp() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i);
    }

    /**
     * move the values rightward and merge them.
     */
    private void moveRight() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(1, SIZE - 1 + i, i);
    }

    /**
     * move the values leftward and merge them.
     */
    private void moveLeft() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(-1, i, SIZE - 1 + i);
    }

    /**
     * Move and merge the values in a specific row or column. The moving direction
     * and the specific row or column is determined by d, s, and l.
     * 
     * @param d - move distance
     * @param s - the index of the first element in the row or column
     * @param l - the index of the last element in the row or column.
     */
    private void moveMerge(int d, int s, int l) {
        int v, j;
        for (int i = s - d; i != l - d; i -= d) {
            j = i;
            if (board[j] <= 0)
                continue;
            v = board[j];
            board[j] = 0;
            while (j + d != s && board[j + d] == 0)
                j += d;

            if (board[j + d] == 0) {
                j += d;
                board[j] = v;
            } else {
                while (j != s && board[j + d] == v) {
                    j += d;
                    board[j] = 0;
                    v++;
                    score++;
                    combo++;
                }
                board[j] = v;
                if (v > level)
                    level = v;
            }
            if (i != j)
                numOfTilesMoved++;

        }
    }

    public int getValue(int r, int c) {
        synchronized (board) {
            return board[r * SIZE + c];
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setPlayerName(String name) {
        playerName = name;
    }

    public int getScore() {
        return score;
    }

    public int getCombo() {
        return combo;
    }

    public int getLevel() {
        return level;
    }

    public int getMoveCount() {
        return totalMoveCount;
    }

    // handle the exception separately instead of just throwing
    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}
