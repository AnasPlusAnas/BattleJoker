import javafx.application.Application;
import javafx.stage.Stage;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.SQLException;

public class BattleJoker extends Application {
   // private Socket clientSocket;

    @Override
    public void start(Stage primaryStage) {
        try {
            GetNameDialog dialog = new GetNameDialog();
            String ip = dialog.getIp();
            int port = dialog.getPort();

//            GameWindow win = new GameWindow(primaryStage, ip, port, dialog.getPlayername());
            GameWindow win = GameWindow.getInstance(primaryStage, ip, port, dialog.getPlayername());

            win.setName(dialog.getPlayername());


            System.out.println("BattleJoker.start");

            Database.connect();


            // currently hardcoded, change later
            // change to a dialog box and ask user for the ip address and the port
            //clientSocket = new Socket("127.0.0.1", 12345);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        try {
            Database.disconnect();
        } catch (SQLException ignored) {
        }
    }

    public static void main(String[] args) {
        System.setErr(new FilteredStream(System.err));  // All JavaFX'es version warnings will not be displayed

        launch();
    }

}

class FilteredStream extends PrintStream {

    public FilteredStream(OutputStream out) {
        super(out);
    }

    @Override
    public void println(String x) {
        if (x != null && !x.contains("SLF4J: "))
            super.println(x);
    }

    @Override
    public void print(String x) {
        if (x!= null && !x.contains("WARNING: Loading FXML document with JavaFX API of version 18"))
            super.print(x);
    }
}

