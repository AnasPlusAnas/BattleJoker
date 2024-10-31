import java.net.Socket;

public class Player {
    private int level = 1;
    private int score;
    private int combo;
    private String playerName;
    private Socket socket;

    public Player(String playerName, Socket socket) {
        this.playerName = playerName;
        this.socket = socket;
        this.level = 0;
        this.score = 0;
        this.combo = 0;
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
}
