import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) {

        try {
            Socket s = new Socket("localhost", 6379);

            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true
            );

            out.println("SET foo bar");
            out.println("GET foo");
            out.println("DELETE foo");
            out.println("GET foo");

            // Closing connections

            //Closing socket
            s.close();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }
}
