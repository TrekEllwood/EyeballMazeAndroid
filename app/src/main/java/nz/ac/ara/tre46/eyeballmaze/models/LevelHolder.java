package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.ArrayList;
import java.util.List;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ILevelHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoal;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMaze;

public class LevelHolder implements ILevelHolder {
    private final List<IMaze> levels;
    private IMaze currentLevel;

    public LevelHolder() {
	levels = new ArrayList<>();
	currentLevel = null;
    }

    @Override
    public void addLevel(int height, int width) {
	Square[][] boardData = new Square[height][width];
	for (int r = 0; r < height; r++) {
	    for (int c = 0; c < width; c++) {
		boardData[r][c] = new BlankSquare();
	    }
	}
	List<IGoal> mazeGoals = new ArrayList<>();
	// Create a new Maze level with default starting position and orientation.
	int mazeNumber = levels.size() + 1;
	IMaze maze = new Maze(mazeNumber, 0, 0, Direction.UP, 0, boardData, mazeGoals);
	levels.add(maze);
	// If this is the first level, set it as the current level.
	if (currentLevel == null) {
	    currentLevel = maze;
	}
    }

    @Override
    public int getLevelWidth() {
	if (currentLevel != null && currentLevel.getBoardData().length > 0) {
	    return currentLevel.getBoardData()[0].length;
	}
	return 0;
    }

    @Override
    public int getLevelHeight() {
	if (currentLevel != null) {
	    return currentLevel.getBoardData().length;
	}
	return 0;
    }

    @Override
    public void setLevel(int levelNumber) {
	if (levelNumber < 0 || levelNumber >= levels.size()) {
	    throw new IllegalArgumentException("Invalid level number: " + levelNumber);
	}
	currentLevel = levels.get(levelNumber);
    }

    @Override
    public int getLevelCount() {
	return levels.size();
    }

    public IMaze getCurrentMaze() {
	return currentLevel;
    }
}
