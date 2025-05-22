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

//    public static GameState of(
//            int row,
//            int col,
//            Direction direction,
//            Set<Point> originalGoals,
//            Square[][] originalBoard
//    ) {
//        Set<Point> copiedGoals = Set.copyOf(originalGoals);
//
//        // Deep copy of the board
//        Square[][] copiedBoard = new Square[originalBoard.length][];
//        for (int i = 0; i < originalBoard.length; i++) {
//            copiedBoard[i] = new Square[originalBoard[i].length];
//            for (int j = 0; j < originalBoard[i].length; j++) {
//                Square square = originalBoard[i][j];
//                copiedBoard[i][j] = square != null ? square.copy() : null;
//            }
//        }
//
//        return new GameState(row, col, direction, copiedGoals, copiedBoard);
//    }
}
