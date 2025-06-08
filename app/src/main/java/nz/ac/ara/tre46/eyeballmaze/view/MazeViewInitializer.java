package nz.ac.ara.tre46.eyeballmaze.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import nz.ac.ara.tre46.eyeballmaze.R;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;

public class MazeViewInitializer {    public interface MoveCallback {
    void onValidMove(int row, int col);
    void onInvalidMove(int row, int col);
}

    public static MazeView create(Context context, EyeballMazeViewModel viewModel, MoveCallback callback) {
        MazeView mazeView = new MazeView(context);
        Bitmap eyeballBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.eyeball);
        mazeView.setEyeballBitmap(eyeballBmp);

        mazeView.setOnCellTapListener((row, col) -> {
            if (viewModel.canMoveTo(row, col)) {
                callback.onValidMove(row, col);
            } else {
                callback.onInvalidMove(row, col);
            }
            viewModel.clickToMoveToward(row, col);
        });

        return mazeView;
    }
}
