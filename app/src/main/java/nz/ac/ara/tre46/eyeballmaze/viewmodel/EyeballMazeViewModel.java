package nz.ac.ara.tre46.eyeballmaze.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Deque;
import java.util.ArrayDeque;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;
import nz.ac.ara.tre46.eyeballmaze.utils.TimeManager;

public class EyeballMazeViewModel extends ViewModel {
    private final IGame game;
    private final Context context;
    private final Deque<Object> undoStack = new ArrayDeque<>();
    private final Deque<Point> movePlaybackTrail = new ArrayDeque<>();
    private static final int MAX_UNDO = 10;
    private int currentLevelIndex = 0;
    private final TimeManager timeManager = new TimeManager();

    private final MutableLiveData<Integer> rowLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> colLiveData = new MutableLiveData<>();
    private final MutableLiveData<Direction> dirLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> currentGoalLiveData = new MutableLiveData<>();
    private final MutableLiveData<Message> moveStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> goalsRemainingLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canUndoLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> moveCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> moveHappenedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canReplayLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isPausedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Long> elapsedTimeLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isTimerStartedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSolvedLiveData = new MutableLiveData<>(false);

    public EyeballMazeViewModel(Context context, IGame gameInstance) {
        this.context = context.getApplicationContext();
        this.game = gameInstance;
        syncGameState();
    }

    // === Accessors ===
    public LiveData<Integer> getEyeballRowLiveData() {
        return rowLiveData;
    }

    public LiveData<Integer> getEyeballColLiveData() {
        return colLiveData;
    }

    public LiveData<Point> getEyeballPositionLiveData() {
        MediatorLiveData<Point> posLiveData = new MediatorLiveData<>();

        posLiveData.addSource(getEyeballRowLiveData(), row -> {
            Integer col = getEyeballColLiveData().getValue();
            if (col != null && row != null) {
                posLiveData.setValue(new Point(col, row));
            }
        });

        posLiveData.addSource(getEyeballColLiveData(), col -> {
            Integer row = getEyeballRowLiveData().getValue();
            if (col != null && row != null) {
                posLiveData.setValue(new Point(col, row));
            }
        });

        return posLiveData;
    }

    public LiveData<Direction> getEyeballDirLiveData() {
        return dirLiveData;
    }

    public LiveData<Boolean> isCurrentGoalLiveData() {
        return currentGoalLiveData;
    }

    public LiveData<Message> getMoveStatusLiveData() {
        return moveStatusLiveData;
    }

    public LiveData<Integer> getGoalsRemainingLiveData() {
        return goalsRemainingLiveData;
    }

    public LiveData<Boolean> canUndoLiveData() {
        return canUndoLiveData;
    }

    public LiveData<Integer> getMoveCountLiveData() {
        return moveCountLiveData;
    }

    public LiveData<Boolean> getMoveHappenedLiveData() {
        return moveHappenedLiveData;
    }

    public LiveData<Boolean> getCanReplayLiveData() {
        return canReplayLiveData;
    }

    public LiveData<Boolean> getIsPausedLiveData() {
        return isPausedLiveData;
    }

    public LiveData<Long> getElapsedTimeLiveData() {
        return elapsedTimeLiveData;
    }

    public LiveData<Boolean> getIsTimerStartedLiveData() {
        return isTimerStartedLiveData;
    }

    public LiveData<Boolean> getIsSolvedLiveData() {
        return isSolvedLiveData;
    }

    public void clearMoveStatusLiveData() {
        moveStatusLiveData.setValue(null);
    }

    public void resetMaze() {
        game.resetCurrentLevel();
        moveCountLiveData.setValue(0);
        resetUndo();
        resetTimer();
        syncGameState();
    }

    public int getCurrentMazeId() {
        return game.getCurrentMazeId();
    }

    public int getBoardWidth() {
        return game.getBoardWidth();
    }

    public int getBoardHeight() {
        return game.getBoardHeight();
    }

    public Color getColorAt(int row, int col) {
        return game.getColorAt(row, col);
    }

    public Shape getShapeAt(int row, int col) {
        return game.getShapeAt(row, col);
    }

    public void clickToMoveToward(int row, int col) {
        if (game.canMoveTo(row, col)) {
            pushUndoState();
            game.moveTo(row, col);
            Integer current = moveCountLiveData.getValue();
            moveCountLiveData.setValue((current != null ? current : 0) + 1);
            moveHappenedLiveData.setValue(true);
        } else {
            moveStatusLiveData.setValue(game.messageIfMovingTo(row, col));
        }
        syncGameState();
    }

    public boolean canMoveTo(int row, int col) {
        return game.canMoveTo(row, col);
    }

    public Set<Point> getGoalPoints() {
        return game.getRemainingGoalPoints();
    }

    // === Internal sync ===
    public void syncGameState() {
        rowLiveData.setValue(game.getEyeballRow());
        colLiveData.setValue(game.getEyeballColumn());
        dirLiveData.setValue(game.getEyeballDirection());
        currentGoalLiveData.setValue(game.isCurrentSquareGoal());
        updateGoalsRemaining();
    }

