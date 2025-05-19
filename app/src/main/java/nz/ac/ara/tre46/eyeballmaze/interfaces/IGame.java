package nz.ac.ara.tre46.eyeballmaze.interfaces;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
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
}
