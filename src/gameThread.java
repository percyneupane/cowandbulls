import java.net.*;
import java.util.*;
import java.io.*;

public class gameThread extends Thread {
    Random gen= new Random();
    public Socket clSocket;


    public gameThread(Socket clSocket) {
        this.clSocket=clSocket;
    }

    public void run(){
        try {
            DataInputStream in = new DataInputStream (clSocket.getInputStream());
            PrintStream out = new PrintStream(clSocket.getOutputStream());
            StringBuilder genCode = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                genCode.append(gen.nextInt(10));
            }
            String code = genCode.toString();
            int count=0;
            String result="     ";
            while(!result.equals("BBBBB") && count<20){
                if(count==0){
                    out.println("GO");
                    count++;
                }else{
                    String guess= in.readLine();
                    if(guess.equals("QUIT")){
                        break;
                    }
                    result = processGuess(guess,code);
                    count++;
                    out.println(result);

                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        }
    String processGuess(String guess,String code){
        int B=0;
        int C=0;
        int[] freq = new int[10];

        for(int i=0;i<5;i++){
            char c = code.charAt(i);
            char g = guess.charAt(i);
            if(c==g){
                B++;
            }else {
                freq[c - '0']++;
            }
        }

        for (int i = 0; i < 5; i++) {
            char c = code.charAt(i);
            char g = guess.charAt(i);
            if (c != g) {
                int digit = g - '0';
                if (freq[digit] > 0) {
                    C++;
                    freq[digit]--;
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for(int i=0; i<B; i++) result.append("B");
        for(int i=0; i<C; i++) result.append("C");

        while(result.length()<5) result.append(" ");
        return result.toString();

    }

}
