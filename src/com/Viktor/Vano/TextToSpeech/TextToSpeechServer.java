package com.Viktor.Vano.TextToSpeech;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static com.Viktor.Vano.TextToSpeech.TextToSpeech.*;

public class TextToSpeechServer extends Thread{
    private int port;
    private boolean run = true;
    private String message;

    //initialize socket and input stream
    private Socket		 socket = null;
    private ServerSocket server = null;
    private DataInputStream in	 = null;

    public TextToSpeechServer(int port){
        this.port = port;
        message = "";
    }

    public void stopServer()
    {
        this.run = false;
        try {
            if(socket!=null)
                socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if(server!=null)
                server.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if(in!=null)
                in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        while (run)
        {
            socket = null;
            server = null;
            in	 = null;

            // starts server and waits for a connection
            try
            {
                server = new ServerSocket(port);
                System.out.println("Server started");

                System.out.println("Waiting for a client ...");

                socket = server.accept();
                System.out.println("Client accepted");

                // takes input from the client socket
                in = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream()));

                message = "";

                try
                {
                    message = in.readUTF();
                }
                catch(Exception e)
                {
                    System.out.println(e);
                }

            }
            catch(Exception e)
            {
                System.out.println(e);
            }
            System.out.println("Closing connection");


            try {
                if(socket!=null)
                    socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if(server!=null)
                    server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if(in!=null)
                    in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            voice.speak(message);
        }
        System.out.println("Server stopped successfully.");
    }
}
