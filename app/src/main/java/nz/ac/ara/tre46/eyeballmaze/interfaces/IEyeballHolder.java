package nz.ac.ara.tre46.eyeballmaze.interfaces;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;

public interface IEyeballHolder {
    public void addEyeball(int row, int column, Direction direction);

    public int getEyeballRow();

    public int getEyeballColumn();

    public Direction getEyeballDirection();
}
