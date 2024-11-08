import java.io.Serializable;
import java.net.Socket;

public class Player implements Serializable {


    private int level;
    private int score;
    private int combo;
    private String playerName;
    private int numberOfMoves;
    private Socket socket;
    private boolean undoFlag;


    public Player(String playerName, Socket socket, int level, int score, int numberOfMoves, int combo) {
        this.playerName = playerName;
        this.socket = socket;
        this.level = level;
        this.score = score;
        this.numberOfMoves = numberOfMoves;
        this.combo = combo;
this.undoFlag = true;    }


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
        return undoFlag;
    }

    public void setUndoFlag(boolean undoFlag){
        this.undoFlag = undoFlag;
    }

}
