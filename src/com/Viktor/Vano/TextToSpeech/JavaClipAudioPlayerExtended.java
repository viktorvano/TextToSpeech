package com.Viktor.Vano.TextToSpeech;

import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.util.BulkTimer;
import com.sun.speech.freetts.util.Timer;
import com.sun.speech.freetts.util.Utilities;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.*;
import javax.sound.sampled.FloatControl.Type;

public class JavaClipAudioPlayerExtended implements AudioPlayer {
    private static final Logger LOGGER;
    private volatile boolean paused;
    private volatile boolean cancelled = false;
    private volatile Clip currentClip;
    private float volume = 1.0F;
    private boolean audioMetrics = false;
    private final BulkTimer timer = new BulkTimer();
    private AudioFormat defaultFormat = new AudioFormat(8000.0F, 16, 1, true, true);
    private AudioFormat currentFormat;
    private boolean firstSample;
    private boolean firstPlay;
    private int curIndex;
    private final PipedOutputStream outputData;
    private AudioInputStream audioInput;
    private final LineListener lineListener;
    private long drainDelay;
    private long openFailDelayMs;
    private long totalOpenFailDelayMs;

    private Mixer.Info mixerInfo;

    public JavaClipAudioPlayerExtended(Mixer.Info info) {
        this.mixerInfo = info;
        this.currentFormat = this.defaultFormat;
        this.firstSample = true;
        this.firstPlay = true;
        this.curIndex = 0;
        this.drainDelay = Utilities.getLong("com.sun.speech.freetts.audio.AudioPlayer.drainDelay", 150L);
        this.openFailDelayMs = Utilities.getLong("com.sun.speech.freetts.audio.AudioPlayer.openFailDelayMs", 0L);
        this.totalOpenFailDelayMs = Utilities.getLong("com.sun.speech.freetts.audio.AudioPlayer.totalOpenFailDelayMs", 0L);
        this.audioMetrics = Utilities.getBoolean("com.sun.speech.freetts.audio.AudioPlayer.showAudioMetrics");
        this.setPaused(false);
        this.outputData = new PipedOutputStream();
        this.lineListener = new JavaClipAudioPlayerExtended.JavaClipLineListener();
    }

    public synchronized void setAudioFormat(AudioFormat format) {
        if (!this.currentFormat.matches(format)) {
            this.currentFormat = format;
            if (this.currentClip != null) {
                this.currentClip = null;
            }

        }
    }

    public AudioFormat getAudioFormat() {
        return this.currentFormat;
    }

    public void pause() {
        if (!this.paused) {
            this.setPaused(true);
            if (this.currentClip != null) {
                this.currentClip.stop();
            }

            synchronized(this) {
                this.notifyAll();
            }
        }

    }

    public synchronized void resume() {
        if (this.paused) {
            this.setPaused(false);
            if (this.currentClip != null) {
                this.currentClip.start();
            }

            this.notifyAll();
        }

    }

    public void cancel() {
        if (this.audioMetrics) {
            this.timer.start("audioCancel");
        }

        if (this.currentClip != null) {
            this.currentClip.stop();
            this.currentClip.close();
        }

        synchronized(this) {
            this.cancelled = true;
            this.paused = false;
            this.notifyAll();
        }

        if (this.audioMetrics) {
            this.timer.stop("audioCancel");
            Timer.showTimesShortTitle("");
            this.timer.getTimer("audioCancel").showTimesShort(0L);
        }

    }

    public synchronized void reset() {
        this.timer.start("speakableOut");
    }

    public boolean drain() {
        this.timer.stop("speakableOut");
        return true;
    }

    public synchronized void close() {
        if (this.currentClip != null) {
            this.currentClip.drain();
            if (this.drainDelay > 0L) {
                try {
                    Thread.sleep(this.drainDelay);
                } catch (InterruptedException var2) {
                }
            }

            this.currentClip.close();
        }

        this.notifyAll();
    }

    public float getVolume() {
        return this.volume;
    }

    public void setVolume(float volume) {
        if (volume > 1.0F) {
            volume = 1.0F;
        }

        if (volume < 0.0F) {
            volume = 0.0F;
        }

        this.volume = volume;
        if (this.currentClip != null) {
            this.setVolume(this.currentClip, volume);
        }

    }

    private void setPaused(boolean state) {
        this.paused = state;
    }

    private void setVolume(Clip clip, float vol) {
        if (clip.isControlSupported(Type.MASTER_GAIN)) {
            FloatControl volumeControl = (FloatControl)clip.getControl(Type.MASTER_GAIN);
            float range = volumeControl.getMaximum() - volumeControl.getMinimum();
            volumeControl.setValue(vol * range + volumeControl.getMinimum());
        }

    }

    public synchronized long getTime() {
        return -1L;
    }

    public synchronized void resetTime() {
    }

