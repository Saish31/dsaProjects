package group_chatting_application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server implements Runnable{

    Socket socket;

    public static Vector client=new Vector();   // The client list holds writers to broadcast messages

    public Server(Socket socket){   // Constructor assigns the connected socket
        try{
            this.socket=socket;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        try{
            BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            client.add(writer);

            // Continuously read messages and broadcast them
            while(true){
                String data=reader.readLine().trim();
                System.out.println("Received "+data);

                for(int i=0;i<client.size();i++){   // Send the message to every connected client
                    try{
                        BufferedWriter bw=(BufferedWriter)client.get(i);
                        bw.write(data);
                        bw.write("\r\n");
                        bw.flush();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerSocket s=new ServerSocket(2003);  // Listen for incoming client connections on port 2003

        // Accept clients in an endless loop
        while(true){
            Socket socket=s.accept();
            Server server=new Server(socket);
            Thread thread=new Thread(server);
            thread.start();
        }
        
    }
}