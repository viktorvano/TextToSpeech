package com.Viktor.Vano.TextToSpeech;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.Viktor.Vano.TextToSpeech.FileManager.*;

public class GUI extends Application {
    private final String version = "20221204";
    private int port = 7775;
    private final int width = 400;
    private final int height = 120;
    private TextToSpeechServer textToSpeechServer;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage)
    {
        Pane pane = new Pane();

        Scene scene = new Scene(pane, width, height);

        stage.setTitle("Text To Speech " + version);
        stage.setScene(scene);
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
        stage.setMaxWidth(stage.getWidth());
        stage.setMaxHeight(stage.getHeight());
        stage.setResizable(false);

        try{
            port = Integer.parseInt(Objects.requireNonNull(readOrCreateFile("tts_port.txt")));
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        Label labelPort = new Label("Port: " + port);
        labelPort.setFont(Font.font("Arial", 24));
        labelPort.setLayoutX(130);
        labelPort.setLayoutY(50);
        pane.getChildren().add(labelPort);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setLayoutX(30);
        progressBar.setLayoutY(90);
        progressBar.setPrefWidth(350);
        progressBar.setStyle("-fx-accent: red");
        pane.getChildren().add(progressBar);

        try
        {
            Image icon = new Image(getClass().getResourceAsStream("tts.jpg"));
            stage.getIcons().add(icon);
            System.out.println("Icon loaded on the first attempt...");
        }catch(Exception e)
        {
            try
            {
                Image icon = new Image("tts.jpg");
                stage.getIcons().add(icon);
                System.out.println("Icon loaded on the second attempt...");
            }catch(Exception e1)
            {
                System.out.println("Icon failed to load...");
            }
        }

        // Param for playback (input) device.
        Line.Info playbackLine = new Line.Info(SourceDataLine.class);
        ArrayList<Mixer.Info> audios = filterDevices(playbackLine);

        System.out.println("\n\nFound audio devices:\n");
        for(int i = 0; i < audios.size(); i++)
        {
            Mixer.Info info = audios.get(i);
            System.out.println(String.format("Index [%s]\nName [%s]\nDescription [%s]", i, info.getName(), info.getDescription()));
            System.out.println("\n");
        }

        int audioIndex = 0;
        try{
            audioIndex = Integer.parseInt(Objects.requireNonNull(readOrCreateFile("tts_audio.txt")));
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        if(audios.size()-1 < audioIndex)
        {
            audioIndex = 0;
            writeToFile("tts_audio.txt", "0");
        }

        System.out.println("\n\nUsing audio output device [" + audioIndex + "]:");
        Mixer.Info info = audios.get(audioIndex);
        System.out.println(String.format("Name [%s]\nDescription [%s]", info.getName(), info.getDescription()));
        System.out.println("\n\n");

        textToSpeechServer = new TextToSpeechServer(port, audios.get(audioIndex));
        textToSpeechServer.start();
    }

    private static ArrayList<Mixer.Info> filterDevices(final Line.Info supportedLine) {
        ArrayList<Mixer.Info> result = new ArrayList<>();

        List<Mixer.Info> audioInfo = Arrays.asList(AudioSystem.getMixerInfo());
        ArrayList<Mixer.Info> infos = new ArrayList<>(audioInfo);
        for (Mixer.Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(supportedLine)) {
                result.add(info);
            }
        }
        return result;
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
        textToSpeechServer.stopServer();
    }
}
