package nz.ac.ara.tre46.eyeballmaze.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ISquareHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoalHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IEyeballHolder;
import nz.ac.ara.tre46.eyeballmaze.models.BlankSquare;
import nz.ac.ara.tre46.eyeballmaze.models.PlayableSquare;
import nz.ac.ara.tre46.eyeballmaze.models.Square;
import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;

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

    // === Movement ===
    public void moveEyeball(Direction direction) {
//        game.setEyeballDirection(direction);
        int newRow = game.getEyeballRow();
        int newCol = game.getEyeballColumn();

        switch (direction) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
        }

//        game.setEyeballRow(newRow);
//        game.setEyeballColumn(newCol);
//        syncGameState();
        Square[][] board = game.getBoardData();
        int rows = board.length;
        int cols = board[0].length;

        // Prevent moving off the board
        if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
            game.setEyeballRow(newRow);
            game.setEyeballColumn(newCol);
            syncGameState();
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
    public void loadLevelFromText(String[] rows,
                                  int startRow,
                                  int startCol,
                                  String startDir) {
        int height = rows.length;
        int width = rows[0].length();

        // Reset and fill board
        if (game instanceof ISquareHolder) {
            ISquareHolder sh = (ISquareHolder) game;
            sh.resetBoard(width, height);
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    char ch = rows[r].charAt(c);
                    if (ch == '.') {
                        sh.addSquare(new BlankSquare(), r, c);
                    } else {
                        // place a default playable square
                        sh.addSquare(new PlayableSquare(Color.GREEN, Shape.DIAMOND), r, c);
                    }
                }
            }
        }

        // Add goals
        if (game instanceof IGoalHolder) {
            IGoalHolder gh = (IGoalHolder) game;
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    if (rows[r].charAt(c) == 'G') {
                        gh.addGoal(r, c);
                    }
                }
            }
        }

        // Position eyeball
        if (game instanceof IEyeballHolder) {
            IEyeballHolder eh = (IEyeballHolder) game;
            eh.addEyeball(startRow, startCol, parseDir(startDir));
        }

        syncGameState();
    }

    private Direction parseDir(String d) {
        switch (d) {
            case "u": return Direction.UP;
            case "d": return Direction.DOWN;
            case "l": return Direction.LEFT;
            case "r": default: return Direction.RIGHT;
        }
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
