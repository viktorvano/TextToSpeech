package com.Viktor.Vano.TextToSpeech;

import javax.speech.AudioException;
import javax.speech.Central;
import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;
import java.beans.PropertyVetoException;
import java.util.Locale;

public class SpeechUtils {

    private SynthesizerModeDesc desc;
    private Synthesizer synthesizer;
    private Voice voice;

    public void init(String voiceName) throws EngineException, AudioException, EngineStateError, PropertyVetoException {
        if (desc == null) {
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            desc = new SynthesizerModeDesc(Locale.US);
            Central.registerEngineCentral("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
            synthesizer = Central.createSynthesizer(desc);
            synthesizer.allocate();
            synthesizer.resume();
            SynthesizerModeDesc smd = (SynthesizerModeDesc) synthesizer.getEngineModeDesc();
            Voice[] voices = smd.getVoices();
            for (Voice voice1 : voices) {
                if (voice1.getName().equals(voiceName)) {
                    voice = voice1;
                    break;
                }
            }
            synthesizer.getSynthesizerProperties().setVoice(voice);
        }
    }

    public void terminate() throws EngineException, EngineStateError {
        synthesizer.deallocate();
    }

    public void doSpeak(String speakText) throws IllegalArgumentException, InterruptedException {
        synthesizer.speakPlainText(speakText, null);
        synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
    }
}