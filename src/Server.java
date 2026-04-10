import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Server {
    final InMemoryStore store = new InMemoryStore();


    private String delegate(String [] commands){
        // Input format i.e. SET hello world
        String command = commands[0];
        String key = commands[1];
        String value = "";


        switch (command.toUpperCase()){
            case "SET" -> {
                value = commands[2];
                store.set(key, value);
                return "OK";
            }
            case "GET" -> {
                String result = store.get(key);
                return result == null ? "NOT FOUND" : result;
            }
            case "DELETE" -> {
                store.delete(key);
                return "OK";
            }
            default -> {
                return "ERR Unknown Command";
            }
        }
    }



    public static void main(String[] args) {
        Server server = new Server();
        System.out.println("Starting server...");

        try {

            // Create a ServerSocket listening on port 6379
            ServerSocket ss = new ServerSocket(6379);

            System.out.println("Server started listening on port 6379... ");

            // Accept a connection from a client
            Socket s = ss.accept();
            System.out.println("Client connected");

            // Read message from the client
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

            String line;

            while((line = reader.readLine()) != null){
                System.out.println("Command " + line);


                String[] commands = line.trim().split(" ");

                if(commands.length < 2){
                    throw new RuntimeException("Command Format unknown");
                }
                String result = server.delegate(commands);

                System.out.println("Result: " + result);

            }

            // Close socket
            s.close();

            ss.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
