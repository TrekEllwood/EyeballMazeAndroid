package nz.ac.ara.tre46.eyeballmaze.interfaces;

import java.util.List;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.models.Square;

public interface IMaze {
    public int getMazeNumber();

    public int getStartRow();

    public int getStartCol();

    public Direction getStartOrientation();

    public int getNumGoals();

    public Square[][] getBoardData();

    public List<IGoal> getMazeGoals();

    void setBoardData(Square[][] boardData);
}
