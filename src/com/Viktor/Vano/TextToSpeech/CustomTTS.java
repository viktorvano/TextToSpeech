package com.Viktor.Vano.TextToSpeech;

import com.sun.speech.freetts.InputMode;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.JavaClipAudioPlayer;
import com.sun.speech.freetts.audio.MultiFileAudioPlayer;
import com.sun.speech.freetts.audio.NullAudioPlayer;
import com.sun.speech.freetts.audio.RawFileAudioPlayer;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

import java.io.*;
import java.net.URL;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.*;

public class CustomTTS{
    private static final Logger LOGGER;
    public static final String VERSION = "Custom FreeTTS 1.0.0";
    private Voice voice;
    private static AudioPlayer audioPlayer;
    private boolean silent = false;
    private String audioFile = null;
    private boolean multiAudio = false;
    private boolean streamingAudio = false;
    private InputMode inputMode;


    public CustomTTS(String message, Mixer.Info info) {
        if(message!=null && message.length() > 0)
        {
            this.inputMode = InputMode.INTERACTIVE;
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            VoiceManager voiceManager = VoiceManager.getInstance();
            this.voice = voiceManager.getVoice("kevin16");
            JavaClipAudioPlayerExtended javaClipAudioPlayerExtended = new JavaClipAudioPlayerExtended(info);
            this.voice.setAudioPlayer(javaClipAudioPlayerExtended);
            this.setAudioFile("play.wav");
            this.startup();
            this.batchTextToSpeech(message);
            this.shutdown();
            try
            {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(
                        new File("play.wav"));
                Clip clip = AudioSystem.getClip(info);
                clip.open(audioInput);
                clip.start();
                do{
                    Thread.sleep(50);
                }while(clip.isRunning());
                clip.stop();
                clip.flush();
                clip.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public CustomTTS(Voice voice) {
        this.inputMode = InputMode.INTERACTIVE;
        this.voice = voice;
    }

    public void startup() {
        this.voice.allocate();
        if (!this.getSilentMode()) {
            if (this.audioFile != null) {
                AudioFileFormat.Type type = this.getAudioType(this.audioFile);
                if (type != null) {
                    if (this.multiAudio) {
                        audioPlayer = new MultiFileAudioPlayer(getBasename(this.audioFile), type);
                    } else {
                        audioPlayer = new SingleFileAudioPlayer(getBasename(this.audioFile), type);
                    }
                } else {
                    try {
                        audioPlayer = new RawFileAudioPlayer(this.audioFile);
                    } catch (IOException var4) {
                        System.out.println("Can't open " + this.audioFile + " " + var4);
                    }
                }
            } else if (!this.streamingAudio) {
                audioPlayer = new JavaClipAudioPlayer();
            } else {
                try {
                    audioPlayer = this.voice.getDefaultAudioPlayer();
                } catch (InstantiationException var3) {
                    var3.printStackTrace();
                }
            }
        }

        if (audioPlayer == null) {
            audioPlayer = new NullAudioPlayer();
        }

        this.voice.setAudioPlayer(audioPlayer);
    }

    private AudioFileFormat.Type getAudioType(String file) {
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
        String extension = getExtension(file);

        for(int i = 0; i < types.length; ++i) {
            if (types[i].getExtension().equals(extension)) {
                return types[i];
            }
        }

        return null;
    }

    private static String getExtension(String path) {
        int index = path.lastIndexOf(".");
        return index == -1 ? null : path.substring(index + 1);
    }

    private static String getBasename(String path) {
        int index = path.lastIndexOf(".");
        return index == -1 ? path : path.substring(0, index);
    }

    public void shutdown() {
        audioPlayer.close();
        this.voice.deallocate();
    }

    public boolean textToSpeech(String text) {
        return this.voice.speak(text);
    }

    private boolean batchTextToSpeech(String text) {
        this.voice.startBatch();
        boolean ok = this.textToSpeech(text);
        this.voice.endBatch();
        return ok;
    }

    private boolean lineToSpeech(String path) {
        boolean ok = true;
        this.voice.startBatch();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            while(true) {
                String line;
                if ((line = reader.readLine()) == null || !ok) {
                    reader.close();
                    break;
                }

                ok = this.textToSpeech(line);
            }
        } catch (IOException var5) {
            LOGGER.severe("can't read " + path);
            throw new Error(var5);
        }

        this.voice.endBatch();
        return ok;
    }

    protected Voice getVoice() {
        return this.voice;
    }

    public boolean streamToSpeech(InputStream is) {
        this.voice.startBatch();
        boolean ok = this.voice.speak(is);
        this.voice.endBatch();
        return ok;
    }

    public boolean urlToSpeech(String urlPath) {
        boolean ok = false;

        try {
            URL url = new URL(urlPath);
            InputStream is = url.openStream();
            ok = this.streamToSpeech(is);
        } catch (IOException var5) {
            System.err.println("Can't read data from " + urlPath);
        }

        return ok;
    }

    public boolean fileToSpeech(String filePath) {
        boolean ok = false;

        try {
            InputStream is = new FileInputStream(filePath);
            ok = this.streamToSpeech(is);
        } catch (IOException var4) {
            System.err.println("Can't read data from " + filePath);
        }

        return ok;
    }

    public void setSilentMode(boolean silent) {
        this.silent = silent;
    }

    public boolean getSilentMode() {
        return this.silent;
    }

    public void setInputMode(InputMode inputMode) {
        this.inputMode = inputMode;
    }

    public InputMode getInputMode() {
        return this.inputMode;
    }

    public void setAudioFile(String audioFile) {
        this.audioFile = audioFile;
    }

    public void setMultiAudio(boolean multiAudio) {
        this.multiAudio = multiAudio;
    }

    public void setStreamingAudio(boolean streamingAudio) {
        this.streamingAudio = streamingAudio;
    }

    static void usage(String voices) {
        System.out.println(VERSION);
        System.out.println("Usage:");
        System.out.println("    -detailedMetrics: turn on detailed metrics");
        System.out.println("    -dumpAudio file : dump audio to file ");
        System.out.println("    -dumpAudioTypes : dump the possible output types");
        System.out.println("    -dumpMultiAudio file : dump audio to file ");
        System.out.println("    -dumpRelations  : dump the relations ");
        System.out.println("    -dumpUtterance  : dump the final utterance");
        System.out.println("    -dumpASCII file : dump the final wave to file as ASCII");
        System.out.println("    -file file      : speak text from given file");
        System.out.println("    -lines file     : render lines from a file");
        System.out.println("    -help           : shows usage information");
        System.out.println("    -voiceInfo      : print detailed voice info");
        System.out.println("    -metrics        : turn on metrics");
        System.out.println("    -run  name      : sets the name of the run");
        System.out.println("    -silent         : don't say anything");
        System.out.println("    -streaming      : use streaming audio player");
        System.out.println("    -text say me    : speak given text");
        System.out.println("    -url path       : speak text from given URL");
        System.out.println("    -verbose        : verbose output");
        System.out.println("    -version        : shows version number");
        System.out.println("    -voice VOICE    : " + voices);
    }

    private static void interactiveMode(CustomTTS customtts) {
        try {
            while(true) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Enter text: ");
                System.out.flush();
                String text = reader.readLine();
                if (text != null && text.length() != 0) {
                    customtts.batchTextToSpeech(text);
                } else {
                    customtts.shutdown();
                    System.exit(0);
                }
            }
        } catch (IOException var3) {
        }
    }

    private static void dumpAudioTypes() {
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();

        for(int i = 0; i < types.length; ++i) {
            System.out.println(types[i].getExtension());
        }

    }

    public static void main(String[] args) {
        String text = null;
        String inFile = null;
        boolean dumpAudioTypes = false;
        Voice voice = null;
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        VoiceManager voiceManager = VoiceManager.getInstance();
        String voices = voiceManager.toString();

        for(int i = 0; i < args.length; ++i) {
            if (args[i].equals("-voice")) {
                ++i;
                if (i < args.length) {
                    String voiceName = args[i];
                    if (voiceManager.contains(voiceName)) {
                        voice = voiceManager.getVoice(voiceName);
                    } else {
                        System.out.println("Invalid voice: " + voiceName);
                        System.out.println("  Valid voices are " + voices);
                        System.exit(1);
                    }
                } else {
                    usage(voices);
                    System.exit(1);
                }
                break;
            }
        }

        if (voice == null) {
            voice = voiceManager.getVoice("kevin16");
        }

        if (voice == null) {
            throw new Error("The specified voice is not defined");
        } else {
            CustomTTS customtts = new CustomTTS(voice);

            for(int i = 0; i < args.length; ++i) {
                if (args[i].equals("-metrics")) {
                    voice.setMetrics(true);
                } else if (args[i].equals("-detailedMetrics")) {
                    voice.setDetailedMetrics(true);
                } else if (args[i].equals("-silent")) {
                    customtts.setSilentMode(true);
                } else if (args[i].equals("-streaming")) {
                    customtts.setStreamingAudio(true);
                } else if (args[i].equals("-verbose")) {
                    Handler handler = new ConsoleHandler();
                    handler.setLevel(Level.ALL);
                    Logger.getLogger("com.sun").addHandler(handler);
                    Logger.getLogger("com.sun").setLevel(Level.ALL);
                } else if (args[i].equals("-dumpUtterance")) {
                    voice.setDumpUtterance(true);
                } else if (args[i].equals("-dumpAudioTypes")) {
                    dumpAudioTypes = true;
                } else if (args[i].equals("-dumpRelations")) {
                    voice.setDumpRelations(true);
                } else if (args[i].equals("-dumpASCII")) {
                    ++i;
                    if (i < args.length) {
                        voice.setWaveDumpFile(args[i]);
                    } else {
                        usage(voices);
                    }
                } else if (args[i].equals("-dumpAudio")) {
                    ++i;
                    if (i < args.length) {
                        customtts.setAudioFile(args[i]);
                    } else {
                        usage(voices);
                    }
                } else if (args[i].equals("-dumpMultiAudio")) {
                    ++i;
                    if (i < args.length) {
                        customtts.setAudioFile(args[i]);
                        customtts.setMultiAudio(true);
                    } else {
                        usage(voices);
                    }
                } else if (args[i].equals("-version")) {
                    System.out.println(VERSION);
                } else if (args[i].equals("-voice")) {
                    ++i;
                } else if (args[i].equals("-help")) {
                    usage(voices);
                    System.exit(0);
                } else if (args[i].equals("-voiceInfo")) {
                    System.out.println(VoiceManager.getInstance().getVoiceInfo());
                    System.exit(0);
                } else {
                    if (args[i].equals("-text")) {
                        customtts.setInputMode(InputMode.TEXT);
                        StringBuffer sb = new StringBuffer();

                        for(int j = i + 1; j < args.length; ++j) {
                            sb.append(args[j]);
                            sb.append(" ");
                        }

                        text = sb.toString();
                        break;
                    }

                    if (args[i].equals("-file")) {
                        ++i;
                        if (i < args.length) {
                            inFile = args[i];
                            customtts.setInputMode(InputMode.FILE);
                        } else {
                            usage(voices);
                        }
                    } else if (args[i].equals("-lines")) {
                        ++i;
                        if (i < args.length) {
                            inFile = args[i];
                            customtts.setInputMode(InputMode.LINES);
                        } else {
                            usage(voices);
                        }
                    } else if (args[i].equals("-url")) {
                        ++i;
                        if (i < args.length) {
                            inFile = args[i];
                            customtts.setInputMode(InputMode.URL);
                        } else {
                            usage(voices);
                        }
                    } else if (args[i].equals("-run")) {
                        ++i;
                        if (i < args.length) {
                            voice.setRunTitle(args[i]);
                        } else {
                            usage(voices);
                        }
                    } else {
                        System.out.println("Unknown option:" + args[i]);
                    }
                }
            }

            if (dumpAudioTypes) {
                dumpAudioTypes();
            }

            customtts.startup();
            if (customtts.getInputMode() == InputMode.TEXT) {
                customtts.batchTextToSpeech(text);
            } else if (customtts.getInputMode() == InputMode.FILE) {
                customtts.fileToSpeech(inFile);
            } else if (customtts.getInputMode() == InputMode.URL) {
                customtts.urlToSpeech(inFile);
            } else if (customtts.getInputMode() == InputMode.LINES) {
                customtts.lineToSpeech(inFile);
            } else {
                interactiveMode(customtts);
            }

            if (customtts.getVoice().isMetrics() && !customtts.getSilentMode()) {
            }

            customtts.shutdown();
            System.exit(0);
        }
    }

    static {
        LOGGER = Logger.getLogger(CustomTTS.class.getName());
        audioPlayer = null;
    }
}
