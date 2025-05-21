package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoal;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ILevelHolder;
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

	// ADDED: NEW
	public int getMazeIndex(IMaze maze) { return levels.indexOf(maze); }

	/**
	 * Parses stripped level text (no comments) and adds a new Maze.
	 * Format:
	 *   lines[0] = "ID,startRow,startCol,startDir,startSquare"
	 *   lines[1..H] = grid rows of "COLOR_SHAPE" tokens
	 *   lines[last] = "goalRow,goalCol"
	 */
	@Override
	public void loadLevelFromText(String[] lines) {
		levels.clear();

		List<String> currentMaze = new ArrayList<>();
		for (String line : lines) {
			if (line.trim().isEmpty() && !currentMaze.isEmpty()) {
				addMazeFromLines(currentMaze);
				currentMaze.clear();
			} else if (!line.trim().startsWith("#")) {
				currentMaze.add(line.trim());
			}
		}
		if (!currentMaze.isEmpty()) {
			addMazeFromLines(currentMaze);
		}

		if (!levels.isEmpty()) {
			currentLevel = levels.get(0);
		}
	}

	private void addMazeFromLines(List<String> lines) {
		if (lines.size() < 3) {
			Log.e("LevelHolder", "Maze block too small, skipping.");
			return;
		}

		try {
			Log.d("LevelHolder", "Parsing maze with " + lines.size() + " lines");
			Log.d("LevelHolder", "Header: " + lines.get(0));

			String[] hdr = lines.get(0).split(",", 5);
			if (hdr.length < 4) {
				Log.e("LevelHolder", "Invalid header: " + lines.get(0));
				return;
			}

			int mazeId     = Integer.parseInt(hdr[0]);
			int startRow   = Integer.parseInt(hdr[1]);
			int startCol   = Integer.parseInt(hdr[2]);
			Direction dir  = parseDirection(hdr[3]);

			// Detect goal lines at the end (must be two integers)
			int goalStart = lines.size();
			for (int i = lines.size() - 1; i > 0; i--) {
				String[] parts = lines.get(i).split(",");
				if (parts.length == 2 && parts[0].trim().matches("\\d+") && parts[1].trim().matches("\\d+")) {
					goalStart = i;
				} else {
					break;
				}
			}

//			int gridEnd = (goalStart == -1) ? lines.size() : goalStart - 1;
			int H = goalStart - 1;
			if (H <= 0) {
				Log.e("LevelHolder", "Invalid grid dimensions.");
				return;
			}
			String[] firstRow = lines.get(1).split(",");
			int W = firstRow.length;

			Square[][] boardData = new Square[H][W];

			for (int r = 0; r < H; r++) {
				String[] tokens = lines.get(1 + r).split(",");
				for (int c = 0; c < W; c++) {
					String tok = tokens[c].trim();
					if (tok.equals("BLANK_BLANK")) {
						boardData[r][c] = new BlankSquare();
					} else {
						String[] parts = tok.split("_", 2);
						Color color = Color.valueOf(parts[0].trim().toUpperCase());
						Shape shape = Shape.valueOf(parts[1].trim().toUpperCase());
						boardData[r][c] = new PlayableSquare(color, shape);
					}
				}
			}

			List<IGoal> mazeGoals = new ArrayList<>();
			for (int i = goalStart; i < lines.size(); i++) {
				String[] g = lines.get(i).split(",");
				int goalRow = Integer.parseInt(g[0].trim());
				int goalCol = Integer.parseInt(g[1].trim());
				mazeGoals.add(new Goal(goalRow, goalCol));
			}

			Maze maze = new Maze(mazeId, startRow, startCol, dir, mazeGoals.size(), boardData, mazeGoals);
			levels.add(maze);
			Log.d("LevelHolder", "Maze " + mazeId + " added. Total levels: " + levels.size());

		} catch (Exception e) {
			Log.e("LevelHolder", "Failed to parse maze: " + e.getMessage(), e);
		}
	}

	private Direction parseDirection(String code) {
        return switch (code.toLowerCase()) {
            case "u" -> Direction.UP;
            case "d" -> Direction.DOWN;
            case "l" -> Direction.LEFT;
            case "r" -> Direction.RIGHT;
            default -> throw new IllegalArgumentException("Invalid dir: " + code);
        };
	}
}
