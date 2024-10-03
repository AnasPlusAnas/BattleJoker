import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class JokerServer {
    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }
    public JokerServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            Thread t = new Thread(() ->{
               serve(clientSocket);
            });
            t.start();
        }
    }

    private void serve(Socket clientSocket) {
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());
    }

    // handle the exception separately instead of just throwing
    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}
