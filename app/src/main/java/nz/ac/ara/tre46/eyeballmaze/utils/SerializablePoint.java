package nz.ac.ara.tre46.eyeballmaze.utils;

import java.io.Serializable;

public class SerializablePoint implements Serializable {
    public int x;
    public int y;

    public SerializablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
