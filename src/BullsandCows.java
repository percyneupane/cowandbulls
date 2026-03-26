import java.net.*;
import java.util.*;
import java.io.*;


public class BullsandCows {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 42212;

    public static void main (String[] args){
        Scanner scanner = new Scanner(System.in);
        try{
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        Socket socket = new Socket(host, port);
        DataInputStream in = new DataInputStream (socket.getInputStream());
        PrintStream out = new PrintStream(socket.getOutputStream());
        int guessCount =0;
        String guessString="";
        while(guessCount<20 && !guessString.equals("QUIT")) {
            String serverMessage = in.readLine();
            if (serverMessage.equals("GO")) {
                System.out.println("Welcome to Bulls and Cows. You will try to guess a 5 digit code using only the digits 0-9). You will lose the game if you are unable to guess the code correctly in 20 guesses.  Good Luck!");
            } else if (serverMessage.equals("BBBBB")) {
                System.out.println("Congratulations!!! You guessed the code correctly in " + guessCount + " guesses");
            } else {
                System.out.println(guessString + "  " + serverMessage);
            }

            do {
                System.out.println("Please enter your guess for the secret code or “QUIT” :");
                guessString = scanner.nextLine();
                if (guessString.equalsIgnoreCase("QUIT")) {
                    System.out.println("Goodbye but please play again!");
                    out.println("QUIT");
                    return;
                }
            } while (!verifyInput(guessString));
            out.println(guessString);
            guessCount++;
        }
        System.out.println("Sorry – the game is over. You did not guess the code correctly in 20 moves.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
     static boolean verifyInput(String gs){
        if(gs.equalsIgnoreCase("QUIT")){
            return true;
        }
        if(gs.length()!=5){
            System.out.println("Sorry, Your input is not in a proper format. Try to guess a 5 digit code");
            return false;
        }
        for (int i = 0; i < gs.length(); i++) {
            char c = gs.charAt(i);
            if (c < '0' || c > '9') {
                System.out.println("Sorry, Your input is not in proper format. Try to guess a 5 digit code using only the digits 0-9");
                return false;
            }
        }
        return true;
    }
}
