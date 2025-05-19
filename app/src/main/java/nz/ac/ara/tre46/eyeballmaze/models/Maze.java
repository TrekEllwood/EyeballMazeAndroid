package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.List;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMaze;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoal;

public class Maze implements IMaze {
    private int mazeNumber;
    private int startRow;
    private int startColumn;
    private Direction startOrientation;
    private int numGoals;
    private Square[][] boardData;
    private List<IGoal> goals;

    public Maze(int mazeNumber, int startRow, int startColumn, Direction startOrientation, int numGoals,
	    Square[][] boardData, List<IGoal> goals) {
	this.mazeNumber = mazeNumber;
	this.startRow = startRow;
	this.startColumn = startColumn;
	this.startOrientation = startOrientation;
	this.numGoals = numGoals;
	this.boardData = boardData;
	this.goals = goals;
    }

    @Override
    public int getMazeNumber() {
	return mazeNumber;
    }

    @Override
    public int getStartRow() {
	return startRow;
    }

    @Override
    public int getStartCol() {
	return startColumn;
    }

    @Override
    public Direction getStartOrientation() {
	return startOrientation;
    }

    @Override
    public int getNumGoals() {
	return numGoals;
    }

    @Override
    public Square[][] getBoardData() {
	return boardData;
    }

    @Override
    public List<IGoal> getMazeGoals() {
	return goals;
    }

    // ADDED
    @Override
    public void setBoardData(Square[][] boardData) {
        this.boardData = boardData;
    }
}
