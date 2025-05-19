package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.Objects;

import nz.ac.ara.tre46.eyeballmaze.interfaces.IPosition;

public class Position implements IPosition {
    private final int row;
    private final int column;

    public Position(int row, int column) {
	this.row = row;
	this.column = column;
    }

    public int getRow() {
	return row;
    }

    public int getColumn() {
	return column;
    }

    @Override
    public boolean equals(Object o) {
	if (this == o)
	    return true;
	if (o == null || getClass() != o.getClass())
	    return false;
	Position position = (Position) o;
	return row == position.row && column == position.column;
    }

    // Position objects used as keys in a HashMap
    @Override
    public int hashCode() {
	return Objects.hash(row, column);
    }
}
