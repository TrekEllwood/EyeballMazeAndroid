package nz.ac.ara.tre46.eyeballmaze.models;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IEyeballHolder;

public class EyeballHolder implements IEyeballHolder {
    private int row;
    private int column;
    private Direction direction;

    public EyeballHolder() {
	this.row = -1; // Default to invalid position
	this.column = -1;
	this.direction = Direction.UP;
    }

    @Override
    public void addEyeball(int row, int column, Direction direction) {
	this.row = row;
	this.column = column;
	this.direction = direction;

    }

    @Override
    public int getEyeballRow() {
	return row;
    }

    @Override
    public int getEyeballColumn() {
	return column;
    }

    @Override
    public Direction getEyeballDirection() {
	return direction;
    }
}
