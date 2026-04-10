import java.io.*;
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
        if(commands.length > 2){
            value = commands[2];
        }

        switch (command.toUpperCase()){
            case "SET" -> {
              if(value.isEmpty()){
                  return "ERR Value isnt provided";
              }
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

            //noinspection InfiniteLoopStatement
            while (true){

                Socket s = ss.accept();
                System.out.println("Server started listening on port 6379... ");

                // Accept a connection from a client


                System.out.println("Client connected");

                // Declare a buffered reader
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

                // Declare an Output Writer
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter (s.getOutputStream()), true
                );

                String line;

                while((line = reader.readLine()) != null){
                    System.out.println("Command " + line);


                    String[] commands = line.trim().split(" ");

                    if(commands.length == 1 && commands[0].equalsIgnoreCase("QUIT")){
                        // Close connections
                        break;
                    }
                    if(commands.length < 2){
                        out.println("ERR expected at least <2> arguments");
                        continue;
                    }
                    String result = server.delegate(commands);

                    out.println(result);

                }

                reader.close();
                out.close();
                s.close();


            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
