import java.io.IOException;
import java.net.*;
import java.util.*;

public class gameDaemon {
    private static final int DEFAULT_PORT = 42212;

    public static void main(String[] args) {
        ServerSocket myserver;
        Socket clSocket;

        try {
            int port = resolvePort(args);
            myserver = new ServerSocket(port);
            System.out.println("Bulls and Cows server listening on port " + port);
            while(true){
                clSocket = myserver.accept();
                gameThread thread = new gameThread(clSocket);
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }

        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            return Integer.parseInt(envPort);
        }

        return DEFAULT_PORT;
    }
}
