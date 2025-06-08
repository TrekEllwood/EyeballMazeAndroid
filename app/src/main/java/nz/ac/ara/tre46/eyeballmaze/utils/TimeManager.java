package nz.ac.ara.tre46.eyeballmaze.utils;

import android.os.Handler;

public class TimeManager {
    private long startTime = 0L;
    private long pausedTime = 0L;
    private long solveTimeMillis = 0L;
    private boolean isPaused = false;
    private boolean hasTimerStarted = false;

    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    public interface TimeUpdateListener {
        void onTimeUpdate(long elapsedMillis);
    }

    public void startTimer(TimeUpdateListener listener) {
        if (hasTimerStarted) return;

        startTime = System.currentTimeMillis();
        hasTimerStarted = true;
        isPaused = false;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    listener.onTimeUpdate(elapsed);
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        timerHandler.post(timerRunnable);
    }

    public void pauseTimer() {
        if (hasTimerStarted && !isPaused) {
            pausedTime = System.currentTimeMillis();
            isPaused = true;
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    public void resumeTimer(TimeUpdateListener listener) {
        if (hasTimerStarted && isPaused) {
            long pausedDuration = System.currentTimeMillis() - pausedTime;
            startTime += pausedDuration;
            isPaused = false;

            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isPaused) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        listener.onTimeUpdate(elapsed);
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            };
            timerHandler.post(timerRunnable);
        }
    }

    public void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        hasTimerStarted = false;
        isPaused = false;
    }

    public long getElapsedTime() {
        if (isPaused) {
            return pausedTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public boolean isSolved() {
        return solveTimeMillis > 0;
    }

    public void setSolved(TimeUpdateListener listener) {
        solveTimeMillis = getElapsedTime();
        listener.onTimeUpdate(solveTimeMillis);
        stopTimer();
    }

    public void clearSolved() {
        solveTimeMillis = 0L;
        stopTimer();
    }

    public long getSolveTimeMillis() {
        return solveTimeMillis;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean hasStarted() {
        return hasTimerStarted;
    }

    public void setHasTimerStarted(boolean started) {
        this.hasTimerStarted = started;
    }

    public void restoreState(long startTime, boolean hasStarted, boolean isPaused, long pausedTime, long solveTimeMillis) {
        this.startTime = startTime;
        this.hasTimerStarted = hasStarted;
        this.isPaused = isPaused;
        this.pausedTime = pausedTime;
        this.solveTimeMillis = solveTimeMillis;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getPausedTime() {
        return pausedTime;
    }
}
