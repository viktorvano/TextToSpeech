package com.Viktor.Vano.TextToSpeech;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class FileManager {
    public static String readOrCreateFile(String filename)
    {
        File file = new File(filename);

        try
        {
            //Create the file
            if (file.createNewFile())
            {
                System.out.println("File is created!");
            } else {
                System.out.println("File already exists.");
            }

            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String data;
            data = reader.readLine();
            reader.close();
            System.out.println("Reading successful.");

            if(data==null && filename.equals("tts_port.txt"))
            {
                data="7775";
                writeToFile(filename, data);
            }else if(data==null && filename.equals("tts_audio.txt"))
            {
                data="0";
                writeToFile(filename, data);
            }

            return data;
        }
        catch (Exception e)
        {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            return null;
        }
    }

    public static boolean writeToFile(String filename, String data)
    {
        File file = new File(filename);

        try
        {
            //Create the file
            if (file.createNewFile())
            {
                System.out.println("File is created!");
            } else {
                System.out.println("File already exists.");
            }

            //Write Content
            FileWriter writer = new FileWriter(file);
            writer.write(data);
            writer.close();
            System.out.println("File write successful.");
            return true;
        }
        catch (Exception e)
        {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            return false;
        }
    }
}
