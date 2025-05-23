package nz.ac.ara.tre46.eyeballmaze.models;

import android.graphics.Point;

import java.util.Set;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;

record GameState(
        int row,
        int col,
        Direction direction,
        Set<Point> goalPoints,
        Square[][] board
) {
}