    public int getLevelCount() {
        return game.getLevelCount();
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public void setLevel(int index) {
        if (index < 0 || index >= game.getLevelCount()) return;
        if (index == currentLevelIndex) return; // Prevent redundant reload

        game.setLevel(index);
        currentLevelIndex = index;
        moveCountLiveData.setValue(0);
        clearSolved();
        resetUndo();
        syncGameState();

        // Persist the level
        SharedPreferences prefs = context.getSharedPreferences("EyeballMazePrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("last_level_index", index).apply();
    }

    public void initializeLevelFromPreferences() {
        SharedPreferences prefs = context.getSharedPreferences("EyeballMazePrefs", Context.MODE_PRIVATE);
        int lastLevel = prefs.getInt("last_level_index", 0);

        if (lastLevel < game.getLevelCount()) {
            setLevel(lastLevel);
        } else {
            setLevel(0);
        }
    }

    public int getMazeIdAt(int index) {
        return game.getMazeIdAt(index);
    }

    private void updateGoalsRemaining() {
        Set<Point> remainingGoals = game.getRemainingGoalPoints();
        int count = remainingGoals.size();

        int row = game.getEyeballRow();
        int col = game.getEyeballColumn();

        if (remainingGoals.contains(new Point(col, row))) {
            count -= 1;
        }

        goalsRemainingLiveData.setValue(count);
    }

    private void pushUndoState() {
        undoStack.push(game.saveState());
        if (undoStack.size() > MAX_UNDO) {
            undoStack.removeLast();
        }
        canUndoLiveData.setValue(true);
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Object previous = undoStack.pop();
            game.loadState(previous);
            moveCountLiveData.setValue(Math.max(0, moveCountLiveData.getValue() != null ? moveCountLiveData.getValue() - 1 : 0));
            syncGameState();
        }
        canUndoLiveData.setValue(!undoStack.isEmpty());
    }

    public void resetUndo() {
        undoStack.clear();
        canUndoLiveData.setValue(false);
    }

    public void clearMoveHappened() {
        moveHappenedLiveData.setValue(false);
    }

    public void updateCanReplay(Collection<Point> trail) {
        canReplayLiveData.setValue(trail.size() > 1);
    }

    public void clearPlaybackTrail() {
        movePlaybackTrail.clear();
    }

    public void addMoveToTrail(Point point) {
        movePlaybackTrail.add(point);
        updateCanReplay(movePlaybackTrail);
    }

    public void removeLastFromTrail() {
        if (!movePlaybackTrail.isEmpty()) {
            movePlaybackTrail.removeLast();
            updateCanReplay(movePlaybackTrail);
        }
    }

    public List<Point> getPlaybackTrail() {
        return new ArrayList<>(movePlaybackTrail);
    }

    public void restorePlaybackTrail(List<Point> trail) {
        movePlaybackTrail.clear();
        movePlaybackTrail.addAll(trail);
        updateCanReplay(movePlaybackTrail);
    }

    public void startTimer() {
        timeManager.startTimer(elapsedTimeLiveData::postValue);
        isTimerStartedLiveData.setValue(true);
        isPausedLiveData.setValue(false);
    }

    public void pauseTimer() {
        timeManager.pauseTimer();
        isPausedLiveData.setValue(true);
    }

    public void resumeTimer() {
        timeManager.resumeTimer(elapsedTimeLiveData::postValue);
        isPausedLiveData.setValue(false);
    }

    public void stopTimer() {
        timeManager.stopTimer();
        isTimerStartedLiveData.setValue(false);
        isPausedLiveData.setValue(false);
    }

    public void markSolved() {
        if (!isSolved()) {
            timeManager.setSolved(elapsedTimeLiveData::postValue);
            isSolvedLiveData.setValue(true);
        }
    }

    public void clearSolved() {
        timeManager.clearSolved();
        isSolvedLiveData.setValue(false);
    }

    public boolean isSolved() {
        return timeManager.isSolved();
    }

    public long getSolveTimeMillis() {
        return timeManager.getSolveTimeMillis();
    }

    public boolean isPaused() {
        return timeManager.isPaused();
    }

    public boolean isTimerStarted() {
        return timeManager.hasStarted();
    }

    public void resetTimer() {
        timeManager.setHasTimerStarted(false);
        isTimerStartedLiveData.setValue(false);
        elapsedTimeLiveData.setValue(0L);
    }

    public void saveTimerStateToBundle(Bundle outState) {
        outState.putLong("start_time", timeManager.getStartTime());
        outState.putBoolean("has_timer_started", timeManager.hasStarted());
        outState.putBoolean("is_paused", timeManager.isPaused());
        outState.putLong("paused_time", timeManager.getPausedTime());
        outState.putLong("solve_time_millis", timeManager.getSolveTimeMillis());
    }

    public void restoreTimerStateFromBundle(Bundle savedInstanceState) {
        long startTime = savedInstanceState.getLong("start_time", 0L);
        boolean hasStarted = savedInstanceState.getBoolean("has_timer_started", false);
        boolean isPaused = savedInstanceState.getBoolean("is_paused", false);
        long pausedTime = savedInstanceState.getLong("paused_time", 0L);
        long solveTimeMillis = savedInstanceState.getLong("solve_time_millis", 0L);

        timeManager.restoreState(startTime, hasStarted, isPaused, pausedTime, solveTimeMillis);
        isTimerStartedLiveData.setValue(timeManager.hasStarted());
        isPausedLiveData.setValue(isPaused);
    }
}
