package nz.ac.ara.tre46.eyeballmaze;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import android.graphics.Point;

import androidx.core.graphics.Insets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nz.ac.ara.tre46.eyeballmaze.audio.SoundManager;
import nz.ac.ara.tre46.eyeballmaze.ui.binding.ViewModelBinder;
import nz.ac.ara.tre46.eyeballmaze.ui.components.ControlPanelBuilder;
import nz.ac.ara.tre46.eyeballmaze.ui.components.MazeViewInitializer;
import nz.ac.ara.tre46.eyeballmaze.ui.binding.ControlActionBinder;
import nz.ac.ara.tre46.eyeballmaze.utils.SnackbarUtils;
import nz.ac.ara.tre46.eyeballmaze.utils.ToastUtils;
import nz.ac.ara.tre46.eyeballmaze.utils.SerializablePoint;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;
    private ControlPanelBuilder controlPanel;
    private ControlActionBinder controlActionBinder;
    private ViewModelBinder viewModelBinder;
    private SoundManager soundManager;
    private Runnable playbackRunnable;
    private final Handler playbackHandler = new Handler();
    private boolean
            gameButtonsEnabled = true,
            isMuted = false,
            isPlayingBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupInitialState(savedInstanceState);
        setupLayoutAndViews(savedInstanceState);
        setupObservers();

        if (savedInstanceState != null) {
            restoreAppState(savedInstanceState);
        }
    }

    private void setupViewModel(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(
                this,
                new EyeballMazeViewModelFactory(getApplicationContext())
        ).get(EyeballMazeViewModel.class);

        if (savedInstanceState == null) {
            viewModel.initializeLevelFromPreferences();
        }
    }

    private void setupInitialState(Bundle savedInstanceState) {
        setupViewModel(savedInstanceState);
        setupSFX();

        if (isLandscapeMode() && getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void setupLayoutAndViews(Bundle savedInstanceState) {
        final boolean isLandscape = isLandscapeMode();

        LinearLayout root = createRootLayout(isLandscape);
        mazeView = createMazeView();

        controlPanel = new ControlPanelBuilder(this);
        setupLevelSpinnerAdapter();

        View controlLayout = createControlPanelLayout(isLandscape);
        addViewsToRoot(root, controlLayout, isLandscape);

        setContentView(root);
        bindControlActions();

        syncMazeViewFromViewModel();
        resizeMazeView(root, controlLayout, isLandscape);

        if (savedInstanceState == null) {
            recordStartPosition();
        }
    }

    private LinearLayout createRootLayout(boolean isLandscape) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sysInsets.left, sysInsets.top, sysInsets.right, sysInsets.bottom);
            return insets;
        });

        return root;
    }

    private MazeView createMazeView() {
        return MazeViewInitializer.create(this, viewModel, new MazeViewInitializer.MoveCallback() {
            @Override
            public void onValidMove(int row, int col) {
                soundManager.playMove();
                viewModel.addMoveToTrail(new Point(col, row));
                if (!viewModel.isTimerStarted()) {
                    viewModel.startTimer();
                    setGameButtonsEnabled(true);
                }
            }

            @Override
            public void onInvalidMove(int row, int col) {
                soundManager.playBad();
                mazeView.setFailedMoveAt(row, col);
            }
        });
    }

    private View createControlPanelLayout(boolean isLandscape) {
        LinearLayout spinnerRow = controlPanel.buildSpinnerRow();
        View controlLayout = isLandscape
                ? controlPanel.buildLandscapeLayout(spinnerRow)
                : controlPanel.buildPortraitLayout(spinnerRow);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(isLandscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        wrapper.setGravity(Gravity.CENTER_VERTICAL);
        wrapper.addView(controlLayout, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        return wrapper;
    }

    private void addViewsToRoot(LinearLayout root, View controls, boolean isLandscape) {
        float mazeWeight = 0.5f;
        float controlsWeight = 0.5f;

        LinearLayout.LayoutParams mazeParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : 0,
                isLandscape ? mazeWeight : 1.0f
        );

        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT,
                isLandscape ? controlsWeight : 0f
        );

        root.addView(mazeView, mazeParams);
        root.addView(controls, controlsParams);
    }

    private void bindControlActions() {
        controlActionBinder = new ControlActionBinder(
                this,
                controlPanel,
                viewModel,
                mazeView,
                new ControlActionBinder.Callback() {
                    @Override public void onResetLevel() { resetLevel(); }
                    @Override public void onSyncMazeView() { syncMazeViewFromViewModel(); }
                    @Override public void onPlayBackMoves() { playBackMoves(); }
                    @Override public void onPauseGame() { setButtonsExceptPauseEnabled(false); }
                    @Override public void onResumeGame() { setButtonsExceptPauseEnabled(true); }
                    @Override public void onToggleMute(boolean muted) {
                        isMuted = muted;
                        applyMuteState();
                    }
                    @Override public void onCancelPlayback() { cancelPlaybackAndJumpToEnd(); }
                },
                isMuted
        );
        controlActionBinder.bind();
    }

    private void setupLevelSpinnerAdapter() {
        List<String> levelLabels = new ArrayList<>();
        for (int i = 0; i < viewModel.getLevelCount(); i++) {
            int mazeId = viewModel.getMazeIdAt(i);  // <-- this was missing
            levelLabels.add("Maze " + mazeId);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levelLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        controlPanel.levelSpinner.setAdapter(adapter);
        controlPanel.levelSpinner.setSelection(viewModel.getCurrentLevelIndex());
    }

    private boolean isLandscapeMode() {
        return getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
    }

    private void setupSFX() {
        soundManager = new SoundManager(this);
    }

    private void resizeMazeView(LinearLayout root, View controls, boolean isLandscape) {
        // Force square mazeView
        mazeView.post(() -> {
            ViewGroup.LayoutParams params = mazeView.getLayoutParams();
            if (isLandscape) {
                int squareSize = mazeView.getHeight();
                params.width = squareSize;
                params.height = squareSize;
            } else {
                int screenHeight = root.getHeight();
                int controlsHeight = controls.getHeight();
                int availableHeight = screenHeight - controlsHeight;
                int screenWidth = root.getWidth();
                int size = Math.min(screenWidth, availableHeight);
                params.width = size;
                params.height = size;
            }

            mazeView.setLayoutParams(params);
            mazeView.invalidate();
        });
    }

    private void setPauseIcon() {
        controlPanel.pauseBtn.setImageResource(R.drawable.baseline_pause_circle_24);
        controlPanel.pauseBtn.setContentDescription(getString(R.string.pause));
    }

    private void setPausedStateUI() {
        controlPanel.pauseBtn.setImageResource(R.drawable.baseline_play_circle_24);
        controlPanel.pauseBtn.setContentDescription(getString(R.string.resume));
        mazeView.setVisibility(View.INVISIBLE);
    }

    private void resetPausedStateUI() {
        setPauseIcon();
        mazeView.setVisibility(View.VISIBLE);
    }

    private void applyPauseState() {
        viewModel.pauseTimer();
        setButtonsExceptPauseEnabled(false);
    }

    @SuppressWarnings("unchecked")
    private void restorePersistentAppState(Bundle savedInstanceState) {
        Serializable trailSerializable = savedInstanceState.getSerializable("move_trail");

        if (trailSerializable instanceof ArrayList<?> trailList) {
            boolean allAreSerializablePoints = trailList.stream()
                    .allMatch(item -> item instanceof SerializablePoint);

            if (allAreSerializablePoints) {
                ArrayList<SerializablePoint> trailData = (ArrayList<SerializablePoint>) trailList;

                List<Point> points = new ArrayList<>();
                for (SerializablePoint sp : trailData) {
                    points.add(new Point(sp.x, sp.y));
                }

                viewModel.restorePlaybackTrail(points);
            }
        }

        isMuted = savedInstanceState.getBoolean("is_muted", false);
    }

    private void restoreAppState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            boolean restoredSolved = savedInstanceState.getBoolean("is_solved", false);
            if (restoredSolved) {
                viewModel.markSolved();
            } else {
                viewModel.clearSolved();
            }

            restorePersistentAppState(savedInstanceState);
            restoreTimerState(savedInstanceState);
            applyMuteState();
        }
    }

    private void observeViewModel() {
        viewModelBinder = new ViewModelBinder(
                this,
                viewModel,
                mazeView,
                controlPanel,
                this::handleLevelSolved // callback when goalsRemaining == 0
        );
        viewModelBinder.bindAll(this);
    }

    private void handleLevelSolved() {
        viewModel.markSolved();
        viewModel.stopTimer();
        mazeView.setTouchEnabled(false);

        long solvedTime  = viewModel.getSolveTimeMillis();
        updateTimerDisplay(solvedTime );
        controlPanel.goalsStatusTextView.setText(getString(R.string.solved));

        playSolvedFeedback(solvedTime );
        showLevelCompleteMsg();
        freezeGameAfterSolve();
    }

    private void playSolvedFeedback(long elapsed) {
        soundManager.playWin();

        String solveTimeFormatted = String.format(Locale.US, "%02d:%02d",
                (elapsed / 1000) / 60,
                (elapsed / 1000) % 60
        );

        ToastUtils.showSolvedMessage(this, solveTimeFormatted);
    }

    private void showLevelCompleteMsg() {
        SnackbarUtils.showLevelComplete(findViewById(android.R.id.content), getString(R.string.next), v -> {
            int next = viewModel.getCurrentLevelIndex() + 1;
            if (next < viewModel.getLevelCount()) {
                cancelPlaybackAndJumpToEnd();
                viewModel.setLevel(next);
                controlPanel.levelSpinner.setSelection(viewModel.getCurrentLevelIndex());
                resetLevel();
            } else {
                ToastUtils.showNoMoreLevels(this);
            }
        });
    }

    private void freezeGameAfterSolve() {
        setGameButtonsEnabled(false);
    }

    private void setupObservers() {
        mazeView.setColorProvider(viewModel::getColorAt);
        mazeView.setShapeProvider(viewModel::getShapeAt);
        mazeView.setGoalPositions(viewModel.getGoalPoints());
        observeViewModel();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("pause_enabled", controlPanel.pauseBtn.isEnabled());
        outState.putInt("selected_level", viewModel.getCurrentLevelIndex());

        // Convert Point list to SerializablePoint list
        ArrayList<SerializablePoint> serializableTrail = new ArrayList<>();
        for (Point p : viewModel.getPlaybackTrail()) {
            serializableTrail.add(new SerializablePoint(p.x, p.y));
        }
        outState.putSerializable("move_trail", serializableTrail);
        outState.putBoolean("is_solved", viewModel.isSolved());
        outState.putBoolean("is_muted", isMuted);
        viewModel.saveTimerStateToBundle(outState);
    }

    private void syncMazeViewFromViewModel() {
        mazeView.setBoardSize(viewModel.getBoardHeight(), viewModel.getBoardWidth());
        mazeView.setGoalPositions(viewModel.getGoalPoints());
        mazeView.setColorProvider(viewModel::getColorAt);
        mazeView.setShapeProvider(viewModel::getShapeAt);
        updateAppTitle();
        mazeView.invalidate();
    }

    private void updateAppTitle() {
        int mazeId = viewModel.getCurrentMazeId();
        String title = getString(R.string.app_name) + " " + mazeId;

        if (isLandscapeMode()) {
            controlPanel.titleTextView.setText(title); // landscape
        } else {
            setTitle(title); // portrait
        }
    }

    private void playBackMoves() {
        List<Point> trail = viewModel.getPlaybackTrail();
        if (trail.isEmpty() || isPlayingBack) return;

        isPlayingBack = true;
        controlPanel.resetBtn.setEnabled(false);
        controlPanel.undoBtn.setEnabled(false);

        final int[] index = {0};

        playbackRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < trail.size()) {
                    Point p = trail.get(index[0]);
                    mazeView.setEyeballPosition(p.y, p.x);
                    mazeView.invalidate();
                    index[0]++;
                    playbackHandler.postDelayed(this, 400);
                } else {
                    isPlayingBack = false;
                    playbackRunnable = null;
                    updateGameButtonsState();
                }
            }
        };

        playbackHandler.post(playbackRunnable);
    }

    private void cancelPlaybackAndJumpToEnd() {
        if (isPlayingBack && playbackRunnable != null) {
            playbackHandler.removeCallbacks(playbackRunnable);
            isPlayingBack = false;
            playbackRunnable = null;

            // Jump to last move
            Point last = null;
            for (Point p : viewModel.getPlaybackTrail()) {
                last = p;
            }

            if (last != null) {
                mazeView.setEyeballPosition(last.y, last.x);
                mazeView.invalidate();
            }

            updateGameButtonsState();
        }
    }

    private void recordStartPosition() {
        controlPanel.timerTextView.setText(getString(R.string.time_format, "00:00"));

        Integer row = viewModel.getEyeballRowLiveData().getValue();
        Integer col = viewModel.getEyeballColLiveData().getValue();

        if (row != null && col != null) {
            viewModel.clearPlaybackTrail();
            viewModel.addMoveToTrail(new Point(col, row));
        }
    }

    private void updateTimerDisplay(long elapsedMillis) {
        if (controlPanel.timerTextView == null) return;

        int seconds = (int) (elapsedMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String time = String.format(Locale.US, "%02d:%02d", minutes, seconds);
        controlPanel.timerTextView.setText(getString(R.string.time_format, time));
    }

    private void resetLevel() {
        viewModel.clearSolved();
        mazeView.setTouchEnabled(true);
        viewModel.stopTimer();
        viewModel.resetTimer();
        viewModel.clearPlaybackTrail();
        syncMazeViewFromViewModel();
        recordStartPosition();
        resetPausedStateUI();
    }

    private void setGameButtonsEnabled(boolean enabled) {
        if (viewModel.isSolved()) enabled = false;
        gameButtonsEnabled = enabled;
        setButtonsExceptPauseEnabled(enabled);
        if (controlPanel.pauseBtn != null) controlPanel.pauseBtn.setEnabled(enabled);
    }

    private void setButtonsExceptPauseEnabled(boolean enabled) {
        boolean solved = Boolean.TRUE.equals(viewModel.getIsSolvedLiveData().getValue());

        if (controlPanel.resetBtn != null) {
            controlPanel.resetBtn.setEnabled(enabled || solved);
        }
        if (controlPanel.undoBtn != null) {
            controlPanel.undoBtn.setEnabled(enabled && !solved);
        }
        if (controlPanel.replayBtn != null) {
            controlPanel.replayBtn.setEnabled(enabled || solved);
        }
    }

    private void updateGameButtonsState() {
        boolean solved = Boolean.TRUE.equals(viewModel.getIsSolvedLiveData().getValue());

        controlPanel.resetBtn.setEnabled(gameButtonsEnabled || solved);
        controlPanel.undoBtn.setEnabled(gameButtonsEnabled && !solved);
        controlPanel.pauseBtn.setEnabled(gameButtonsEnabled);
    }

    private void restoreTimerState(Bundle savedInstanceState) {
        viewModel.restoreTimerStateFromBundle(savedInstanceState);
        applyTimerStateFromViewModel();
    }

    private void applyTimerStateFromViewModel() {
        if (viewModel.isSolved()) {
            long solvedTime = viewModel.getSolveTimeMillis();
            updateTimerDisplay(solvedTime);
            setGameButtonsEnabled(false);
            return;
        }

        if (viewModel.isTimerStarted()) {
            if (viewModel.isPaused()) {
                setPausedStateUI();
            } else {
                resetPausedStateUI();
            }
        } else {
            long solvedTime = viewModel.getSolveTimeMillis();
            updateTimerDisplay(solvedTime);
            resetPausedStateUI();
        }
    }

    private void updateMuteIcon(boolean muted) {
        if (controlPanel.muteBtn == null) return;

        int icon = muted ? R.drawable.baseline_volume_off_24 : R.drawable.baseline_volume_up_24;
        String description = muted ? getString(R.string.mute) : getString(R.string.unmute);

        controlPanel.muteBtn.setImageResource(icon);
        controlPanel.muteBtn.setContentDescription(description);
    }

    private void applyMuteState() {
        soundManager.setMuted(isMuted);
        updateMuteIcon(isMuted);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            applyPauseState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundManager != null) {
            soundManager.release();
        }
    }
}
