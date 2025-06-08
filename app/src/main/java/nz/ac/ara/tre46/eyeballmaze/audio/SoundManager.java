package nz.ac.ara.tre46.eyeballmaze.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import nz.ac.ara.tre46.eyeballmaze.R;

public class SoundManager {
    private final SoundPool soundPool;
    private final int moveSoundId, winSoundId, badSoundId;
    private boolean soundsLoaded = false;
    private boolean isMuted = false;

    public SoundManager(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        moveSoundId = soundPool.load(context, R.raw.move, 1);
        winSoundId = soundPool.load(context, R.raw.win, 1);
        badSoundId = soundPool.load(context, R.raw.bad, 1);

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) soundsLoaded = true;
        });
    }

    public void playMove() { play(moveSoundId); }
    public void playWin() { play(winSoundId); }
    public void playBad() { play(badSoundId); }

    private void play(int soundId) {
        if (!isMuted && soundsLoaded) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void release() {
        soundPool.release();
    }
}
