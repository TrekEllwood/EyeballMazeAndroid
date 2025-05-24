package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.exceptions.LevelLoadException;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoal;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ILevelHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMaze;
import nz.ac.ara.tre46.eyeballmaze.dto.JsonLevel;
import nz.ac.ara.tre46.eyeballmaze.dto.GoalData;

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

	// ADDED: NEW
	public int getMazeIndex(IMaze maze) { return levels.indexOf(maze); }

	public IMaze getMazeAt(int index) {
		if (index >= 0 && index < levels.size()) {
			return levels.get(index);
		}
		return null;
	}

	/**
	 * Loads one or more levels from a JSON string and adds them to the level list.
	 */
	public void loadLevelFromJson(String json) {
		levels.clear();
		Gson gson = new Gson();

		try {
			Type levelListType = new TypeToken<List<JsonLevel>>() {}.getType();
			List<JsonLevel> jsonLevels = gson.fromJson(json, levelListType);

			for (JsonLevel jsonLevel : jsonLevels) {
				addMazeFromJsonLevel(jsonLevel);
			}

			if (!levels.isEmpty()) {
				currentLevel = levels.get(0);
			}
		} catch (Exception e) {
			throw new LevelLoadException("Failed to load JSON levels", e); // ADDED: custom exceptions
		}
	}

	private void addMazeFromJsonLevel(JsonLevel jsonLevel) {
		try {
			if (jsonLevel.grid == null || jsonLevel.grid.isEmpty()) {
				throw new LevelLoadException("Grid is null or empty in level: " + jsonLevel.id, null);
			}

			int H = jsonLevel.grid.size();
			int W = jsonLevel.grid.get(0).size();

			for (int r = 0; r < H; r++) {
				if (jsonLevel.grid.get(r).size() != W) {
					throw new LevelLoadException("Inconsistent row size at row " + r + " in level: " + jsonLevel.id, null);
				}
			}

			Square[][] boardData = new Square[H][W];

			for (int r = 0; r < H; r++) {
				for (int c = 0; c < W; c++) {
					String token = jsonLevel.grid.get(r).get(c);
					if (token.equals("BLANK_BLANK")) {
						boardData[r][c] = new BlankSquare();
					} else {
						if (!token.contains("_")) {
							throw new LevelLoadException("Invalid token format '" + token + "' at (" + r + "," + c + ")", null);
						}
						String[] parts = token.split("_", 2);
						try {
							Color color = Color.valueOf(parts[0].trim().toUpperCase());
							Shape shape = Shape.valueOf(parts[1].trim().toUpperCase());
							boardData[r][c] = new PlayableSquare(color, shape);
						} catch (IllegalArgumentException e) {
							throw new LevelLoadException("Invalid color or shape in token '" + token + "' at (" + r + "," + c + ")", e);
						}
					}
				}
			}

			Direction dir;
			try {
				dir = Direction.valueOf(jsonLevel.startDir.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new LevelLoadException("Invalid direction: " + jsonLevel.startDir + " in level: " + jsonLevel.id, e);
			}

			List<IGoal> mazeGoals = new ArrayList<>();
			for (GoalData g : jsonLevel.goals) {
				mazeGoals.add(new Goal(g.row, g.col));
			}

			Maze maze = new Maze(
					jsonLevel.id,
					jsonLevel.startRow,
					jsonLevel.startCol,
					dir,
					mazeGoals.size(),
					boardData,
					mazeGoals
			);
			levels.add(maze);
		} catch (LevelLoadException e) {
			throw e; // rethrow if already custom
		} catch (Exception e) {
			throw new LevelLoadException("Unexpected error while parsing level: " + jsonLevel.id, e);
		}
	}
}
