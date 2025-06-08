package nz.ac.ara.tre46.eyeballmaze.ui.playback;

import android.graphics.Point;
import android.os.Handler;

import java.util.List;

import nz.ac.ara.tre46.eyeballmaze.view.MazeView;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;

public class PlaybackManager {
    private final MazeView mazeView;
    private final Handler handler;
    private final EyeballMazeViewModel viewModel;

    private Runnable playbackRunnable;

    public PlaybackManager(MazeView mazeView, Handler handler, EyeballMazeViewModel viewModel) {
        this.mazeView = mazeView;
        this.handler = handler;
        this.viewModel = viewModel;
    }

    public void playBackMoves(Runnable onStart, Runnable onFinish) {
        List<Point> trail = viewModel.getPlaybackTrail();
        if (trail.isEmpty() || Boolean.TRUE.equals(viewModel.getIsPlayingBackLiveData().getValue())) return;

        viewModel.setIsPlayingBack(true);
        if (onStart != null) onStart.run();

        final int[] index = {0};
        playbackRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < trail.size()) {
                    Point p = trail.get(index[0]);
                    mazeView.setEyeballPosition(p.y, p.x);
                    mazeView.invalidate();
                    index[0]++;
                    handler.postDelayed(this, 400);
                } else {
                    viewModel.setIsPlayingBack(false);
                    playbackRunnable = null;
                    if (onFinish != null) onFinish.run();
                }
            }
        };
        handler.post(playbackRunnable);
    }

    public void cancelPlaybackAndJumpToEnd() {
        if (!Boolean.TRUE.equals(viewModel.getIsPlayingBackLiveData().getValue()) || playbackRunnable == null) return;

        handler.removeCallbacks(playbackRunnable);
        playbackRunnable = null;
        viewModel.setIsPlayingBack(false);

        List<Point> trail = viewModel.getPlaybackTrail();
        if (!trail.isEmpty()) {
            Point last = trail.get(trail.size() - 1);
            mazeView.setEyeballPosition(last.y, last.x);
            mazeView.invalidate();
        }
    }
}
