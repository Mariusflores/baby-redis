import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client {

    public static void help() {
        System.out.println("=== Baby Redis ===");
        System.out.println("String commands:");
        System.out.println("  SET <key> <value>        Store a value");
        System.out.println("  GET <key>                Retrieve a value");
        System.out.println("  DELETE <key>             Remove a key");
        System.out.println(" ");
        System.out.println("Set commands:");
        System.out.println("  SADD <key> <val> [val]   Add values to a set");
        System.out.println("  SREM <key> <val> [val]   Remove values from a set");
        System.out.println("  SISMEMBER/SIM <key> <val>    Check if value is in set");
        System.out.println("  SMEMBERS/SM <key>           List all values in set");
        System.out.println(" ");
        System.out.println("  HELP                     Show this message");
        System.out.println("  QUIT                     Disconnect");
    }

    public static void main(String[] args) {

        List<String> allowedCommands =
                Arrays.asList("SET", "GET", "DELETE", "QUIT", "SADD", "SREM", "SISMEMBER", "SMEMBERS", "SIM", "SM", "EXPIRE", "EXP");
        try {
            Socket s = new Socket("localhost", 6379);

            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true
            );
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)
            );

            Scanner scanner = new Scanner(System.in);

            Client.help();
            while (scanner.hasNext()) {
                String line = scanner.nextLine();

                if (line.equalsIgnoreCase("HELP")) {
                    Client.help();
                } else if (allowedCommands.contains(line.trim().split(" ")[0].toUpperCase())) {

                    out.println(line);

                    if (line.equalsIgnoreCase("QUIT")) {
                        return;
                    }

                    System.out.println(reader.readLine());
                } else {
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
