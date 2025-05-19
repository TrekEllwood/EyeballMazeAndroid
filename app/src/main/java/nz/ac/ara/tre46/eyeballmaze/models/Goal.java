package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.Objects;

import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoal;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IPosition;

public class Goal implements IGoal {
    private final IPosition position;

    public Goal(int row, int column) {
	this.position = new Position(row, column);
    }

    @Override
    public int getRow() {
	return position.getRow();
    }

    @Override
    public int getColumn() {
	return position.getColumn();
    }

    /**
     * Checks if two Goal objects are equal. Two goals are considered equal if they
     * have the same row and column values.
     * 
     * @param o The object to compare with this goal.
     * @return true if both objects are Goals with the same row and column, false
     *         otherwise.
     */
    @Override
    public boolean equals(Object o) {
	if (this == o)
	    return true;

	if (o == null || getClass() != o.getClass())
	    return false;

	Goal goal = (Goal) o;

	return Objects.equals(position, goal.position);
    }

    /**
     * Generates a hash code for the Goal object. Ensures that two equal Goal
     * objects have the same hash code.
     * 
     * @return A hash code based on row and column values.
     */
    @Override
    public int hashCode() {
	return Objects.hash(position);
    }
}
