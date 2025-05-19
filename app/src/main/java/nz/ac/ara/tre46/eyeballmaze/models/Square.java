package nz.ac.ara.tre46.eyeballmaze.models;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;

public abstract class Square {
    protected Color color;
    protected Shape shape;
    protected final SquareType type;

    public Square(SquareType type, Color color, Shape shape) {
	this.type = type;
	this.color = color;
	this.shape = shape;
    }

    public Color getColor() {
	return color;
    }

    public Shape getShape() {
	return shape;
    }

    public SquareType getType() {
	return type;
    }

    public void setColor(Color color) {
	this.color = color;
    }

    public void setShape(Shape shape) {
	this.shape = shape;
    }

    public abstract boolean isPlayable();
}