    public synchronized void begin(int size) {
        this.timer.start("utteranceOutput");
        this.cancelled = false;
        this.curIndex = 0;

        try {
            PipedInputStream in = new PipedInputStream(this.outputData);
            this.audioInput = new AudioInputStream(in, this.currentFormat, (long)size);
        } catch (IOException var12) {
            LOGGER.warning(var12.getLocalizedMessage());
        }

        while(this.paused && !this.cancelled) {
            try {
                this.wait();
            } catch (InterruptedException var11) {
                return;
            }
        }

        this.timer.start("clipGeneration");
        boolean opened = false;
        long totalDelayMs = 0L;

        do {
            try {
                this.currentClip = this.getClip();
                this.currentClip.open(this.audioInput);
                opened = true;
            } catch (LineUnavailableException var9) {
                System.err.println("LINE UNAVAILABLE: Format is " + this.currentFormat);

                try {
                    Thread.sleep(this.openFailDelayMs);
                    totalDelayMs += this.openFailDelayMs;
                } catch (InterruptedException var8) {
                    var8.printStackTrace();
                }
            } catch (IOException var10) {
                LOGGER.warning(var10.getLocalizedMessage());
            }
        } while(!opened && totalDelayMs < this.totalOpenFailDelayMs);

        if (!opened) {
            this.close();
        } else {
            this.setVolume(this.currentClip, this.volume);
            if (this.audioMetrics && this.firstPlay) {
                this.firstPlay = false;
                this.timer.stop("firstPlay");
                this.timer.getTimer("firstPlay");
                Timer.showTimesShortTitle("");
                this.timer.getTimer("firstPlay").showTimesShort(0L);
            }

            this.currentClip.start();
        }

    }

    private Clip getClip() throws LineUnavailableException {
        if (this.currentClip == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("creating new clip");
            }

            //DataLine.Info info = new DataLine.Info(Clip.class, this.currentFormat);

            try {
                this.currentClip = AudioSystem.getClip(this.mixerInfo);//(Clip)AudioSystem.getLine(info);
                this.currentClip.addLineListener(this.lineListener);
            } catch (SecurityException var3) {
                throw new LineUnavailableException(var3.getLocalizedMessage());
            } catch (IllegalArgumentException var4) {
                throw new LineUnavailableException(var4.getLocalizedMessage());
            }
        }

        return this.currentClip;
    }

    public synchronized boolean end() {
        boolean ok = true;
        if (this.cancelled) {
            return false;
        } else {
            if (this.currentClip != null && this.currentClip.isOpen()) {
                this.setVolume(this.currentClip, this.volume);
                if (this.audioMetrics && this.firstPlay) {
                    this.firstPlay = false;
                    this.timer.stop("firstPlay");
                    this.timer.getTimer("firstPlay");
                    Timer.showTimesShortTitle("");
                    this.timer.getTimer("firstPlay").showTimesShort(0L);
                }

                try {
                    while(this.currentClip != null && (this.currentClip.isRunning() || this.paused) && !this.cancelled) {
                        this.wait();
                    }
                } catch (InterruptedException var3) {
                    ok = false;
                }

                this.close();
            } else {
                this.close();
                ok = false;
            }

            this.timer.stop("clipGeneration");
            this.timer.stop("utteranceOutput");
            ok &= !this.cancelled;
            return ok;
        }
    }

    public boolean write(byte[] audioData) {
        return this.write(audioData, 0, audioData.length);
    }

    public boolean write(byte[] bytes, int offset, int size) {
        if (this.firstSample) {
            this.firstSample = false;
            this.timer.stop("firstAudio");
            if (this.audioMetrics) {
                Timer.showTimesShortTitle("");
                this.timer.getTimer("firstAudio").showTimesShort(0L);
            }
        }

        try {
            this.outputData.write(bytes, offset, size);
        } catch (IOException var5) {
            LOGGER.warning(var5.getLocalizedMessage());
            return false;
        }

        this.curIndex += size;
        return true;
    }

    public String toString() {
        return "JavaClipAudioPlayerExtended";
    }

    public void showMetrics() {
        this.timer.show(this.toString());
    }

    public void startFirstSampleTimer() {
        this.timer.start("firstAudio");
        this.firstSample = true;
        if (this.audioMetrics) {
            this.timer.start("firstPlay");
            this.firstPlay = true;
        }

    }

    static {
        LOGGER = Logger.getLogger(JavaClipAudioPlayerExtended.class.getName());
    }

    private class JavaClipLineListener implements LineListener {
        private JavaClipLineListener() {
        }

        public void update(LineEvent event) {
            if (event.getType().equals(javax.sound.sampled.LineEvent.Type.START)) {
                if (JavaClipAudioPlayerExtended.LOGGER.isLoggable(Level.FINE)) {
                    JavaClipAudioPlayerExtended.LOGGER.fine(this.toString() + ": EVENT START");
                }
            } else if (event.getType().equals(javax.sound.sampled.LineEvent.Type.STOP)) {
                if (JavaClipAudioPlayerExtended.LOGGER.isLoggable(Level.FINE)) {
                    JavaClipAudioPlayerExtended.LOGGER.fine(this.toString() + ": EVENT STOP");
                }

                synchronized(JavaClipAudioPlayerExtended.this) {
                    JavaClipAudioPlayerExtended.this.notifyAll();
                }
            } else if (event.getType().equals(javax.sound.sampled.LineEvent.Type.OPEN)) {
                if (JavaClipAudioPlayerExtended.LOGGER.isLoggable(Level.FINE)) {
                    JavaClipAudioPlayerExtended.LOGGER.fine(this.toString() + ": EVENT OPEN");
                }
            } else if (event.getType().equals(javax.sound.sampled.LineEvent.Type.CLOSE)) {
                if (JavaClipAudioPlayerExtended.LOGGER.isLoggable(Level.FINE)) {
                    JavaClipAudioPlayerExtended.LOGGER.fine(this.toString() + ": EVENT CLOSE");
                }

                synchronized(JavaClipAudioPlayerExtended.this) {
                    JavaClipAudioPlayerExtended.this.notifyAll();
                }
            }

        }
    }
}
