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
import nz.ac.ara.tre46.eyeballmaze.interfaces.IEyeballHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoalHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMoving;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ISquareHolder;
import nz.ac.ara.tre46.eyeballmaze.models.BlankSquare;
import nz.ac.ara.tre46.eyeballmaze.models.PlayableSquare;
import nz.ac.ara.tre46.eyeballmaze.models.Square;

public class EyeballMazeViewModel extends ViewModel {
    private final IGame game;

    private final MutableLiveData<Square[][]> boardLiveData    = new MutableLiveData<>();
    private final MutableLiveData<Integer>    rowLiveData      = new MutableLiveData<>();
    private final MutableLiveData<Integer>    colLiveData      = new MutableLiveData<>();
    private final MutableLiveData<Direction>  dirLiveData      = new MutableLiveData<>();
    private final MutableLiveData<Boolean>    currentGoalLiveData = new MutableLiveData<>();

    public EyeballMazeViewModel(IGame gameInstance) {
        this.game = gameInstance;
        syncGameState();
    }

    // === Accessors ===
    public LiveData<Square[][]> getBoard()       { return boardLiveData;    }
    public LiveData<Integer>    getEyeballRow()  { return rowLiveData;      }
    public LiveData<Integer>    getEyeballCol()  { return colLiveData;      }
    public LiveData<Direction>  getEyeballDir()  { return dirLiveData;      }
    public LiveData<Boolean>    isCurrentGoal()   { return currentGoalLiveData; }

    // Movement with validation
    public Message checkMove(Direction direction) {
        int row = game.getEyeballRow();
        int col = game.getEyeballColumn();
        switch (direction) {
            case UP:    row--; break;
            case DOWN:  row++; break;
            case LEFT:  col--; break;
            case RIGHT: col++; break;
        }
        if (game instanceof IMoving) {
            IMoving moving = (IMoving) game;
            return moving.messageIfMovingTo(row, col);
        }
        return Message.OK;
    }

    public void moveEyeball(Direction direction) {
        int newRow = game.getEyeballRow();
        int newCol = game.getEyeballColumn();

        switch (direction) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
        }

        if (game instanceof IMoving) {
            IMoving moving = (IMoving) game;
            if (moving.canMoveTo(newRow, newCol)) {
                moving.moveTo(newRow, newCol);
                syncGameState();
            }
        }
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
    /**
     * Load a level defined by '.' = blank, 'G' = goal
     */
//    public void loadLevelFromText(String[] rows,
//                                  int startRow,
//                                  int startCol,
//                                  String startDir) {
//        int height = rows.length;
//        int width = rows[0].length();
//
//        // Reset and fill board
//        if (game instanceof ISquareHolder) {
//            ISquareHolder sh = (ISquareHolder) game;
//            sh.resetBoard(width, height);
//            for (int r = 0; r < height; r++) {
//                for (int c = 0; c < width; c++) {
//                    char ch = rows[r].charAt(c);
//                    if (ch == '.') {
//                        sh.addSquare(new BlankSquare(), r, c);
//                    } else {
//                        // place a default playable square
//                        sh.addSquare(new PlayableSquare(Color.GREEN, Shape.DIAMOND), r, c);
//                    }
//                }
//            }
//        }
//
//        // Add goals
//        if (game instanceof IGoalHolder) {
//            IGoalHolder gh = (IGoalHolder) game;
//            for (int r = 0; r < height; r++) {
//                for (int c = 0; c < width; c++) {
//                    if (rows[r].charAt(c) == 'G') {
//                        gh.addGoal(r, c);
//                    }
//                }
//            }
//        }
//
//        // Position eyeball
//        if (game instanceof IEyeballHolder) {
//            IEyeballHolder eh = (IEyeballHolder) game;
//            eh.addEyeball(startRow, startCol, parseDir(startDir));
//        }
//
//        syncGameState();
//    }

    public void loadLevelFromText(String[] lines) {
        int lineIndex = 0;

        // --- Parse Metadata ---
        while (lineIndex < lines.length && lines[lineIndex].startsWith("#")) lineIndex++;
        if (lineIndex >= lines.length) return;

        String[] metadata = lines[lineIndex++].trim().split(",");
        if (metadata.length < 5) return; // Prevent crash
        int startRow = Integer.parseInt(metadata[1].trim());
        int startCol = Integer.parseInt(metadata[2].trim());
        Direction startDir = parseDir(metadata[3].trim());
        String[] startParts = metadata[4].trim().split("_");
        Color startColor = parseColor(startParts[0].trim());
        Shape startShape = parseShape(startParts[1].trim());

        // --- Skip to grid ---
        while (lineIndex < lines.length && (lines[lineIndex].startsWith("#") || lines[lineIndex].trim().isEmpty())) {
            lineIndex++;
        }

        // --- Parse Grid Layout ---
        List<String[]> gridTokens = new ArrayList<>();
        while (lineIndex < lines.length && !lines[lineIndex].startsWith("#") && !lines[lineIndex].trim().isEmpty()) {
            gridTokens.add(lines[lineIndex++].trim().split(","));
        }

        int gridHeight = gridTokens.size();
        int gridWidth = gridTokens.get(0).length;

        // --- Create board ---
        if (game instanceof ISquareHolder) {
            ISquareHolder sh = (ISquareHolder) game;
            sh.resetBoard(gridWidth, gridHeight);

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
                        sh.addSquare(new BlankSquare(), r, c);
                    } else {
                        sh.addSquare(new PlayableSquare(color, shape), r, c);
                    }
                }
            }
        }

        // --- Skip to goals ---
        while (lineIndex < lines.length && !lines[lineIndex].toLowerCase().contains("goal")) {
            lineIndex++;
        }
        lineIndex++; // skip "Goal Coordinates" comment line

        // --- Parse goals ---
        while (lineIndex < lines.length) {
            String line = lines[lineIndex++].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] goalCoords = line.split(",");
            if (goalCoords.length < 2) continue;

            int goalRow = Integer.parseInt(goalCoords[0].trim());
            int goalCol = Integer.parseInt(goalCoords[1].trim());

            if (game instanceof IGoalHolder) {
                ((IGoalHolder) game).addGoal(goalRow, goalCol);
            }
        }

        // --- Place eyeball ---
        if (game instanceof IEyeballHolder) {
            ((IEyeballHolder) game).addEyeball(startRow, startCol, startDir);
        }

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
    private void syncGameState() {
        rowLiveData.setValue(game.getEyeballRow());
        colLiveData.setValue(game.getEyeballColumn());
        dirLiveData.setValue(game.getEyeballDirection());
        boardLiveData.setValue(game.getBoardData());
        currentGoalLiveData.setValue(game.isCurrentSquareGoal());
    }
}
