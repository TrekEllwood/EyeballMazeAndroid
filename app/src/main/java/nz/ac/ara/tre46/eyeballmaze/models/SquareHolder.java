package nz.ac.ara.tre46.eyeballmaze.models;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ISquareHolder;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMaze;

public class SquareHolder implements ISquareHolder {
    private Square[][] board = null;
	private final IMaze maze;

	public SquareHolder(IMaze maze) {
		this.maze = maze;
	}

    @Override
    public void addSquare(Square square, int row, int column) {
		if (row >= 0 && row < board.length && column >= 0 && column < board[0].length) {
			board[row][column] = square;
		}
    }

    @Override
    public void resetBoard(int width, int height) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Board dimensions must be greater than 0.");
		}
		this.board = new Square[height][width];
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
			board[r][c] = new BlankSquare();
			}
		}
    }

	@Override
    public Square getSquareAt(int row, int column) {
	if (row >= 0 && row < board.length && column >= 0 && column < board[0].length) {
	    return board[row][column];
	}
	return null;
    }

    @Override
    public Square[][] getBoardData() {
	return board;
    }

    @Override
    public Color getColorAt(int row, int column) {
	Square square = getSquareAt(row, column);
	return (square != null) ? square.getColor() : null;
    }

    @Override
    public Shape getShapeAt(int row, int column) {
	Square square = getSquareAt(row, column);
	return (square != null) ? square.getShape() : null;
    }

    @Override
    public SquareType getTypeAt(int row, int column) {
	Square square = getSquareAt(row, column);
	return (square != null) ? square.getType() : null;
    }
}
