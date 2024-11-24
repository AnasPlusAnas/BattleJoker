import java.io.Serializable;
import java.net.Socket;

public class Player implements Serializable {


    private int level;
    private int score;
    private boolean isHost;
    private int combo;
    private String playerName;
    private int numberOfMoves;
    private Socket socket;
    private boolean undoFlag;
    private boolean isMyTurn;

    private boolean isAwaitingPlayer;


    public Player(String playerName, Socket socket, int level, int score, int numberOfMoves, int combo) {
        this.playerName = playerName;
        this.socket = socket;
        this.level = level;
        this.score = score;
        this.numberOfMoves = numberOfMoves;
        this.combo = combo;
        this.undoFlag = true;
        this.isAwaitingPlayer = false;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public void setMyTurn(boolean myTurn) {
        isMyTurn = myTurn;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getNumberOfMoves() {
        return numberOfMoves;
    }

    public void setNumberOfMoves(int numberOfMoves) {
        this.numberOfMoves = numberOfMoves;
    }

    public int getCombo() {
        return combo;
    }

    public void setCombo(int combo) {
        this.combo = combo;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean getUndoFlag(){
        return this.undoFlag;
    }

    public void setUndoFlag(boolean undoFlag){
        this.undoFlag = undoFlag;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public void resetPlayerStatus(){
        this.level = 0;
        this.score = 0;
        this.numberOfMoves = 0;
        this.combo = 0;
        this.undoFlag = true;
        this.isAwaitingPlayer = false;
        this.isHost = false;
        this.isMyTurn = false;
    }

    public void setAwaitingPlayer(boolean awaitingPlayer) {
        isAwaitingPlayer = awaitingPlayer;
    }

    public boolean isAwaitingPlayer() {
        return this.isAwaitingPlayer;
    }

    public Player clone(){
        Player clonePlayer = new Player(this.playerName, this.socket, this.level, this.score, this.numberOfMoves, this.combo);
        clonePlayer.setUndoFlag(this.undoFlag);
        clonePlayer.setHost(this.isHost);
        clonePlayer.setMyTurn(this.isMyTurn);
        clonePlayer.setAwaitingPlayer(this.isAwaitingPlayer);
        return clonePlayer;
    }

    public String toString(){
        String msg = "";
        msg += "playerName = "+playerName;
        msg += " / isAwaitingPlayer = "+isAwaitingPlayer;
        msg += " / socket= "+socket;
        return msg;
    }
}
