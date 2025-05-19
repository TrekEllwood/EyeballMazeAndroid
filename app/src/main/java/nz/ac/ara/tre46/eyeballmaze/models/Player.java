package nz.ac.ara.tre46.eyeballmaze.models;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IPlayer;

public class Player implements IPlayer {
    private int row;
    private int column;
    private Direction direction;

    public Player(int row, int column, Direction direction) {
	this.row = row;
	this.column = column;
	this.direction = direction;
    }

    @Override
    public int getRow() {
	return row;
    }

    @Override
    public int getColumn() {
	return column;
    }

    @Override
    public Direction getDirection() {
	return direction;
    }

    @Override
    public void setRow(int row) {
	this.row = row;
    }

    @Override
    public void setColumn(int column) {
	this.column = column;
    }

    @Override
    public void setDirection(Direction direction) {
	this.direction = direction;
    }
}
