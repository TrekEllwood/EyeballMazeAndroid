package nz.ac.ara.tre46.eyeballmaze.models;

import android.content.Context;
import android.graphics.Point;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    // ADDED #1: to make Game self contained
    public Game(Context context) {
        this(); // call default constructor to set up dependencies

        // ADDED for levels from file
        String json = loadJsonFromAssets(context, "levels.json");
        loadLevelFromJson(json);
    }

    // ADDED #2: it now owns responsibility for loading and initialising levels
    private String loadJsonFromAssets(Context context, String filename) {
        try (InputStream is = context.getAssets().open(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n"); // preserve formatting
            }

            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException("Could not read JSON level file", e);
        }
    }

    @Override
    public void addLevel(int height, int width) {
        levelHolder.addLevel(height, width);
        // Set the most recently added level as the current level.
        setLevel(levelHolder.getLevelCount() - 1);
    }

    // CHANGE: So maze does not get overridden by blanks
    @Override
    public void setLevel(int levelNumber) {
        levelHolder.setLevel(levelNumber);
        currentMaze = ((LevelHolder) levelHolder).getCurrentMaze();
        if (currentMaze == null) return;

        int H = getLevelHeight();
        int W = getLevelWidth();

        // Initialize SquareHolder
        squareHolder = new SquareHolder(currentMaze);
        squareHolder.resetBoard(W, H);

        // Copy non-blank squares
        Square[][] board = currentMaze.getBoardData();
        for (int r = 0; r < H; r++) {
            for (int c = 0; c < W; c++) {
                Square sq = board[r][c];
                if (!(sq instanceof BlankSquare)) {
                    squareHolder.addSquare(sq, r, c);
                }
            }
        }

        // Setup player
        player = new Player(
                currentMaze.getStartRow(),
                currentMaze.getStartCol(),
                currentMaze.getStartOrientation()
        );

        // Goals
        Set<IGoal> goals = new HashSet<>(currentMaze.getMazeGoals());
        goalHolder.setGoals(goals);
    }

    @Override
    public void loadLevelFromJson(String json) { // ADDED to import levels
        levelHolder.loadLevelFromJson(json);

        if (levelHolder.getLevelCount() == 0) {
            throw new IllegalStateException("No levels were loaded. Check JSON format.");
        }

        setLevel(0); // First time initialise level
    }

    public void loadLevelsFromAssets(Context context) { // ADDED: for factory to trigger level loading
        try (InputStream is = context.getAssets().open("levels.json");
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            String json = baos.toString(StandardCharsets.UTF_8.name());
            loadLevelFromJson(json);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load levels from assets", e);
        }
    }

    public int getCurrentMazeId() { // ADDED
        return (currentMaze != null) ? currentMaze.getMazeNumber() : -1;
    }

    @Override
    public int getLevelWidth() {
        return levelHolder.getLevelWidth();
    }

    @Override
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

    public void setEyeballPosition(int row, int col) { // ADDED: for undo
        if (player != null) {
            player.setRow(row);
            player.setColumn(col);
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

    @Override // ADDED
    public boolean canMoveTo(int destinationRow, int destinationColumn) {
        return moving.canMoveTo(destinationRow, destinationColumn);
    }

    @Override // ADDED
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

    @Override // ADDED
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

    // ADDED
    @Override
    public void resetCurrentLevel() {
        if (levelHolder instanceof LevelHolder holder) {
            IMaze current = holder.getCurrentMaze();
            int index = holder.getMazeIndex(current);
//            holder.resetCurrentLevelFromText();
            setLevel(index);
        }
    }

    @Override
    public Set<Point> getRemainingGoalPoints() {
        Set<Point> set = new HashSet<>();
        for (IGoal goal : goalHolder.getGoals()) {
            set.add(new Point(goal.getColumn(), goal.getRow()));
        }
        return set;
    }

    public void setRemainingGoalPoints(Set<Point> points) { // ADDED: for undo
        Set<IGoal> goals = new HashSet<>();
        for (Point p : points) {
            goals.add(new Goal(p.y, p.x)); // Point = (x, y), Goal = (row, col)
        }
        goalHolder.setGoals(goals);
    }

    public int getBoardHeight() {
        return levelHolder.getLevelHeight();
    }

    public int getBoardWidth() {
        return levelHolder.getLevelWidth();
    }

    @Override
    public int getMazeIdAt(int index) { // ADDED
        if (levelHolder instanceof LevelHolder holder) {
            IMaze maze = holder.getMazeAt(index);
            return (maze != null) ? maze.getMazeNumber() : index + 1;
        }
        return index + 1;
    }

    @Override
    public Object saveState() { // ADDED: for undo
        Square[][] original = getBoardData();
        Square[][] boardCopy = new Square[original.length][];

        for (int r = 0; r < original.length; r++) {
            boardCopy[r] = new Square[original[r].length];
            for (int c = 0; c < original[r].length; c++) {
                Square sq = original[r][c];
                if (sq != null) {
                    boardCopy[r][c] = sq.copy();
                }
            }
        }

        return new GameState(
                getEyeballRow(),
                getEyeballColumn(),
                getEyeballDirection(),
                getRemainingGoalPoints(),
                boardCopy
        );
    }

    @Override
    public void loadState(Object state) { // ADDED: for undo
        if (!(state instanceof GameState gs)) {
            throw new IllegalArgumentException("Invalid state object");
        }
        setEyeballPosition(gs.row(), gs.col());
        setEyeballDirection(gs.direction());
        setRemainingGoalPoints(gs.goalPoints());

        // Restore board
        Square[][] board = gs.board();
        squareHolder.resetBoard(board[0].length, board.length); // width, height

        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                Square sq = board[r][c];
                if (sq != null) {
                    squareHolder.addSquare(sq.copy(), r, c); // copy to avoid shared reference
                }
            }
        }
    }
}
