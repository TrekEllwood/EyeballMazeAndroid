package nz.ac.ara.tre46.eyeballmaze.interfaces;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;
import nz.ac.ara.tre46.eyeballmaze.models.Square;

public interface ISquareHolder {
    void addSquare(Square square, int row, int column);

    void resetBoard(int width, int height);

    public Square getSquareAt(int row, int column);

    public Square[][] getBoardData();

    public Color getColorAt(int row, int column);

    public Shape getShapeAt(int row, int column);

    public SquareType getTypeAt(int row, int column);
}
