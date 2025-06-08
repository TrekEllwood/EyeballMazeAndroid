package nz.ac.ara.tre46.eyeballmaze.ui.binding;

import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import java.util.Locale;

import nz.ac.ara.tre46.eyeballmaze.R;
import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.ui.components.ControlPanelBuilder;
import nz.ac.ara.tre46.eyeballmaze.utils.SnackbarUtils;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;

public class ViewModelBinder {
    private final FragmentActivity activity;
    private final EyeballMazeViewModel viewModel;
    private final MazeView mazeView;
    private final ControlPanelBuilder controls;

    private final Runnable onSolved;

    public ViewModelBinder(
            FragmentActivity activity,
            EyeballMazeViewModel viewModel,
            MazeView mazeView,
            ControlPanelBuilder controls,
            Runnable onSolved
    ) {
        this.activity = activity;
        this.viewModel = viewModel;
        this.mazeView = mazeView;
        this.controls = controls;
        this.onSolved = onSolved;
    }

    public void bindAll(LifecycleOwner owner) {
        observeMazePosition(owner);
        observeMazeStatus(owner);
        observeMoveFeedback(owner);
        observeGameState(owner);
        observeButtonStates(owner);
        observeElapsedTime(owner);
    }

    private void observeMazePosition(LifecycleOwner owner) {
        viewModel.getEyeballPositionLiveData().observe(owner, pos -> {
            if (pos != null) {
                mazeView.setEyeballPosition(pos.y, pos.x);
                mazeView.setGoalPositions(viewModel.getGoalPoints());
            }
        });

        viewModel.getEyeballDirLiveData().observe(owner, mazeView::setDirection);
    }

    private void observeMazeStatus(LifecycleOwner owner) {
        viewModel.isCurrentGoalLiveData().observe(owner, isGoal -> {
            mazeView.setCurrentSquareIsGoal(isGoal);
            mazeView.invalidate();
        });
    }

    private void observeMoveFeedback(LifecycleOwner owner) {
        viewModel.getMoveCountLiveData().observe(owner, count -> controls.moveCounterTextView.setText(activity.getString(R.string.moves_format, count)));

        viewModel.getMoveHappenedLiveData().observe(owner, happened -> {
            if (Boolean.TRUE.equals(happened)) {
                viewModel.clearMoveHappened();
            }
        });

        viewModel.getMoveStatusLiveData().observe(owner, message -> {
            if (message != null && message != Message.OK) {
                SnackbarUtils.showMoveBlocked(
                        activity.findViewById(android.R.id.content),
                        message.name()
                );
                viewModel.clearMoveStatusLiveData();
            }
        });
    }

    private void observeGameState(LifecycleOwner owner) {
        viewModel.getGoalsRemainingLiveData().observe(owner, remaining -> {
            updateGoalStatusText(remaining);
            if (remaining <= 0) onSolved.run();
        });
    }

    private void observeButtonStates(LifecycleOwner owner) {
        viewModel.getIsSolvedLiveData().observe(owner, isSolved -> updateUndoButtonState());
        viewModel.canUndoLiveData().observe(owner, canUndo -> updateUndoButtonState());

        viewModel.getCanReplayLiveData().observe(owner, canReplay -> controls.replayBtn.setEnabled(Boolean.TRUE.equals(canReplay)));

        viewModel.getIsPausedLiveData().observe(owner, this::updatePauseUI);

        viewModel.getIsTimerStartedLiveData().observe(owner, started -> updatePauseButtonEnabled());
    }

    private void observeElapsedTime(LifecycleOwner owner) {
        viewModel.getElapsedTimeLiveData().observe(owner, this::updateTimerDisplay);
    }

    private void updatePauseUI(boolean paused) {
        if (paused) {
            controls.pauseBtn.setImageResource(R.drawable.baseline_play_circle_24);
            controls.pauseBtn.setContentDescription(activity.getString(R.string.resume));
            mazeView.setVisibility(View.INVISIBLE);
        } else {
            controls.pauseBtn.setImageResource(R.drawable.baseline_pause_circle_24);
            controls.pauseBtn.setContentDescription(activity.getString(R.string.pause));
            mazeView.setVisibility(View.VISIBLE);
        }
    }

    private void updatePauseButtonEnabled() {
        boolean hasStarted = Boolean.TRUE.equals(viewModel.getIsTimerStartedLiveData().getValue());
        boolean isPaused = Boolean.TRUE.equals(viewModel.getIsPausedLiveData().getValue());
        controls.pauseBtn.setEnabled(hasStarted || isPaused);
    }

    private void updateUndoButtonState() {
        boolean solved = Boolean.TRUE.equals(viewModel.getIsSolvedLiveData().getValue());
        boolean canUndo = Boolean.TRUE.equals(viewModel.canUndoLiveData().getValue());
        controls.undoBtn.setEnabled(canUndo && !solved);
    }

    private void updateTimerDisplay(long elapsedMillis) {
        int seconds = (int) (elapsedMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String formatted = String.format(Locale.US, "%02d:%02d", minutes, seconds);
        controls.timerTextView.setText(activity.getString(R.string.time_format, formatted));
    }

    private void updateGoalStatusText(int remaining) {
        String text = activity.getResources().getQuantityString(
                R.plurals.goals_remaining, remaining, remaining);
        controls.goalsStatusTextView.setText(text);
    }
}
