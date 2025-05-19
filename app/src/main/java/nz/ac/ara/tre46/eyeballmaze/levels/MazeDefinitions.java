//package nz.ac.ara.tre46.eyeballmaze.levels;
//
//import nz.ac.ara.tre46.eyeballmaze.models.Square;
//
//public class MazeDefinitions {
//
//    public static Square[][] loadMaze1() {
//        int rows = 10;
//        int cols = 11;
//        String[] layout = {
//                "70", "70", "70", "70", "70", "70", "74", "70", "70", "70", "70",
//                "70", "70", "70", "70", "74", "76", "4b", "7c", "70", "70", "70",
//                "70", "70", "70", "72", "2a", "4c", "1c", "2d", "78", "70", "70",
//                "70", "70", "70", "72", "4d", "3b", "3d", "1c", "78", "70", "70",
//                "70", "70", "70", "72", "4b", "4a", "3b", "4d", "78", "70", "70",
//                "70", "70", "70", "72", "3a", "1b", "4a", "1a", "78", "70", "70",
//                "70", "70", "70", "70", "73", "1a", "79", "71", "70", "70", "70",
//                "70", "70", "70", "70", "70", "71", "70", "70", "70", "70", "70",
//                "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70",
//                "70", "70", "70", "70", "70", "70", "70", "70", "70", "70", "70"
//        };
//
//        Square[][] board = new Square[rows][cols];
//        int index = 0;
//        for (int r = 0; r < rows; r++) {
//            for (int c = 0; c < cols; c++) {
//                String code = layout[index++];
//                boolean isGoal = code.equals("4b"); // Define '4b' as goal cells
//                board[r][c] = new Square(isGoal);
//            }
//        }
//
//        return board;
//    }
//
//    public static int startRow() {
//        return 6;  // Example from JS data
//    }
//
//    public static int startCol() {
//        return 5;  // Example from JS data
//    }
//
//    public static String startDirection() {
//        return "u";  // Example from JS data
//    }
//}
