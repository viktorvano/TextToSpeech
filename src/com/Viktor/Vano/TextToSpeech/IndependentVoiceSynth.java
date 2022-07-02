package com.Viktor.Vano.TextToSpeech;

import javax.speech.EngineException;

public class IndependentVoiceSynth extends Thread{
    private String message;
    private SpeechUtils speechUtils;

    public IndependentVoiceSynth(String message)
    {
        this.message = message;
        this.speechUtils = new SpeechUtils();
        try{
            speechUtils.init("kevin16");
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(this.message);
        try {
            speechUtils.doSpeak(this.message);
        } catch (Exception e) {
            System.out.println("Did not speak: " + this.message);
        }
    }

    public void terminate()
    {
        try {
            speechUtils.terminate();
        } catch (EngineException e) {
            throw new RuntimeException(e);
        }
    }
}
