import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class JokerServer {
    private final ArrayList<Socket> clientList = new ArrayList<>();

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public JokerServer(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                synchronized (clientList) {
                    clientList.add(clientSocket);
                }

                Thread t = new Thread(() -> {
                    try {
                        serve(clientSocket);
                    } catch (IOException e) {
                        //e.printStackTrace();
                        System.out.println("The connection was dropped by the client! " + clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort());

                        synchronized (clientList) {
                            clientList.remove(clientSocket);
                        }
                    }
                });
                t.start();
            }
        }
    }

    private void serve(Socket clientSocket) throws IOException {
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());

        // start receiving the moves from client. (GameEngine.java, moveMerge())
        DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());

        while (true) {
            char direction = (char) inputStream.read();
            System.out.println(clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort() + "= " + direction);

            // send the move back to other clients connected
            // assert clientList != null;
            synchronized (clientList) {
                for (Socket socket : clientList) {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    out.write(direction);
                    out.flush();
                }
            }
        }
    }

    // handle the exception separately instead of just throwing
    public static void main(String[] args) throws IOException {
        new JokerServer(12345);
    }
}
