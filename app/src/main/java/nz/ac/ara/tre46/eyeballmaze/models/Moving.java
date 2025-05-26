package nz.ac.ara.tre46.eyeballmaze.models;

import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IMoving;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;

public class Moving implements IMoving {
    private final IGame game;

    public Moving(IGame game) {
        this.game = game;
    }

    @Override
    public boolean canMoveTo(int destinationRow, int destinationColumn) {
        return messageIfMovingTo(destinationRow, destinationColumn) == Message.OK;
    }

    @Override
    public Message messageIfMovingTo(int destinationRow, int destinationColumn) {
        int currentRow = game.getEyeballRow();
        int currentColumn = game.getEyeballColumn();
        Square[][] board = game.getBoardData();

        // Cannot move into the same square.
        if (destinationRow == currentRow && destinationColumn == currentColumn) {
            return Message.DIFFERENT_SHAPE_OR_COLOR;
        }
        // No diagonal moves allowed.
        if (destinationRow != currentRow && destinationColumn != currentColumn) {
            return Message.MOVING_DIAGONALLY;
        }
        // Cannot move to non-playable square
        if (!board[destinationRow][destinationColumn].isPlayable()) {
            return Message.MOVING_OVER_BLANK;
        }

        Square currentSquare = board[currentRow][currentColumn];
        Square destinationSquare = board[destinationRow][destinationColumn];
        // Must match shape or color.
        if (currentSquare.getShape() != destinationSquare.getShape() && currentSquare.getColor() != destinationSquare.getColor()) {
            return Message.DIFFERENT_SHAPE_OR_COLOR;
        }
        return isDirectionOK(destinationRow, destinationColumn)
                ? hasBlankFreePathTo(destinationRow, destinationColumn) ? Message.OK : Message.MOVING_OVER_BLANK
                : Message.BACKWARDS_MOVE;
    }

    @Override
    public boolean isDirectionOK(int destinationRow, int destinationColumn) {
        int currentRow = game.getEyeballRow();
        int currentColumn = game.getEyeballColumn();
        Direction currentOrientation = game.getEyeballDirection();

        // Diagonal moves are disallowed.
        if (destinationRow != currentRow && destinationColumn != currentColumn) {
            return false;
        }
        // For vertical moves:
        if (destinationColumn == currentColumn && destinationRow != currentRow) {
            if (destinationRow > currentRow && currentOrientation == Direction.UP) {
                return false;
            }
            if (destinationRow < currentRow && currentOrientation == Direction.DOWN) {
                return false;
            }
        }
        // For horizontal moves:
        if (destinationRow == currentRow && destinationColumn != currentColumn) {
            if (destinationColumn > currentColumn && currentOrientation == Direction.LEFT) {
                return false;
            }
            if (destinationColumn < currentColumn && currentOrientation == Direction.RIGHT) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Message checkDirectionMessage(int destinationRow, int destinationColumn) {
        int currentRow = game.getEyeballRow();
        int currentColumn = game.getEyeballColumn();
        Direction currentOrientation = game.getEyeballDirection();

        if (destinationRow != currentRow && destinationColumn != currentColumn) {
            return Message.MOVING_DIAGONALLY;
        }
        switch (currentOrientation) {
            case Direction.UP -> {
                if (destinationRow > currentRow) {
                    return Message.BACKWARDS_MOVE;
                }
            }
            case Direction.DOWN -> {
                if (destinationRow < currentRow) {
                    return Message.BACKWARDS_MOVE;
                }
            }
            case Direction.LEFT -> {
                if (destinationColumn > currentColumn) {
                    return Message.BACKWARDS_MOVE;
                }
            }
            case Direction.RIGHT -> {
                if (destinationColumn < currentColumn) {
                    return Message.BACKWARDS_MOVE;
                }
            }
        }
        return Message.OK;
    }

    @Override
    public boolean hasBlankFreePathTo(int destinationRow, int destinationColumn) {
        int currentRow = game.getEyeballRow();
        int currentColumn = game.getEyeballColumn();
        Square[][] board = game.getBoardData();

        int rowStep = Integer.compare(destinationRow, currentRow);
        int colStep = Integer.compare(destinationColumn, currentColumn);
        int r = currentRow + rowStep;
        int c = currentColumn + colStep;
        while (r != destinationRow || c != destinationColumn) {
            if (!board[r][c].isPlayable()) {
                return false;
            }
            r += rowStep;
            c += colStep;
        }
        return true;
    }

    @Override
    public Message checkMessageForBlankOnPathTo(int destinationRow, int destinationColumn) {
        return hasBlankFreePathTo(destinationRow, destinationColumn) ? Message.OK : Message.MOVING_OVER_BLANK;
    }

    // CHANGE: for better handling of square changes...
    @Override
    public void moveTo(int destinationRow, int destinationColumn) {
        Message msg = messageIfMovingTo(destinationRow, destinationColumn);
        if (msg != Message.OK) {
            System.out.println("Move error: " + msg);
            return;
        }
        int currentRow = game.getEyeballRow();
        int currentColumn = game.getEyeballColumn();
//		Square[][] board = game.getBoardData();

        // If leaving a square that was marked as a goal, update that square to BlankSquare.
        //	if (game.isCurrentSquareGoal() && (destinationRow != currentRow || destinationColumn != currentColumn)) {
        //	    board[currentRow][currentColumn] = new BlankSquare();
        //	    game.setCurrentSquareGoal(false);
        //	}

//		if (game.isCurrentSquareGoal()
//				&& game.hasGoalAt(currentRow, currentColumn)
//				&& (destinationRow != currentRow || destinationColumn != currentColumn)) {
//
//			game.removeGoalAt(currentRow, currentColumn);
//			game.completedGoal(currentRow, currentColumn);
//			game.setCurrentSquareGoal(false);
//			game.addSquare(new BlankSquare(), currentRow, currentColumn);
//		}

        if (game.hasGoalAt(currentRow, currentColumn)) {
            game.completedGoal(currentRow, currentColumn);
            game.removeGoalAt(currentRow, currentColumn);
            game.setCurrentSquareGoal(false);
            game.addSquare(new BlankSquare(), currentRow, currentColumn);
        }

        // Update orientation based on move direction.
        if (destinationRow != currentRow) {
            game.setEyeballDirection(destinationRow > currentRow ? Direction.DOWN : Direction.UP);
        } else if (destinationColumn != currentColumn) {
            game.setEyeballDirection(destinationColumn > currentColumn ? Direction.RIGHT : Direction.LEFT);
        }
        // Update the current position.
        game.setEyeballRow(destinationRow);
        game.setEyeballColumn(destinationColumn);

        // If the destination square is a goal, complete it immediately.
        game.setCurrentSquareGoal(game.hasGoalAt(destinationRow, destinationColumn));
    }
}
