package nz.ac.ara.tre46.eyeballmaze.models;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;

public class BlankSquare extends Square {
    public BlankSquare() {
	super(SquareType.BLANK, Color.BLANK, Shape.BLANK);
    }

    @Override
    public boolean isPlayable() {
	return false;
    }

    @Override
    public Square copy() { // ADDED: for undo
        return new BlankSquare();
    }
}
