package com.Viktor.Vano.TextToSpeech;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

import static com.Viktor.Vano.TextToSpeech.FileManager.*;

public class GUI extends Application {
    private final String version = "20220117";
    private int port = 7775;
    private final int width = 400;
    private final int height = 120;
    private TextToSpeechServer textToSpeechServer;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage){
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
            port = Integer.parseInt(Objects.requireNonNull(readOrCreateFile("port.txt")));
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        Label labelPort = new Label("Port: " + port);
        labelPort.setFont(Font.font("Arial", 24));
        labelPort.setLayoutX(130);
        labelPort.setLayoutY(50);
        pane.getChildren().add(labelPort);

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

        textToSpeechServer = new TextToSpeechServer(port);
        textToSpeechServer.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        textToSpeechServer.stopServer();
    }
}
