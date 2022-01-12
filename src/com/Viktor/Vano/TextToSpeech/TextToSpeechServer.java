package com.Viktor.Vano.TextToSpeech;

import javax.sound.sampled.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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

            boolean isWindows = System.getProperty("os.name")
                    .toLowerCase().startsWith("windows");
            Process process;
            int exitCode = 0;
            try {
                if (isWindows) {
                    process = Runtime.getRuntime()
                            .exec(String.format("cmd.exe /c java -jar lib/freetts.jar -dumpAudio playback.wav -text %s", this.message));
                } else {
                    process = Runtime.getRuntime()
                            .exec(String.format("sh -c java -jar lib/freetts.jar -dumpAudio playback.wav -text %s", this.message));
                }
                StreamGobbler streamGobbler =
                        new StreamGobbler(process.getInputStream(), System.out::println);
                Executors.newSingleThreadExecutor().submit(streamGobbler);

                exitCode = process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            assert exitCode == 0;

            try
            {
                File yourFile = new File("playback.wav");
                AudioInputStream stream;
                AudioFormat format;
                DataLine.Info info;
                Clip clip;

                stream = AudioSystem.getAudioInputStream(yourFile);
                format = stream.getFormat();
                info = new DataLine.Info(Clip.class, format);
                clip = (Clip) AudioSystem.getLine(info);
                clip.open(stream);
                clip.start();
                Thread.sleep((clip.getMicrosecondLength()/1000) + 250);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Server stopped successfully.");
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
