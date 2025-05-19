package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IEyeballHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoal;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoalHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ILevelHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ISquareHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMaze;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMoving;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IPlayer;

public class Game implements IGame, IGoalHolder, IEyeballHolder, ILevelHolder, ISquareHolder {
    private final ILevelHolder levelHolder;
    private final IGoalHolder goalHolder;
    private final IMoving moving;
    private ISquareHolder squareHolder;

    private IMaze currentMaze;

    // Eyeball (player) state
    private IPlayer player;

    private boolean currentSquareWasGoal = false;

    public Game() {
	levelHolder = new LevelHolder();
	goalHolder = new GoalHolder();
	moving = new Moving(this);
//	squareHolder = new SquareHolder(); // CHANGE
    }

    @Override
    public void addLevel(int height, int width) {
	levelHolder.addLevel(height, width);
	// Set the most recently added level as the current level.
	setLevel(levelHolder.getLevelCount() - 1);
    }

    @Override
    public void setLevel(int levelNumber) {
	levelHolder.setLevel(levelNumber);
	if (levelHolder instanceof LevelHolder) {
	    currentMaze = ((LevelHolder) levelHolder).getCurrentMaze();
	}
	if (currentMaze != null) {
	    int width = getLevelWidth();
	    int height = getLevelHeight();

        squareHolder = new SquareHolder(currentMaze); // ADDED
	    squareHolder.resetBoard(width, height);

	    player = new Player(currentMaze.getStartRow(), currentMaze.getStartCol(),
		    currentMaze.getStartOrientation());

	    Set<IGoal> mazeGoals = new HashSet<>(currentMaze.getMazeGoals());
	    goalHolder.setGoals(mazeGoals);
	}
    }

    public int getLevelWidth() {
	return levelHolder.getLevelWidth();
    }

    public int getLevelHeight() {
	return levelHolder.getLevelHeight();
    }

    @Override
    public int getLevelCount() {
	return levelHolder.getLevelCount();
    }

    @Override
    public int getEyeballRow() {
	return (player != null) ? player.getRow() : -1;
    }

    @Override
    public int getEyeballColumn() {
	return (player != null) ? player.getColumn() : -1;
    }

    @Override
    public Direction getEyeballDirection() {
	return (player != null) ? player.getDirection() : null;
    }

    @Override
    public void addEyeball(int row, int column, Direction direction) {
	// TODO: create helper/util
	if (row < 0 || row >= getLevelHeight() || column < 0 || column >= getLevelWidth()) {
	    throw new IllegalArgumentException("Eyeball position out of bounds: (" + row + ", " + column + ")");
	}
	player = new Player(row, column, direction);
    }

    @Override
    public void setEyeballRow(int row) {
	if (player != null) {
	    player.setRow(row);
	}
    }

    @Override
    public void setEyeballColumn(int column) {
	if (player != null) {
	    player.setColumn(column);
	}
    }

    @Override
    public void setEyeballDirection(Direction direction) {
	if (player != null) {
	    player.setDirection(direction);
	}
    }

    @Override
    public void addGoal(int row, int column) {
	// TODO: create helper/util
	if (row < 0 || row >= getLevelHeight() || column < 0 || column >= getLevelWidth()) {
	    throw new IllegalArgumentException("Goal position out of bounds: (" + row + "," + column + ")");
	}
	goalHolder.addGoal(row, column);
    }

    @Override
    public int getGoalCount() {
	return goalHolder.getGoalCount();
    }

    @Override
    public boolean hasGoalAt(int targetRow, int targetColumn) {
	return goalHolder.hasGoalAt(targetRow, targetColumn);
    }

    @Override
    public int getCompletedGoalCount() {
	return goalHolder.getCompletedGoalCount();
    }

    @Override
    public boolean areAllGoalsCompleted() {
	return goalHolder.areAllGoalsCompleted();
    }

    @Override
    public void completedGoal(int row, int column) {
	goalHolder.completedGoal(row, column);
    }

    @Override
    public void removeGoalAt(int row, int column) {
	goalHolder.removeGoalAt(row, column);
    }

    @Override
    public Set<IGoal> getGoals() {
	return goalHolder.getGoals();
    }

    @Override
    public void setGoals(Collection<IGoal> newGoals) {
	goalHolder.setGoals(newGoals);
    }

    @Override
    public void addSquare(Square square, int row, int column) {
	// TODO: create helper/util
	if (row < 0 || row >= getLevelHeight() || column < 0 || column >= getLevelWidth()) {
	    throw new IllegalArgumentException("Square position out of bounds: (" + row + "," + column + ")");
	}
	squareHolder.addSquare(square, row, column);
    }

    @Override
    public void resetBoard(int width, int height) {
	squareHolder.resetBoard(width, height);
    }

    @Override
    public Square[][] getBoardData() {
	return squareHolder.getBoardData();
    }

    @Override
    public SquareType getTypeAt(int row, int column) {
	return squareHolder.getTypeAt(row, column);
    }

    @Override
    public Color getColorAt(int row, int column) {
	return squareHolder.getColorAt(row, column);
    }

    @Override
    public Shape getShapeAt(int row, int column) {
	return squareHolder.getShapeAt(row, column);
    }

    public boolean canMoveTo(int destinationRow, int destinationColumn) {
	return moving.canMoveTo(destinationRow, destinationColumn);
    }

    public Message messageIfMovingTo(int destinationRow, int destinationColumn) {
	return moving.messageIfMovingTo(destinationRow, destinationColumn);
    }

    public boolean isDirectionOK(int destinationRow, int destinationColumn) {
	return moving.isDirectionOK(destinationRow, destinationColumn);
    }

    public Message checkDirectionMessage(int destinationRow, int destinationColumn) {
	return moving.checkDirectionMessage(destinationRow, destinationColumn);
    }

    public boolean hasBlankFreePathTo(int destinationRow, int destinationColumn) {
	return moving.hasBlankFreePathTo(destinationRow, destinationColumn);
    }

    public Message checkMessageForBlankOnPathTo(int destinationRow, int destinationColumn) {
	return moving.checkMessageForBlankOnPathTo(destinationRow, destinationColumn);
    }

    public void moveTo(int destinationRow, int destinationColumn) {
	moving.moveTo(destinationRow, destinationColumn);
    }

    @Override
    public Square getSquareAt(int row, int column) {
	return squareHolder.getSquareAt(row, column);
    }

    // Goal completion tracking
    @Override
    public boolean isCurrentSquareGoal() {
	return currentSquareWasGoal;
    }

    @Override
    public void setCurrentSquareGoal(boolean flag) {
	currentSquareWasGoal = flag;
    }
}
