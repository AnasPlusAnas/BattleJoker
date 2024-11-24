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
    private Player lastPlayer = null;
    private int nextPlayerIndex = 0;
    private boolean restartFlag = false;

    final int[] clonedBoard = new int[SIZE * SIZE]; // for undo method
    private final Map<String, Runnable> actionMap = new HashMap<>();

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    //Command
    private final char CHECK_IS_STARTED = 'C';
    private final char START_THE_GAME = 'S';
    private final char SEND_ARRAY_FROM_SERVER = 'A';
    private final char SEND_PLAYER_LIST_FROM_SERVER = 'P';
    private final char SEND_REMOVE_PLAYER_FROM_SERVER = 'Q';
    private final char SEND_GAMEOVER_FROM_SERVER = 'F';
    private final char CHECK_IS_AWAITING = 'W';
    private final char REQUEST_RESTART_THE_GAME = 'E';
    private final char ACTION_UP = 'U';
    private final char ACTION_DOWN = 'D';
    private final char ACTION_LEFT = 'L';
    private final char ACTION_RIGHT = 'R';
    private final char ACTION_UNDO = 'N';
    //Command Map
    private final Map<Character, String> COMMAND_MAP = new HashMap<Character, String>() {{
        put(CHECK_IS_STARTED, "Check is the Game Started?");
        put(START_THE_GAME, "Start the game now");
        put(SEND_ARRAY_FROM_SERVER,"Send Board Array to the client");
        put(SEND_PLAYER_LIST_FROM_SERVER,"Send Player Array List to the client");
        put(SEND_REMOVE_PLAYER_FROM_SERVER,"Send Removed Player to the client");
        put(SEND_GAMEOVER_FROM_SERVER,"Send Gameover to the client");
        put(CHECK_IS_AWAITING,"Check is awaiting from the server");
        put(REQUEST_RESTART_THE_GAME,"Restart the game");
        put(ACTION_UP,"ACTION_UP");
        put(ACTION_DOWN,"ACTION_DOWN");
        put(ACTION_LEFT,"ACTION_LEFT");
        put(ACTION_RIGHT,"ACTION_RIGHT");
        put(ACTION_UNDO,"ACTION_UNDO");
    }};

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

                            if(playerToRemove == null){
                                playerToRemove = awaitingPlayerList.stream().filter(p -> p.getSocket() == clientSocket)
                                        .findFirst()
                                        .orElse(null);
                            }

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
        log("Server action: Established a connection: "+clientSocket);

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
            if(isGameStarted && playerList.size() == 0){
                endGameAction();
            }

            if(isGameStarted) {
                player.setAwaitingPlayer(true);
                awaitingPlayerList.add(player);
                String awaitingPlayerListStr = "[ ";
                for(int i = 0; i < awaitingPlayerList.size(); i++){
                    awaitingPlayerListStr += awaitingPlayerList.get(i).getPlayerName()+" ";
                }
                awaitingPlayerListStr += "]";
                log("Server log: awaitingPlayerListStr = "+awaitingPlayerListStr);
            }else {
                playerList.add(player);
            }
            log("Server action: Added new player: "+player);
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
            //log("Command = "+direction+" (requested by "+getPlayerBySocket(clientSocket)+")");
            Player requestPlayer = getPlayerBySocket(clientSocket);
            if(requestPlayer == null){
                requestPlayer = getAwaitingPlayerBySocket(clientSocket);
            }
            log("Received: "+direction+" ("+ COMMAND_MAP.get(direction) +", requested by "+requestPlayer+")");


            //20241122 Melody updated - Start

            if(direction == 'P'){
                sendPlayerList();
                continue;
            }


            if(direction == REQUEST_RESTART_THE_GAME && !restartFlag){
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

            if(direction == CHECK_IS_STARTED){
                //DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                //_out.writeBoolean(isGameStarted);
                sendBooleanToClient(_out,isGameStarted,"isGameStarted");
                //log("Return isGameStarted = "+isGameStarted);
                continue;
            }

            if(direction == CHECK_IS_AWAITING){
                boolean isAwaitingPlayer = false;
                Player tempPlayer = getPlayerBySocket(clientSocket);

                if(playerList.contains(tempPlayer)){
                    isAwaitingPlayer = false;
                }else{
                    isAwaitingPlayer = true;
                }
                //_out.writeBoolean(isAwaitingPlayer);
                //log("Return isAwaitingPlayer = "+isAwaitingPlayer);
                sendBooleanToClient(_out,isAwaitingPlayer, "isAwaitingPlayer");
                continue;
            }

            Player currentPlayer = getPlayerBySocket(clientSocket);
            if(currentPlayer == null){
                Player awaitingCurrentPlayer = getAwaitingPlayerBySocket(clientSocket);
                if(awaitingCurrentPlayer == null){
                    continue;
                }
            }
            //20241122 Melody updated - End

            /*
            if(direction == 'P'){
                sendPlayerList();
                continue;
            }
            */

            //20241122 Melody updated - Start
            if(direction == START_THE_GAME && currentPlayer.isHost() && !isGameStarted){
                startNewGame();
            }

            if(!isGameStarted){
                continue;
            }

            //20241122 Melody updated - End

            if (direction == ACTION_UNDO) {
                if (currentPlayer.getUndoFlag() && (totalMoveCount > 0 ||  (totalMoveCount == 0 && playerList.size() == 1))) {
                        if (lastPlayer != null && lastPlayer.getSocket().equals(currentPlayer.getSocket())) {
                        undoPuzzle();
                        undoPlayer();

                        //currentPlayer.setUndoFlag(false);
                        synchronized (playerList) {
                            sendPlayerList();
                        }

                        for (Socket socket : clientList) {
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // get the output stream from

                            sendPuzzle(out);

                        }
                }
            }
                continue;
            }

            cloneBoard(); // cloned the board before move
            clonePlayer(); // cloned the playerlist before move
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
        if(player == null){
            return;
        }
        for (Socket client : clientList) {
            log("JokerServer.sendRemovedPlayer");
            log("player = " + player.getPlayerName());

            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            //out.write('Q');
            sendToClient(out,SEND_REMOVE_PLAYER_FROM_SERVER);
            byte[] bytes = player.getPlayerName().getBytes();
            //out.writeInt(bytes.length);
            sendIntToClient(out,bytes.length, "bytes.length");
            //out.write(bytes);
            sendBytesToClient(out,bytes,"bytes");
            //HERE
            //out.flush();
        }
    }

    private void sendPlayerList() throws IOException {
        for (Socket client : clientList) {
            //log("JokerServer.sendPlayerList");

            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            String playerListStr = "[ ";

            for (Player player : playerList) {
                if(player.isAwaitingPlayer()){
                    continue;
                }
                //out.write(SEND_PLAYER_LIST_FROM_SERVER);
                sendToClient(out,SEND_PLAYER_LIST_FROM_SERVER);
                byte[] bytes = player.getPlayerName().getBytes();
                //out.writeInt(bytes.length);
                sendIntToClient(out,bytes.length,"bytes.length");
                //out.write(bytes);
                sendBytesToClient(out,bytes,"bytes");
                log("Sending: "+player.getPlayerName()+" (Player name)");
                //out.writeInt(player.getLevel());
                sendIntToClient(out,player.getLevel(),"player.getLevel()");
                //out.writeInt(player.getScore());
                sendIntToClient(out,player.getScore(),"player.getScore()");
                //out.writeInt(player.getCombo());
                sendIntToClient(out, player.getCombo(),"player.getCombo()");
                //out.writeInt(player.getNumberOfMoves());
                sendIntToClient(out,player.getNumberOfMoves(),"player.getNumberOfMoves()");
                //out.writeBoolean(player.isMyTurn());
                sendBooleanToClient(out,player.isMyTurn(),"player.isMyTurn()");
                //out.writeBoolean(player.isHost());
                sendBooleanToClient(out,player.isHost(),"player.isHost()");
                //out.writeBoolean(player.isAwaitingPlayer());
                sendBooleanToClient(out,player.isAwaitingPlayer(),"player.isAwaitingPlayer()");
                //out.flush();
                //log("Sending: "+player);
                playerListStr += player.getPlayerName()+" ";
            }
            playerListStr += "]";
            log("Player List = "+playerListStr);


            String awaitingPlayerListStr = "[ ";
            for(int i = 0; i < awaitingPlayerList.size(); i++){
                awaitingPlayerListStr += awaitingPlayerList.get(i).getPlayerName()+" ";
            }
            awaitingPlayerListStr += "]";
            log("Awaiting Player List = "+awaitingPlayerListStr);
        }
    }

    private void sendGameOver() throws IOException {
        for (Socket client : clientList) {
            log("JokerServer.sendGameOver");

            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            //out.write('F');
            //out.flush();
            sendToClient(out,SEND_GAMEOVER_FROM_SERVER);
        }
    }

    public void undoPuzzle() {
        // board = clonedBoard.clone();
        System.arraycopy(clonedBoard, 0, board, 0, board.length);
        if (currentPlayer.getScore() != 0 && currentPlayer.getCombo() != 0 && currentPlayer.getNumberOfMoves() != 0) {
            currentPlayer.setScore(getScore() - 1);
            currentPlayer.setCombo(getCombo() - 1);
            currentPlayer.setNumberOfMoves(currentPlayer.getNumberOfMoves() - 1);
        }
    }

    public void undoPlayer() {
        currentPlayer = lastPlayer;

        for(int i = 0; i < playerList.size(); i++){
            if(playerList.get(i).getSocket().equals(currentPlayer.getSocket())){
                playerList.get(i).setUndoFlag(false);
                playerList.set(i,currentPlayer);
            }
        }
    }

    public void cloneBoard() {
        // clonedBoard = board.clone();
        System.arraycopy(board, 0, clonedBoard, 0, board.length);
    }

    public void clonePlayer(){
        lastPlayer = currentPlayer.clone();
    }

    private void sendToClient(DataOutputStream out, char msg){
        try {
            log("Sending: "+msg+" ("+COMMAND_MAP.get(msg)+")");
            out.write(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+COMMAND_MAP.get(msg)+")");
        }
    }

    private void sendBytesToClient(DataOutputStream out, byte[] msg){
        try {
            log("Sending: "+msg+" ("+COMMAND_MAP.get(msg)+")");
            out.write(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+COMMAND_MAP.get(msg)+")");
        }
    }

    private void sendIntToClient(DataOutputStream out, int msg){
        try {
            log("Sending: "+msg+" ("+COMMAND_MAP.get(msg)+")");
            out.writeInt(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+COMMAND_MAP.get(msg)+")");
        }
    }

    private void sendBooleanToClient(DataOutputStream out, boolean msg){
        try {
            log("Sending: "+msg+" ("+COMMAND_MAP.get(msg)+")");
            out.writeBoolean(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+COMMAND_MAP.get(msg)+")");
        }
    }

    private void sendToClient(DataOutputStream out, char msg, String dataName){
        try {
            log("Sending: "+msg+" ("+dataName+")");
            out.write(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+dataName+")");
        }
    }

    private void sendBytesToClient(DataOutputStream out, byte[] msg, String dataName){
        try {
            log("Sending: "+msg+" ("+dataName+")");
            out.write(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+dataName+")");
        }
    }

    private void sendIntToClient(DataOutputStream out, int msg, String dataName){
        try {
            log("Sending: "+msg+" ("+dataName+")");
            out.writeInt(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+dataName+")");
        }
    }

    private void sendBooleanToClient(DataOutputStream out, boolean msg, String dataName){
        try {
            log("Sending: "+msg+" ("+dataName+")");
            out.writeBoolean(msg);
            out.flush();
        }catch (IOException e){
            log("Failed to send: "+msg+" ("+dataName+")");
        }
    }

    public void sendPuzzle(DataOutputStream out) throws IOException { // handle later
        //out.write('A'); // going to send out an array to client
        sendToClient(out,SEND_ARRAY_FROM_SERVER);
        //out.writeInt(board.length); // size of array
        sendIntToClient(out,board.length,"board.length");
        String boardStr = "[ ";
        for (int i : board) {
            out.writeInt(i); // send the value to the array
            boardStr += i+" " ;
        }
        boardStr += "]";
        out.flush(); // force java to send the data out
        log("Sending: Board = "+boardStr);
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

        //log("Player index = "+nextPlayerIndex);
        log("Server action: Updated currentPlayer = "+currentPlayer.getPlayerName()+"(Player Index = "+nextPlayerIndex+")");
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
                String awaitingPlayerListStr = "[ ";
                for(int j = 0; j < awaitingPlayerList.size(); j++){
                    awaitingPlayerListStr += awaitingPlayerList.get(j).getPlayerName()+" ";
                }
                awaitingPlayerListStr += "]";
                log("Server log: awaitingPlayerListStr = "+awaitingPlayerListStr);
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
        restartFlag = false;
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
