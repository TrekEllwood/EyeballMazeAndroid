package nz.ac.ara.tre46.eyeballmaze.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Set;
import java.util.Deque;
import java.util.ArrayDeque;

import android.graphics.Point;


import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;

public class EyeballMazeViewModel extends ViewModel {
    private final IGame game;

    private final MutableLiveData<Integer> rowLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> colLiveData = new MutableLiveData<>();
    private final MutableLiveData<Direction> dirLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> currentGoalLiveData = new MutableLiveData<>();
    private final MutableLiveData<Message> moveStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> goalsRemaining = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canUndoLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> moveCount = new MutableLiveData<>(0);

    private final Deque<Object> undoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 10;
    private int currentLevelIndex = 0;

    public EyeballMazeViewModel(IGame gameInstance) {
        this.game = gameInstance;
        syncGameState();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

//    public IGame getGame() {
//        return game;
//    }

    // === Accessors ===
    public LiveData<Integer> getEyeballRow() {
        return rowLiveData;
    }

    public LiveData<Integer> getEyeballCol() {
        return colLiveData;
    }

    public LiveData<Direction> getEyeballDir() {
        return dirLiveData;
    }

    public LiveData<Boolean> isCurrentGoal() {
        return currentGoalLiveData;
    }

    public LiveData<Message> getMoveStatus() {
        return moveStatusLiveData;
    }

    public LiveData<Integer> getGoalsRemaining() {
        return goalsRemaining;
    }

    public LiveData<Boolean> canUndoLiveData() {
        return canUndoLiveData;
    }

    public LiveData<Integer> getMoveCount() {
        return moveCount;
    }

    public void clearMoveStatus() {
        moveStatusLiveData.setValue(null);
    }

    public void resetMaze() {
        game.resetCurrentLevel();
        moveCount.setValue(0);
        resetUndo();
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

    public SquareType getTypeAt(int row, int col) {
        return game.getTypeAt(row, col);
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
            Integer current = moveCount.getValue();
            moveCount.setValue((current != null ? current : 0) + 1);
        } else {
            moveStatusLiveData.setValue(game.messageIfMovingTo(row, col));
        }
        syncGameState();
    }

    // === Goal operations ===
    public boolean hasGoalAt(int row, int col) {
        return game.hasGoalAt(row, col);
    }

    public void removeGoalAt(int row, int col) {
        game.removeGoalAt(row, col);
        syncGameState();
    }

    public void completeCurrentGoal() {
        if (game.isCurrentSquareGoal()) {
            game.setCurrentSquareGoal(false);
            game.completedGoal(game.getEyeballRow(), game.getEyeballColumn());
        }
        syncGameState();
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
        if (index == currentLevelIndex) return; // Prevent redundant reload
        game.setLevel(index);
        currentLevelIndex = index;
        moveCount.setValue(0);
        resetUndo();
        syncGameState();
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

        goalsRemaining.setValue(count);
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
            moveCount.setValue(Math.max(0, moveCount.getValue() != null ? moveCount.getValue() - 1 : 0));
            syncGameState();
        }
        canUndoLiveData.setValue(!undoStack.isEmpty());
    }

    public void resetUndo() {
        undoStack.clear();
        canUndoLiveData.setValue(false);
    }
}
