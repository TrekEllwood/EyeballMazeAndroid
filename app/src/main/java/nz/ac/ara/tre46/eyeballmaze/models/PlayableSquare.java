package nz.ac.ara.tre46.eyeballmaze.models;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;

public class PlayableSquare extends Square {
    public PlayableSquare(Color color, Shape shape) {
	super(SquareType.PLAYABLE, color, shape);
    }

    @Override
    public boolean isPlayable() {
	return true;
    }
}
