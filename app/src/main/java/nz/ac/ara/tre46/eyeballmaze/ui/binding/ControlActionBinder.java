package nz.ac.ara.tre46.eyeballmaze.ui.binding;

import android.view.View;
import android.widget.AdapterView;

import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.atomic.AtomicBoolean;

import nz.ac.ara.tre46.eyeballmaze.ui.components.ControlPanelBuilder;
import nz.ac.ara.tre46.eyeballmaze.ui.dialogs.TutorialVideoDialogFragment;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;

public class ControlActionBinder {
    public interface Callback {
        void onResetLevel();
        void onSyncMazeView();
        void onPlayBackMoves();
        void onPauseGame();
        void onResumeGame();
        void onToggleMute(boolean isMuted);
        void onCancelPlayback();
    }

    private final FragmentActivity activity;
    private final ControlPanelBuilder controls;
    private final EyeballMazeViewModel viewModel;
    private final MazeView mazeView;
    private final Callback callback;
    private boolean isMuted;

    public ControlActionBinder(FragmentActivity activity, ControlPanelBuilder controls, EyeballMazeViewModel viewModel,
                               MazeView mazeView, Callback callback, boolean isMutedInitially) {
        this.activity = activity;
        this.controls = controls;
        this.viewModel = viewModel;
        this.mazeView = mazeView;
        this.callback = callback;
        this.isMuted = isMutedInitially;
    }

    public void bind() {
        bindResetButton();
        bindUndoButton();
        bindReplayButton();
        bindPauseButton();
        bindMuteButton();
        bindHowToButton();
        bindLevelSpinner();
    }

    private void bindResetButton() {
        controls.resetBtn.setOnClickListener(v -> {
            viewModel.resetMaze();
            mazeView.setGoalPositions(viewModel.getGoalPoints());
            callback.onResetLevel();
            viewModel.updateCanReplay(viewModel.getPlaybackTrail());
        });
    }

    private void bindUndoButton() {
        controls.undoBtn.setOnClickListener(v -> {
            viewModel.undo();
            callback.onSyncMazeView();
            viewModel.removeLastFromTrail();
        });
    }

    private void bindReplayButton() {
        controls.replayBtn.setOnClickListener(v -> callback.onPlayBackMoves());
    }

    private void bindPauseButton() {
        controls.pauseBtn.setOnClickListener(v -> {
            boolean isPaused = Boolean.TRUE.equals(viewModel.getIsPausedLiveData().getValue());
            if (isPaused) {
                viewModel.resumeTimer();
                callback.onResumeGame();
            } else {
                viewModel.pauseTimer();
                callback.onPauseGame();
                callback.onCancelPlayback();
            }
        });
    }

    private void bindMuteButton() {
        controls.muteBtn.setOnClickListener(v -> {
            isMuted = !isMuted;
            callback.onToggleMute(isMuted);
        });
    }

    private void bindHowToButton() {
        controls.howToBtn.setOnClickListener(v -> {
            callback.onCancelPlayback();
            new TutorialVideoDialogFragment().show(activity.getSupportFragmentManager(), "RulesVideo");
        });
    }

    private void bindLevelSpinner() {
        AtomicBoolean isFirstSelection = new AtomicBoolean(true);
        controls.levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstSelection.getAndSet(false)) return;

                if (position != viewModel.getCurrentLevelIndex()) {
                    callback.onCancelPlayback();
                    controls.levelSpinner.setEnabled(false);
                    viewModel.setLevel(position);
                    callback.onResetLevel();
                    controls.pauseBtn.setEnabled(false);
                    controls.levelSpinner.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}
