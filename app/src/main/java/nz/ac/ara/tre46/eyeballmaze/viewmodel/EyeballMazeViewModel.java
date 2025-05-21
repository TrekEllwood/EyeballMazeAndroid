package nz.ac.ara.tre46.eyeballmaze.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.ArrayList;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;
import nz.ac.ara.tre46.eyeballmaze.models.BlankSquare;
import nz.ac.ara.tre46.eyeballmaze.models.PlayableSquare;
import nz.ac.ara.tre46.eyeballmaze.models.Square;

public class EyeballMazeViewModel extends ViewModel {
    private final IGame game;

    private final MutableLiveData<Square[][]> boardLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> rowLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> colLiveData = new MutableLiveData<>();
    private final MutableLiveData<Direction> dirLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> currentGoalLiveData = new MutableLiveData<>();
    private final MutableLiveData<Message> moveStatusLiveData = new MutableLiveData<>();

    public EyeballMazeViewModel(IGame gameInstance) {
        this.game = gameInstance;
        syncGameState();
    }

    // === Accessors ===
    public LiveData<Square[][]> getBoard() { return boardLiveData; }
    public LiveData<Integer> getEyeballRow() { return rowLiveData; }
    public LiveData<Integer> getEyeballCol() { return colLiveData; }
    public LiveData<Direction> getEyeballDir() { return dirLiveData; }
    public LiveData<Boolean> isCurrentGoal() { return currentGoalLiveData; }
    public LiveData<Message> getMoveStatus() { return moveStatusLiveData; }

    public void clearMoveStatus() {
        moveStatusLiveData.setValue(null);
    }

    public void resetMaze() {
        game.resetCurrentLevel();
        syncGameState();
    }

//    public void clickToMoveInDirection() {
//        int row = game.getEyeballRow();
//        int col = game.getEyeballColumn();
//        Direction dir = game.getEyeballDirection();
//
//        int targetRow = row, targetCol = col;
//        switch (dir) {
//            case UP    -> targetRow--;
//            case DOWN  -> targetRow++;
//            case LEFT  -> targetCol--;
//            case RIGHT -> targetCol++;
//        }
//
//        Message status = game.messageIfMovingTo(targetRow, targetCol);
//        if (status == Message.OK) {
//            game.moveTo(targetRow, targetCol);
//            syncGameState();
//        }
//
//        moveStatusLiveData.setValue(status);
//    }

//    public void moveEyeball(Direction direction) {
//        int newRow = game.getEyeballRow();
//        int newCol = game.getEyeballColumn();
//
//        switch (direction) {
//            case UP:    newRow--; break;
//            case DOWN:  newRow++; break;
//            case LEFT:  newCol--; break;
//            case RIGHT: newCol++; break;
//        }
//
//        if (game.canMoveTo(newRow, newCol)) {
//            game.moveTo(newRow, newCol);
//            syncGameState();
//        } else {
//            moveStatusLiveData.setValue(game.messageIfMovingTo(newRow, newCol));
//        }
//    }

    public void clickToMoveToward(int row, int col) {
        if (game.canMoveTo(row, col)) {
            game.moveTo(row, col);
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

    // === Level loader via interfaces ===
    public void loadLevelFromText(String[] lines) {
        int lineIndex = 0;

        // --- Parse Metadata ---
        while (lineIndex < lines.length && lines[lineIndex].startsWith("#")) lineIndex++;
        if (lineIndex >= lines.length) return;

        String[] metadata = lines[lineIndex++].trim().split(",");
        if (metadata.length < 5) return;

        int startRow = Integer.parseInt(metadata[1].trim());
        int startCol = Integer.parseInt(metadata[2].trim());
        Direction startDir = parseDir(metadata[3].trim());

//        String[] startParts = metadata[4].trim().split("_");
//        Color startColor = parseColor(startParts[0].trim());
//        Shape startShape = parseShape(startParts[1].trim());

        while (lineIndex < lines.length && (lines[lineIndex].startsWith("#") || lines[lineIndex].trim().isEmpty())) {
            lineIndex++;
        }

        List<String[]> gridTokens = new ArrayList<>();
        while (lineIndex < lines.length && !lines[lineIndex].startsWith("#") && !lines[lineIndex].trim().isEmpty()) {
            gridTokens.add(lines[lineIndex++].trim().split(","));
        }

        int gridHeight = gridTokens.size();
        int gridWidth = gridTokens.get(0).length;

        game.resetBoard(gridWidth, gridHeight); // JUST ADDED! OK

        // --- Create board ---
        for (int r = 0; r < gridHeight; r++) {
            String[] row = gridTokens.get(r);
            for (int c = 0; c < gridWidth; c++) {
                String token = row[c].trim();
                String[] parts = token.split("_");
                String colorStr = parts.length > 0 ? parts[0].trim() : "BLANK";
                String shapeStr = parts.length > 1 ? parts[1].trim() : "BLANK";

                Color color = parseColor(colorStr);
                Shape shape = parseShape(shapeStr);

                if (color == Color.BLANK && shape == Shape.BLANK) {
                    game.addSquare(new BlankSquare(), r, c);
                } else {
                    game.addSquare(new PlayableSquare(color, shape), r, c);
                }
            }
        }

        while (lineIndex < lines.length && !lines[lineIndex].toLowerCase().contains("goal")) {
            lineIndex++;
        }
        lineIndex++; // skip "Goal Coordinates" comment line

        // --- Parse goals ---
        while (lineIndex < lines.length) {
            String line = lines[lineIndex++].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] goalCords = line.split(",");
            if (goalCords.length < 2) continue;

            int goalRow = Integer.parseInt(goalCords[0].trim());
            int goalCol = Integer.parseInt(goalCords[1].trim());

            game.addGoal(goalRow, goalCol);
        }

        // --- Place eyeball ---
        game.addEyeball(startRow, startCol, startDir);

        syncGameState();
    }

    private Color parseColor(String colorStr) {
        return Color.valueOf(colorStr.toUpperCase());
    }

    private Shape parseShape(String shapeStr) {
        return Shape.valueOf(shapeStr.toUpperCase());
    }

    private Direction parseDir(String d) {
        return switch (d.toLowerCase()) {
            case "u" -> Direction.UP;
            case "d" -> Direction.DOWN;
            case "l" -> Direction.LEFT;
            default -> Direction.RIGHT;
        };
    }

    // === Internal sync ===
    public void syncGameState() {
        rowLiveData.setValue(game.getEyeballRow());
        colLiveData.setValue(game.getEyeballColumn());
        dirLiveData.setValue(game.getEyeballDirection());
        boardLiveData.setValue(game.getBoardData());
        currentGoalLiveData.setValue(game.isCurrentSquareGoal());
    }
}
