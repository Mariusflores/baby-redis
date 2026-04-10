import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {



    public static void main(String[] args) {
        final InMemoryStore store = new InMemoryStore();
        //Server server = new Server();
        System.out.println("Starting server...");

        try {

            // Create a ServerSocket listening on port 6379
            ServerSocket ss = new ServerSocket(6379);

            System.out.println("Server started listening on port 6379... ");

            // Accept a connection from a client
            Socket s = ss.accept();
            System.out.println("Client connected");

            // Read message from the client
            DataInputStream d = new DataInputStream(s.getInputStream());

            // Get first message SET foo bar
            String command1 = d.readUTF();

            String[] commands = command1.split(" ");
            System.out.printf("Setting %s as %s%n",commands[1],commands[2]);

            // call store

            store.set(commands[1],commands[2]);

            // second message GET foo
            String command2 = d.readUTF();
            commands = command2.split(" ");
            String result = store.get(commands[1]);
            //print out results
            System.out.printf("Key %s Value %s%n", commands[1], result);

            // last message DELETE foo
            String command3 = d.readUTF();
            commands = command3.split(" ");
            System.out.printf("deleting %s%n", commands[1]);
            store.delete(commands[1]);

            //double check foo got deleted
            result = store.get(commands[1]);
            System.out.printf("Deleted value: %s%n", result);



            // Close socket
            ss.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
