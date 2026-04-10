import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {

        List<String> allowedCommands = Arrays.asList("SET", "GET", "DELETE", "QUIT");
        try {
            Socket s = new Socket("localhost", 6379);

            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true
            );
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)
            );

            Scanner scanner = new Scanner(System.in);

            System.out.println("Welcome");
            System.out.println("Supported commands are SET, GET, DELETE");
            System.out.println("SET takes key value pair, the other takes only key");
            System.out.println("Allowed format is <command> <key> <value>");
            System.out.println("Type HELP for information or QUIT to stop");
            while (scanner.hasNext()){
                String line = scanner.nextLine();

                    if(line.equalsIgnoreCase("HELP")){
                        System.out.println("Supported commands are SET, GET, DELETE");
                        System.out.println("SET takes key value pair, the other takes only key");
                        System.out.println("Allowed format is <command> <key> <value>");
                    }else if (allowedCommands.contains(line.trim().split(" ")[0].toUpperCase())){

                        out.println(line);

                        if(line.equalsIgnoreCase("QUIT")){
                            return;
                        }

                        System.out.println(reader.readLine());
                    }
                    else{
                        System.out.println("Supported commands are SET, GET, DELETE");
                        System.out.println("Type HELP for information or QUIT to stop");

                    }

            }


            // Closing connections

            scanner.close();

            reader.close();

            //Closing socket
            s.close();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }
}
