package nz.ac.ara.tre46.eyeballmaze.utils;

import android.content.Context;
import android.widget.Toast;

import nz.ac.ara.tre46.eyeballmaze.R;

public class ToastUtils {
    public static void showShort(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showLong(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showSolvedMessage(Context context, String timeFormatted, int moveCount, boolean isNewRecord) {
        String message = context.getString(R.string.solved) + "\n" +
                context.getString(R.string.time_format, timeFormatted) + "\n" +
                context.getString(R.string.moves_format, moveCount);

        if (isNewRecord) {
            message += "\n" + context.getString(R.string.new_record);
        }

        showLong(context, message);
    }

    public static void showNoMoreLevels(Context context) {
        showLong(context, context.getString(R.string.no_more_levels));
    }
}
