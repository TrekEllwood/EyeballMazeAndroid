package nz.ac.ara.tre46.eyeballmaze.interfaces;

import android.graphics.Point;

import java.util.Set;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;
import nz.ac.ara.tre46.eyeballmaze.models.Square;

public interface IGame {
    public int getEyeballRow();

    public int getEyeballColumn();

    public Direction getEyeballDirection();

    public Square[][] getBoardData();

    public boolean hasGoalAt(int row, int col);

    public void removeGoalAt(int row, int col);

    public void setEyeballRow(int row);

    public void setEyeballColumn(int col);

    public void setEyeballDirection(Direction d);

    public boolean isCurrentSquareGoal();

    public void setCurrentSquareGoal(boolean flag);

    public void completedGoal(int row, int column);

    // NEW: all below this line ---
    public int getCurrentMazeId();

    boolean canMoveTo(int destinationRow, int destinationColumn);

    Message messageIfMovingTo(int destinationRow, int destinationColumn);

    void moveTo(int destinationRow, int destinationColumn);

    void addGoal(int row, int col);

    void addEyeball(int row, int col, Direction direction);

    void addSquare(Square square, int row, int col);

    void resetBoard(int width, int height);

    void resetCurrentLevel();

    Set<Point> getRemainingGoalPoints();

    public void setLevel(int levelNumber);

    public int getLevelCount();

    void loadLevelFromText(String[] lines);

    public Color getColorAt(int row, int column);

    public Shape getShapeAt(int row, int column);

    public SquareType getTypeAt(int row, int column);

    public int getBoardHeight();

    public int getBoardWidth();

    public int getMazeIdAt(int index);
}
