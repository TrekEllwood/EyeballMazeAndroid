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
import nz.ac.ara.tre46.eyeballmaze.models.BlankSquare;
import nz.ac.ara.tre46.eyeballmaze.models.PlayableSquare;

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

	// NEW
	/**
	 * Parses stripped level text (no comments) and adds a new Maze.
	 * Format:
	 *   lines[0] = "ID,startRow,startCol,startDir,startSquare"
	 *   lines[1..H] = grid rows of "COLOR_SHAPE" tokens
	 *   lines[last] = "goalRow,goalCol"
	 */
	@Override
	public void loadLevelFromText(String[] lines) {
		// 1) Header
		String[] hdr = lines[0].split(",", 5);
		int mazeId     = Integer.parseInt(hdr[0]);
		int startRow   = Integer.parseInt(hdr[1]);
		int startCol   = Integer.parseInt(hdr[2]);
		Direction dir  = parseDirection(hdr[3]);
		// startSquare hdr[4] is redundant with grid data

		// 2) Grid lines
		int totalLines = lines.length;
		int gridLines  = totalLines - 2;
		int H = gridLines;
		String[] firstRow = lines[1].split(",");
		int W = firstRow.length;
		Square[][] boardData = new Square[H][W];

		for (int r = 0; r < H; r++) {
			String[] tokens = lines[1 + r].split(",");
			for (int c = 0; c < W; c++) {
				String tok = tokens[c];
				if (tok.equals("BLANK_BLANK")) {
					boardData[r][c] = new BlankSquare();
				} else {
					String[] parts = tok.split("_", 2);
					Color color = Color.valueOf(parts[0]);
					Shape shape = Shape.valueOf(parts[1]);
					boardData[r][c] = new PlayableSquare(color, shape);
				}
			}
		}

		// ─── DEBUG: dump loaded boardData ─────────────────────────────────────
		for (int r = 0; r < H; r++) {
			for (int c = 0; c < W; c++) {
				Square sq = boardData[r][c];
				Log.d("LevelHolder", String.format(
						"loaded boardData[%d][%d] = %s_%s",
						r, c,
						sq.getColor(),
						sq.getShape()
				));
			}
		}
		// ─────────────────────────────────────────────────────────────────────

		// 3) Goal
		String[] g = lines[1 + H].split(",", 2);
		int goalRow = Integer.parseInt(g[0]);
		int goalCol = Integer.parseInt(g[1]);
		List<IGoal> mazeGoals = new ArrayList<>();
		mazeGoals.add(new Goal(goalRow, goalCol));

		// 4) Build new Maze and set as current
		Maze maze = new Maze(mazeId, startRow, startCol, dir,
				mazeGoals.size(), boardData, mazeGoals);
		levels.add(maze);
		currentLevel = maze;
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
