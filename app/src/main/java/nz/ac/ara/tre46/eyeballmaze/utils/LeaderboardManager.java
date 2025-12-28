package nz.ac.ara.tre46.eyeballmaze.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class LeaderboardManager {
    private final SharedPreferences prefs;
    private static final String PREFS_NAME = "EyeballMazePrefs";
    private static final String KEY_BEST_TIME_PREFIX = "best_time_maze_";
    private static final String KEY_BEST_MOVES_PREFIX = "best_moves_maze_";

    public LeaderboardManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the best time for a specific maze ID
     * @param mazeId The maze ID (1-13)
     * @return Best time in milliseconds, or -1 if not completed
     */
    public long getBestTime(int mazeId) {
        return prefs.getLong(KEY_BEST_TIME_PREFIX + mazeId, -1L);
    }

    /**
     * Get the best move count for a specific maze ID
     * @param mazeId The maze ID (1-13)
     * @return Best move count, or -1 if not completed
     */
    public int getBestMoves(int mazeId) {
        return prefs.getInt(KEY_BEST_MOVES_PREFIX + mazeId, -1);
    }

    /**
     * Check if the current completion is a new record and update if so
     * @param mazeId The maze ID
     * @param timeMillis Completion time in milliseconds
     * @param moves Number of moves taken
     * @return true if this is a new record (either time or moves improved)
     */
    public boolean checkAndUpdateRecord(int mazeId, long timeMillis, int moves) {
        long currentBestTime = getBestTime(mazeId);
        int currentBestMoves = getBestMoves(mazeId);

        // First completion is always a new record
        if (currentBestTime == -1 || currentBestMoves == -1) {
            saveRecord(mazeId, timeMillis, moves);
            return true;
        }

        // Check if either time or moves improved
        boolean betterTime = timeMillis < currentBestTime;
        boolean betterMoves = moves < currentBestMoves;

        if (betterTime || betterMoves) {
            // Save the better values (keep best of each independently)
            long newBestTime = betterTime ? timeMillis : currentBestTime;
            int newBestMoves = betterMoves ? moves : currentBestMoves;
            saveRecord(mazeId, newBestTime, newBestMoves);
            return true;
        }

        return false;
    }

    /**
     * Save a new record for a maze
     * @param mazeId The maze ID
     * @param timeMillis Time in milliseconds
     * @param moves Move count
     */
    private void saveRecord(int mazeId, long timeMillis, int moves) {
        prefs.edit()
                .putLong(KEY_BEST_TIME_PREFIX + mazeId, timeMillis)
                .putInt(KEY_BEST_MOVES_PREFIX + mazeId, moves)
                .apply();
    }

    /**
     * Format the best time for a maze as "MM:SS" or "--:--" if not completed
     * @param mazeId The maze ID
     * @return Formatted time string
     */
    public String formatBestTime(int mazeId) {
        long timeMillis = getBestTime(mazeId);
        if (timeMillis == -1) {
            return "--:--";
        }

        long seconds = timeMillis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds);
    }

    /**
     * Format the best move count for a maze or "--" if not completed
     * @param mazeId The maze ID
     * @return Formatted moves string
     */
    public String formatBestMoves(int mazeId) {
        int moves = getBestMoves(mazeId);
        if (moves == -1) {
            return "--";
        }
        return String.valueOf(moves);
    }
}
