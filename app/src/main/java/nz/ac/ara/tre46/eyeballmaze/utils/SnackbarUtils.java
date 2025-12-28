package nz.ac.ara.tre46.eyeballmaze.utils;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.snackbar.Snackbar;

import nz.ac.ara.tre46.eyeballmaze.R;

public class SnackbarUtils {
    public static void showCenteredShort(View anchorView, String message) {
        Snackbar snackbar = Snackbar.make(anchorView, message, Snackbar.LENGTH_SHORT);
        centerSnackbar(snackbar);
        snackbar.show();
    }

    public static void showCenteredLong(View anchorView, String message) {
        Snackbar snackbar = Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG);
        centerSnackbar(snackbar);
        snackbar.show();
    }

    public static void showMoveBlocked(View anchorView, String reason) {
        String message = anchorView.getContext().getString(R.string.move_blocked, reason);
        showCenteredShort(anchorView, message);
    }

    public static void showLevelComplete(View anchorView, String message, String actionLabel, View.OnClickListener onNextClick) {
        Snackbar snackbar = Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(actionLabel, onNextClick);
        snackbar.show();
    }

    private static void centerSnackbar(Snackbar snackbar) {
        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.CENTER;
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        snackbarView.setLayoutParams(params);
    }
}
