package nz.ac.ara.tre46.eyeballmaze.interfaces;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;

public interface IPlayer {
    int getRow();

    int getColumn();

    Direction getDirection();

    void setRow(int row);

    void setColumn(int column);

    void setDirection(Direction direction);
}
