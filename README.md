# TextToSpeech
 Text to speech that listens on a server socket.
  
  
### Server Socket  
It listens on a port 7775 by default. The port can be changed in "port.txt".  
  
### Essential JAR library  
[Download FreeTTS-1.2.2.jar in a ZIP](https://download.jar-download.com/cache_jars/net.sf.sociaal/freetts/1.2.2/jar_files.zip)  
[Search FreeTTS on jar-download.com](https://jar-download.com/artifact-search/freetts)  
  
### Screenshots  
![alt text](https://github.com/viktorvano/TextToSpeech/blob/main/screenshots/TTS.png?raw=true)  
  
  
### Client Code Example  
```Java
import java.net.*;
import java.io.*;

public class Client {
    // initialize socket and input output streams
    private Socket socket            = null;
    private DataInputStream  input   = null;
    private DataOutputStream out     = null;

    // constructor to put ip address and port
    public Client(String address, int port)
    {
        // establish a connection
        try
        {
            socket = new Socket(address, port);
            System.out.println("Connected");

            // takes input from terminal
            input  = new DataInputStream(System.in);

            // sends output to the socket
            out    = new DataOutputStream(socket.getOutputStream());
        }
        catch(UnknownHostException u)
        {
            System.out.println(u);
        }
        catch(IOException i)
        {
            System.out.println(i);
        }

        // string to read message from input
        String line = "";

        try
        {
            line = input.readLine();
            out.writeUTF(line);
        }
        catch(IOException i)
        {
            System.out.println(i);
        }


        // close the connection
        try
        {
            input.close();
            out.close();
            socket.close();
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    public static void main(String[] args)
    {
            new Client("192.168.1.15", 7775);
    }
}
```
