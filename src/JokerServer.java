import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JokerServer {
    private final ArrayList<Socket> clientList = new ArrayList<>(); // remember all the client
    private final ArrayList<Player> playerList = new ArrayList<>(); // remember all the client
    private final ArrayList<Player> awaitingPlayerList = new ArrayList<>(); // remember all the client

    // client
    // game setting
    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    Random random = new Random(0);

    private boolean isGameStarted = false;

    private static GameEngine instance;
    private boolean gameOver;
    private final int MAX_MOVES = 4;

    private String playerName;
    private int level = 1;
    private int score;
    private int combo;
    private int totalMoveCount;
    private int numOfTilesMoved;
    private Player currentPlayer = null;
    private int nextPlayerIndex = 0;
    private boolean restartFlag = false;

    final int[] clonedBoard = new int[SIZE * SIZE]; // for undo method
    private final Map<String, Runnable> actionMap = new HashMap<>();

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }



    public JokerServer(int port) throws IOException {
        log("Joker Server has started");

        // define a hash map to contain the links from the actions to the corresponding
        // methods
        actionMap.put("U", this::moveUp);
        actionMap.put("D", this::moveDown);
        actionMap.put("L", this::moveLeft);
        actionMap.put("R", this::moveRight);

        // start the first round
        //nextRound();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) { // Allow multiple connections
                Socket clientSocket = serverSocket.accept(); // client successfully connect to the server

                synchronized (clientList) {
                    clientList.add(clientSocket); // add the client socket into the clientList
                    log("Added new clientSocket: " + clientSocket.toString());
                }

                Thread t = new Thread(() -> {
                    try {
                        serve(clientSocket); // try if generate exception (client disconnect to the server)
                    } catch (IOException e) {
                        // e.printStackTrace();
                        log("The connection was dropped by the client! "
                                + clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort());

                        synchronized (clientList) {
                            clientList.remove(clientSocket); // remove the client from the socketList

                            Player playerToRemove = playerList.stream().filter(p -> p.getSocket() == clientSocket)
                                    .findFirst()
                                    .orElse(null);

                            synchronized (playerList) {
                                // Check if the player to remove is the current player
                                boolean isCurrentPlayer = currentPlayer.equals(playerToRemove);

                                // Remove the player from the list
                                playerList.remove(playerToRemove);
                                awaitingPlayerList.remove(playerToRemove);

                                try {
                                    sendRemovedPlayer(playerToRemove);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }

                                // If the removed player is the current player, select a new current player
                                if (isCurrentPlayer && !playerList.isEmpty()) {
                                    assignNewCurrentPlayer();
                                }

                                try {
                                    sendPlayerList();
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
        log("Established a connection: "+clientSocket);

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

        //20241122 Melody updated - Start
        // add the player to the playerList
        synchronized (playerList) {
            if(isGameStarted) {
                player.setAwaitingPlayer(true);
                awaitingPlayerList.add(player);
            }else {
                playerList.add(player);
            }
            log("Added new player: "+player);
            //20241122 Melody updated - End

            // if the first player, the player who started the game, he gets to play first.
            if (playerList.size() == 1 && awaitingPlayerList.size() == 0) {
                assignNewCurrentPlayer();
                currentPlayer.setHost(true);
                //currentPlayer = player;
            }

            //20241122 Melody updated - Start
            if(playerList.size() >= 4){
                //isGameStarted = true;
                startNewGame();
            }
            //20241122 Melody updated - End
        }


        // send out the updated version of puzzle board to the client
        DataOutputStream _out = new DataOutputStream(clientSocket.getOutputStream());
        synchronized (clientList) {
            for (Socket socket : clientList) {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // get the output stream from
                // the socket
                //out.write(direction); // send data to the outputSteam
                //out.flush(); // force to send out the data immediately

                sendPuzzle(out);

            }
        }

        synchronized (playerList) {
            sendPlayerList();
        }

        while (true) {
            char direction = (char) inputStream.read();
            //log("ClientSocket:"+clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort() + "= " + direction
            //                + "\n*** " + clientSocket);
            log("Command = "+direction+" (requested by "+getPlayerBySocket(clientSocket)+")");

            //20241122 Melody updated - Start

            if(direction == 'P'){
                sendPlayerList();
                continue;
            }


            if(direction == 'E' && !restartFlag){
                //DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                //log("Restarting the game");

                //sendPlayerList();
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                awaitingPlayerJoinGame();
                endGameAction();
                //sendPuzzle(out);
                //sendPlayerList();

                synchronized (clientList) {
                    for (Socket socket : clientList) {
                        out = new DataOutputStream(socket.getOutputStream()); // get the output stream from
                        // the socket
                        //out.write(direction); // send data to the outputSteam
                        //out.flush(); // force to send out the data immediately

                        sendPuzzle(out);

                    }
                }

                restartFlag = true;

/*
                synchronized (clientList) {
                    for (Socket socket : clientList) {
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // get the output stream from
                        // the socket
                        //out.write(direction); // send data to the outputSteam
                        //out.flush(); // force to send out the data immediately

                    }
                }

 */
                //score = 0;
                /*
                synchronized (clientList) {
                    for (Socket socket : clientList) {
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // get the output stream from
                        // the socket
                        //out.write(direction); // send data to the outputSteam
                        //out.flush(); // force to send out the data immediately
                        sendPlayerList();
                        sendPuzzle(out);
                    }
                }
                //_out.writeBoolean(isGameStarted);
                 */

            }

            if(direction == 'C'){
                //DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                _out.writeBoolean(isGameStarted);
                log("Return isGameStarted = "+isGameStarted);
                continue;
            }

            if(direction == 'W'){
                boolean isAwaitingPlayer = false;
                Player tempPlayer = getPlayerBySocket(clientSocket);

                if(playerList.contains(tempPlayer)){
                    isAwaitingPlayer = false;
                }else{
                    isAwaitingPlayer = true;
                }
                _out.writeBoolean(isAwaitingPlayer);
                log("Return isAwaitingPlayer = "+isAwaitingPlayer);
                continue;
            }

            Player currentPlayer = getPlayerBySocket(clientSocket);
            if(currentPlayer == null){
                Player awaitingCurrentPlayer = getAwaitingPlayerBySocket(clientSocket);
                if(awaitingCurrentPlayer == null){
                    continue;
                }
                // TODO handle awaiting player
                // Code here:


            }else{
                log("CurrentPlayer = "+currentPlayer.getPlayerName());
            }
            //20241122 Melody updated - End

            /*
            if(direction == 'P'){
                sendPlayerList();
                continue;
            }
            */

            //20241122 Melody updated - Start
            if(direction == 'S' && currentPlayer.isHost() && !isGameStarted){
                startNewGame();
            }

            if(!isGameStarted){
                continue;
            }

            //20241122 Melody updated - End

//            if (direction == 'N' && false) {
//                if (currentPlayer.getUndoFlag() && totalMoveCount != 0) {
//                    undoPuzzle();
//                    currentPlayer.setUndoFlag(false);
//                    synchronized (playerList) {
//                        sendPlayerList();
//                    }
//
//                    synchronized (clientList) {
//                        sendPuzzle(_out);
//                    }
//                }
//            }

            cloneBoard(); // cloned the board before move
           /// if (!isGameStarted) return;
            moveMerge("" + direction);

            // if nextRound = true then the game is still not over, else the game is over
            //gameOver = !nextRound();

            // send the move back to other clients connected
            // assert clientList != null;
            synchronized (clientList) {
                for (Socket socket : clientList) {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // get the output stream from
                                                                                           // the socket
                    //out.write(direction); // send data to the outputSteam
                    //out.flush(); // force to send out the data immediately

                    sendPuzzle(out);
                }
            }

            //updatePlayerStat(player);
        }
    }

    private void sendRemovedPlayer(Player player) throws IOException {
        for (Socket client : clientList) {
            log("JokerServer.sendRemovedPlayer");
            log("player = " + player.getPlayerName());

            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            out.write('Q');
            byte[] bytes = player.getPlayerName().getBytes();
            out.writeInt(bytes.length);
            out.write(bytes);

            out.flush();
        }
    }

    private void sendPlayerList() throws IOException {
        for (Socket client : clientList) {
            log("JokerServer.sendPlayerList");

            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            for (Player player : playerList) {
                if(player.isAwaitingPlayer()){
                    continue;
                }
                out.write('P');
                byte[] bytes = player.getPlayerName().getBytes();
                out.writeInt(bytes.length);
                out.write(bytes);
                out.writeInt(player.getLevel());
                out.writeInt(player.getScore());
                out.writeInt(player.getCombo());
                out.writeInt(player.getNumberOfMoves());
                out.writeBoolean(player.isMyTurn());
                out.writeBoolean(player.isHost());
                out.writeBoolean(player.isAwaitingPlayer());
                out.flush();
                log("Sending: "+player);
            }
        }
    }

    private void sendGameOver() throws IOException {
        for (Socket client : clientList) {
            log("JokerServer.sendGameOver");

            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            out.write('F');
            out.flush();
        }
    }

    public void undoPuzzle() {
        // board = clonedBoard.clone();
        System.arraycopy(clonedBoard, 0, board, 0, board.length);
        if (currentPlayer.getScore() != 0 && currentPlayer.getCombo() != 0 && currentPlayer.getNumberOfMoves() != 0) {
            currentPlayer.setScore(getScore() - 1);
            currentPlayer.setCombo(getCombo() - 1);
            currentPlayer.setNumberOfMoves(currentPlayer.getNumberOfMoves() - 1);
            totalMoveCount--;
        }
    }

    public void cloneBoard() {
        // clonedBoard = board.clone();
        System.arraycopy(board, 0, clonedBoard, 0, board.length);
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
                currentPlayer.setCombo(combo);

                // go to the hash map, find the corresponding method and call it
                actionMap.get(dir).run();

                // calculate the new score
                score += combo / 5 * 2;
                currentPlayer.setScore(score);
//                int playerScore = currentPlayer.getScore();
//                int playerCombo = currentPlayer.getCombo();
//                currentPlayer.setScore(playerScore += playerCombo / 5 * 2);

                // determine whether the game is over or not
                if (numOfTilesMoved > 0) {
                    totalMoveCount++;
                    currentPlayer.setNumberOfMoves(currentPlayer.getNumberOfMoves() + 1);
                    // Once the current player's moves reach MAX_MOVES, reset the count for the next player.
                    if (totalMoveCount == MAX_MOVES) {
                        totalMoveCount = 0;
                        // If the player is playing alone, no need to change turns
                        if (playerList.size() > 1) {
                            assignNewCurrentPlayer();
                        }
                    }

                    try {
                        sendPlayerList();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    gameOver = level == LIMIT || !nextRound();
                } else
                    gameOver = isFull();

                // update the database if the game is over
                if (gameOver) {
                    try {
                        // get the player with the highest score
                        Player player = playerList.stream().max(Comparator.comparingInt(Player::getScore)).get();

                        Database.putScore(player.getPlayerName(), player.getScore(), player.getLevel());
                        synchronized (clientList) {
                            for (Player p : playerList) {
                                p.setMyTurn(false);
                            }
                            sendPlayerList();
                            sendGameOver();
                            gameOver = false;
                            restartFlag = false;
/*
                            synchronized (clientList) {
                                for (Socket socket : clientList) {
                                    DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // get the output stream from
                                    // the socket
                                    //out.write(direction); // send data to the outputSteam
                                    //out.flush(); // force to send out the data immediately


                                    awaitingPlayerJoinGame();
                                    endGameAction();

                                    sendPuzzle(out);
                                    sendPlayerList();
                                }
                            }
                            */
                        }


                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            }
        }
    }

    // Method to assign a new current player randomly
    private void assignNewCurrentPlayer() {
        if (currentPlayer != null) {
            currentPlayer.setMyTurn(false);
        }

        // Update current player to the next player

        do {
            nextPlayerIndex = (nextPlayerIndex+1)%playerList.size();
        } while (playerList.get(nextPlayerIndex) == currentPlayer || playerList.get(nextPlayerIndex).isAwaitingPlayer()); // Ensure it's not the current player

        currentPlayer = playerList.get(nextPlayerIndex); // Set the next player
        currentPlayer.setMyTurn(true); // Set their turn
        score = currentPlayer.getScore();
        combo = currentPlayer.getCombo();

        log("Player index = "+nextPlayerIndex);
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
                    // get the currentPlayer score and combo and update score and combo to the
                    // currentPlayer first then update the count

                    score = currentPlayer.getScore() + 1;
                    combo = currentPlayer.getCombo() + 1;
                    currentPlayer.setScore(score);
                    currentPlayer.setCombo(combo);
                    // score++;
                    // combo++;
                }
                board[j] = v;
                if (v > level){
                    level = v;
                    playerList.forEach(player -> {
                        player.setLevel(level);
                    });
                }
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

    //20241122 Melody updated - Start
    public Player getPlayerBySocket(Socket socket){
        for(int i = 0; i < playerList.size(); i++){
            Player player = playerList.get(i);
            if(player.getSocket().equals(socket)){
                return player;
            }
        }
        return null;
    }

    public Player getAwaitingPlayerBySocket(Socket socket){
        for(int i = 0; i < awaitingPlayerList.size(); i++){
            Player player = awaitingPlayerList.get(i);
            if(player.getSocket().equals(socket)){
                return player;
            }
        }
        return null;
    }

    public void awaitingPlayerJoinGame(){
        ArrayList<Player> removedList = new ArrayList<>();

        if((playerList.size()+awaitingPlayerList.size()) > 4){
            int moveToAwaitingPlayerCount = (playerList.size()+awaitingPlayerList.size()) - 4;

            for(int i = 0; i < moveToAwaitingPlayerCount; i++){
                Player player = playerList.get(i);
                //playerList.remove(player);
                removedList.add(player);
                player.setAwaitingPlayer(true);
                awaitingPlayerList.add(player);
            }

            for(int i = 0; i < removedList.size(); i++){
                playerList.remove(removedList.get(i));
            }

            removedList = new ArrayList<>();

        }

        for(int i = 0; i < awaitingPlayerList.size(); i++){
            if(playerList.size() < 4){
                Player player = awaitingPlayerList.get(i);
                //awaitingPlayerList.remove(player);
                removedList.add(player);
                player.setAwaitingPlayer(false);
                //player.resetPlayerStatus();
                playerList.add(player);
            }
        }

        for(int i = 0; i < removedList.size(); i++){
            awaitingPlayerList.remove(removedList.get(i));
        }

        for(int i = 0; i < playerList.size(); i++){
            Player player = playerList.get(i);
            //player.resetPlayerStatus();
            if(i == 0){
                player.setHost(true);
                player.setMyTurn(true);
                currentPlayer = player;
            }else{
                player.setHost(false);
                player.setMyTurn(false);
            }
        }

        removedList = new ArrayList<>();
    }

    public void endGameAction(){
        for (int i = 0; i < board.length; i++) {
            board[i] = 0; // Set each element to 0
        }
        score = 0;
        level = 1;
        combo = 0;
        totalMoveCount = 0;
        isGameStarted = false;
        if(playerList.size() >= 4 && !isGameStarted){
            startNewGame();
        }
    }

    public void startNewGame(){
        //log("The host started the game!");
        for (int i = 0; i < board.length; i++) {
            board[i] = 0; // Set each element to 0
        }
        //For DEBUG
        for(int i = 0; i < playerList.size(); i++){
            //log("***DEBUG: "+playerList.get(i).getPlayerName()+"/HOST:"+playerList.get(i).isHost()+"/TURN:"+playerList.get(i).isMyTurn());
            Player player = playerList.get(i);
            player.resetPlayerStatus();
            if(i == 0){
                player.setHost(true);
                player.setMyTurn(true);
            }else{
                player.setHost(false);
                player.setMyTurn(false);
            }
        }

        /*
        for(int i = 0; i < playerList.size(); i++){
            Player player = playerList.get(i);
            player.resetPlayerStatus();
            if(i == 0){
                player.setHost(true);
                player.setMyTurn(true);
            }

        }
         */
        nextRound();
        isGameStarted = true;
        nextPlayerIndex = 0;
    }

    private void log(String msg){
        // Get current datetime
        String dateTime = getCurrentDateTimeStr();
        // Print log
        String logMessage = dateTime + " - " + msg;
        System.out.println(logMessage);
    }

    private void log(char msg){
        log(""+msg);
    }

    private String getCurrentDateTimeStr(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTime = sdf.format(new Date());
        return dateTime;
    }

    //20241122 Melody updated - End



    // handle the exception separately instead of just throwing
    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}
