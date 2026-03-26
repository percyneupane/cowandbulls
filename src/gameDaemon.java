import java.io.IOException;
import java.net.*;
import java.util.*;

public class gameDaemon {

    public static void main(String[] args) {
        ServerSocket myserver;
        Socket clSocket;

        try {
            myserver = new ServerSocket(42212);
            while(true){
                clSocket = myserver.accept();
                gameThread thread = new gameThread(clSocket);
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}