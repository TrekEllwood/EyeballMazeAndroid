package nz.ac.ara.tre46.eyeballmaze;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import android.graphics.Point;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.graphics.Insets;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.ViewModelProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.ui.TutorialVideoDialogFragment;
import nz.ac.ara.tre46.eyeballmaze.utils.SnackbarUtils;
import nz.ac.ara.tre46.eyeballmaze.utils.ToastUtils;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;
import nz.ac.ara.tre46.eyeballmaze.utils.SerializablePoint;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;
    private TextView titleTextView, spinnerLabel, goalsStatusTextView, moveCounterTextView, timerTextView;
    private SoundPool soundPool;
    private Button resetBtn, undoBtn, replayBtn;
    private ImageButton pauseBtn, muteBtn, howToBtn;
    private Spinner levelSpinner;
    private Runnable playbackRunnable;
    private final Handler playbackHandler = new Handler();
    private boolean
            gameButtonsEnabled = true,
            isMuted = false,
            isSolved = false,
            isPlayingBack = false,
            soundsLoaded = false;
    private int moveSoundId, winSoundId, badSoundId;
    private static final float TEXT_SIZE = 16f;

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

        LinearLayout root = setupLayout(isLandscape);
        mazeView = createMazeView();

        setupControlButtonTints();
        setupStatusTextViews();

        LinearLayout spinnerRow = setupLevelSpinner();
        LinearLayout controls = createControlsContainer(isLandscape, spinnerRow);

        addViewsToRoot(root, controls, isLandscape);
        setContentView(root);

        syncMazeViewFromViewModel();
        resizeMazeView(root, controls, isLandscape);

        if (savedInstanceState == null) {
            recordStartPosition();
        }
    }

    private LinearLayout createControlsContainer(boolean isLandscape, LinearLayout spinnerRow) {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(isLandscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        View controlsLayout = createControlsLayout(isLandscape, spinnerRow);
        controls.addView(controlsLayout, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        return controls;
    }

    private void addViewsToRoot(LinearLayout root, LinearLayout controls, boolean isLandscape) {
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

    private boolean isLandscapeMode() {
        return getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
    }

    private void setupSFX() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        moveSoundId = soundPool.load(this, R.raw.move, 1);
        winSoundId = soundPool.load(this, R.raw.win, 1);
        badSoundId = soundPool.load(this, R.raw.bad, 1);

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) soundsLoaded = true;
        });
    }

    private void setupControlButtonTints() {
        ColorStateList tintList = ContextCompat.getColorStateList(this, R.color.icon_tint);
        assert tintList != null;
        final int tint = tintList.getDefaultColor();

        createControlButtons(tint);
        applyMuteState();

        ImageViewCompat.setImageTintList(pauseBtn, tintList);
        ImageViewCompat.setImageTintList(muteBtn, tintList);
    }

    private void setupStatusTextViews() {
        goalsStatusTextView = createCenteredTextView();
        goalsStatusTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        moveCounterTextView = createCenteredTextView();
        timerTextView = createCenteredTextView();
    }

    private TextView createCenteredTextView() {
        TextView textView = new TextView(this);
        textView.setTextSize(TEXT_SIZE);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        return textView;
    }

    private LinearLayout setupLayout(boolean isLandscape) {
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
        mazeView = new MazeView(this);
        Bitmap eyeballBmp = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
        mazeView.setEyeballBitmap(eyeballBmp);

        mazeView.setOnCellTapListener((row, col) -> {
            if (isSolved) return;

            if (viewModel.canMoveTo(row, col)) {
                handleValidMove(row, col);
            } else {
                handleInvalidMove(row, col);
            }

            viewModel.clickToMoveToward(row, col); // Handles messages
        });

        return mazeView;
    }

    private void handleValidMove(int row, int col) {
        viewModel.addMoveToTrail(new Point(col, row));
        if (!viewModel.isTimerStarted()) {
            viewModel.startTimer();
            setGameButtonsEnabled(true);
        }
    }

    private void handleInvalidMove(int row, int col) {
        playSoundEffect(badSoundId);
        mazeView.setFailedMoveAt(row, col);
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

    private void createControlButtons(int tint) {
        createResetButton();
        createUndoButton();
        createReplayButton();
        createPauseButton(tint);
        createMuteButton(tint);
        createHowToButton(tint);
    }

    private void createResetButton() {
        resetBtn = new Button(this);
        resetBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        resetBtn.setText(getString(R.string.reset));
        resetBtn.setOnClickListener(v -> {
            viewModel.resetMaze();
            mazeView.setGoalPositions(viewModel.getGoalPoints());
            resetLevel();
            viewModel.updateCanReplay(viewModel.getPlaybackTrail());
        });
    }

    private void createUndoButton() {
        undoBtn = new Button(this);
        undoBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        undoBtn.setText(getString(R.string.undo));
        undoBtn.setOnClickListener(v -> {
            viewModel.undo();
            syncMazeViewFromViewModel();

            viewModel.removeLastFromTrail();
        });
    }

    private void createReplayButton() {
        replayBtn = new Button(this);
        replayBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        replayBtn.setText(getString(R.string.replay));
        replayBtn.setOnClickListener(v -> playBackMoves());
    }

    private void createMuteButton(int tint) {
        muteBtn = new ImageButton(this);
        muteBtn.setImageResource(R.drawable.baseline_volume_up_24);
        muteBtn.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        muteBtn.setContentDescription(getString(R.string.mute));
        muteBtn.setBackground(null);

        muteBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        muteBtn.setOnClickListener(v -> {
            isMuted = !isMuted;
            applyMuteState();
        });
    }

    private void createHowToButton(int tint) {
        howToBtn = new ImageButton(this);
        howToBtn.setImageResource(R.drawable.baseline_help_24);
        howToBtn.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        howToBtn.setContentDescription(getString(R.string.help));
        howToBtn.setBackground(null);

        howToBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        howToBtn.setOnClickListener(v -> {
            cancelPlaybackAndJumpToEnd();
            TutorialVideoDialogFragment dialog = new TutorialVideoDialogFragment();
            dialog.show(getSupportFragmentManager(), "RulesVideo");
        });
    }

    private void createPauseButton(int tint) {
        pauseBtn = new ImageButton(this);
        initialPauseBtnImage();
        pauseBtn.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        pauseBtn.setEnabled(false);
        pauseBtn.setBackground(null);
        pauseBtn.setOnClickListener(v -> {
            boolean isPaused = Boolean.TRUE.equals(viewModel.getIsPausedLiveData().getValue());

            if (isPaused) {
                viewModel.resumeTimer();
                setButtonsExceptPauseEnabled(true);
            } else {
                viewModel.pauseTimer();
                setButtonsExceptPauseEnabled(false);
                cancelPlaybackAndJumpToEnd();
            }
        });
    }

    private void initialPauseBtnImage() {
        pauseBtn.setImageResource(R.drawable.baseline_pause_circle_24);
        pauseBtn.setContentDescription(getString(R.string.pause));
    }

    private void setPausedStateUI() {
        pauseBtn.setImageResource(R.drawable.baseline_play_circle_24);
        pauseBtn.setContentDescription(getString(R.string.resume));
        mazeView.setVisibility(View.INVISIBLE);
    }

    private void resetPausedStateUI() {
        initialPauseBtnImage();
        mazeView.setVisibility(View.VISIBLE);
    }

    private void updatePauseButtonUI(boolean isPaused) {
        if (pauseBtn == null) return;

        if (isPaused) {
            pauseBtn.setImageResource(R.drawable.baseline_play_circle_24);
            pauseBtn.setContentDescription(getString(R.string.resume));
            mazeView.setVisibility(View.INVISIBLE);
        } else {
            pauseBtn.setImageResource(R.drawable.baseline_pause_circle_24);
            pauseBtn.setContentDescription(getString(R.string.pause));
            mazeView.setVisibility(View.VISIBLE);
        }
    }

    private void updatePauseButtonEnabledState() {
        boolean hasStarted = Boolean.TRUE.equals(viewModel.getIsTimerStartedLiveData().getValue());
        boolean isPaused = Boolean.TRUE.equals(viewModel.getIsPausedLiveData().getValue());
        if (pauseBtn != null) {
            pauseBtn.setEnabled(gameButtonsEnabled && (hasStarted || isPaused) && !isSolved);
        }
    }

    private LinearLayout setupLevelSpinner() {
        // Container row for label + spinner
        LinearLayout spinnerRow = new LinearLayout(this);
        spinnerRow.setOrientation(LinearLayout.HORIZONTAL);
        spinnerRow.setGravity(Gravity.CENTER_HORIZONTAL);

        spinnerLabel = new TextView(this);
        spinnerLabel.setText(getString(R.string.choose_maze));
        spinnerLabel.setTextSize(TEXT_SIZE);
        spinnerLabel.setPadding(0, 0, 16, 0);  // space between label and spinner

        levelSpinner = new Spinner(this);
        levelSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        ));

        setupLevelSpinnerAdapter();
        setupLevelSpinnerListener();

        spinnerRow.addView(spinnerLabel);
        spinnerRow.addView(levelSpinner);

        return spinnerRow;
    }

    private void setupLevelSpinnerAdapter() {
        List<String> levelLabels = new ArrayList<>();
        for (int i = 0; i < viewModel.getLevelCount(); i++) {
            levelLabels.add("Maze " + viewModel.getMazeIdAt(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levelLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);
        levelSpinner.setSelection(viewModel.getCurrentLevelIndex());
    }

    private void setupLevelSpinnerListener() {
        AtomicBoolean isFirstSpinnerSelection = new AtomicBoolean(true);

        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstSpinnerSelection.getAndSet(false)) return;

                if (position != viewModel.getCurrentLevelIndex()) {
                    cancelPlaybackAndJumpToEnd();
                    levelSpinner.setEnabled(false);
                    viewModel.setLevel(position);
                    resetLevel();
                    pauseBtn.setEnabled(false);
                    levelSpinner.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private View createControlsLayout(boolean isLandscape, LinearLayout spinnerRow) {
        return isLandscape
                ? createLandscapeControlsLayout(spinnerRow)
                : createPortraitControlsLayout(spinnerRow);
    }

    private View createLandscapeControlsLayout(LinearLayout spinnerRow) {
        titleTextView = new TextView(this);
        titleTextView.setTextSize(20);
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        // Create vertical layout for controls
        LinearLayout verticalLayout = new LinearLayout(this);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setPadding(16, 16, 16, 16);
        verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        verticalLayout.addView(titleTextView);
        verticalLayout.addView(goalsStatusTextView);
        verticalLayout.addView(moveCounterTextView);
        verticalLayout.addView(timerTextView);
        verticalLayout.addView(createFlexButtonRow(resetBtn, undoBtn, replayBtn));
        verticalLayout.addView(createHorizontalRow(pauseBtn, muteBtn, howToBtn));
        verticalLayout.addView(spinnerRow);

        // Wrap verticalLayout in ScrollView
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
        scrollView.addView(verticalLayout);

        // Wrap ScrollView in FrameLayout to center vertically
        FrameLayout scrollWrapper = new FrameLayout(this);
        scrollWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        // Apply system insets to avoid camera
        ViewCompat.setOnApplyWindowInsetsListener(scrollWrapper, (v, insets) -> {
            Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sysInsets.left, sysInsets.top, sysInsets.right, sysInsets.bottom);
            return insets;
        });

        // Add the ScrollView inside with vertical centering
        FrameLayout.LayoutParams centeredParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
        );
        scrollWrapper.addView(scrollView, centeredParams);

        return scrollWrapper;
    }

    private View createPortraitControlsLayout(LinearLayout spinnerRow) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        LinearLayout verticalLayout = new LinearLayout(this);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setPadding(16, 16, 16, 16);
        verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        verticalLayout.addView(goalsStatusTextView);
        verticalLayout.addView(moveCounterTextView);
        verticalLayout.addView(timerTextView);
        verticalLayout.addView(createHorizontalRow(resetBtn, undoBtn, replayBtn));
        verticalLayout.addView(createHorizontalRow(pauseBtn, muteBtn, howToBtn));
        verticalLayout.addView(spinnerRow);

        scrollView.addView(verticalLayout);
        return scrollView;
    }

    private LinearLayout createHorizontalRow(View... views) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        for (View v : views) row.addView(v);
        return row;
    }

    private FlexboxLayout createFlexButtonRow(View... views) {
        FlexboxLayout row = new FlexboxLayout(this);
        row.setFlexDirection(FlexDirection.ROW);
        row.setFlexWrap(FlexWrap.WRAP);
        row.setJustifyContent(JustifyContent.CENTER);
        row.setPadding(0, 8, 0, 8);

        FlexboxLayout.LayoutParams btnParams = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(8, 0, 8, 0);

        for (View v : views) {
            v.setLayoutParams(btnParams);
            row.addView(v);
        }

        return row;
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
        isSolved = savedInstanceState.getBoolean("is_solved", false);
        restorePersistentAppState(savedInstanceState);
        restoreTimerState(savedInstanceState);
        applyMuteState();
    }

    private void observeViewModel() {
        observeMazePosition();
        observeMazeStatus();
        observeMoveFeedback();
        observeGameState();
        observeButtonStates();
        observeElapsedTime();
    }

    private void observeMazePosition() {
        viewModel.getEyeballRowLiveData().observe(this, row -> {
            Integer col = viewModel.getEyeballColLiveData().getValue();
            if (col != null) {
                mazeView.setEyeballPosition(row, col);
                mazeView.setGoalPositions(viewModel.getGoalPoints());
            }
        });

        viewModel.getEyeballColLiveData().observe(this, col -> {
            Integer row = viewModel.getEyeballRowLiveData().getValue();
            if (row != null) {
                mazeView.setEyeballPosition(row, col);
            }
        });

        viewModel.getEyeballDirLiveData().observe(this, mazeView::setDirection);
    }

    private void observeMazeStatus() {
        viewModel.isCurrentGoalLiveData().observe(this, isGoal -> {
            mazeView.setCurrentSquareIsGoal(isGoal);
            mazeView.invalidate();
        });
    }

    private void observeMoveFeedback() {
        viewModel.getMoveCountLiveData().observe(this, count -> moveCounterTextView.setText(getString(R.string.moves_format, count)));

        viewModel.getMoveHappenedLiveData().observe(this, happened -> {
            if (Boolean.TRUE.equals(happened)) {
                playMoveSound();
                viewModel.clearMoveHappened();
            }
        });

        viewModel.getMoveStatusLiveData().observe(this, this::showMoveStatusMsg);
    }

    private void showMoveStatusMsg(Message message) {
        if (message == null || message == Message.OK) return;

        SnackbarUtils.showMoveBlocked(findViewById(android.R.id.content), message.name());

        viewModel.clearMoveStatusLiveData();
    }

    private void observeGameState() {
        viewModel.getGoalsRemainingLiveData().observe(this, this::handleGoalsRemaining);
    }

    private void handleGoalsRemaining(int remaining) {
        if (goalsStatusTextView == null) return;

        if (remaining <= 0) {
            if (isSolved) {
                handleAlreadySolved();
            } else {
                handleLevelSolved();
            }
        }

        updateGoalStatusText(remaining);
    }

    private void handleAlreadySolved() {
        goalsStatusTextView.setText(getString(R.string.solved));
        replayBtn.setEnabled(viewModel.getPlaybackTrail().size() > 1);
        setGameButtonsEnabled(false);
    }

    private void handleLevelSolved() {
        isSolved = true;
        viewModel.markSolved();
        viewModel.stopTimer();

        long elapsed = viewModel.getSolveTimeMillis();
        updateTimerDisplay(elapsed);
        goalsStatusTextView.setText(getString(R.string.solved));

        playSolvedFeedback(elapsed);
        showLevelCompleteMsg();
        freezeGameAfterSolve();
    }

    private void playSolvedFeedback(long elapsed) {
        playSoundEffect(winSoundId);

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
                levelSpinner.setSelection(viewModel.getCurrentLevelIndex());
                resetLevel();
            } else {
                ToastUtils.showNoMoreLevels(this);
            }
        });
    }

    private void freezeGameAfterSolve() {
        setGameButtonsEnabled(false);
    }

    private void updateGoalStatusText(int remaining) {
        String goalText = getResources().getQuantityString(R.plurals.goals_remaining, remaining, remaining);
        goalsStatusTextView.setText(goalText);

        Drawable flagIcon = AppCompatResources.getDrawable(this, R.drawable.baseline_flag_circle_24);
        if (flagIcon != null) {
            flagIcon.setBounds(0, 0, flagIcon.getIntrinsicWidth(), flagIcon.getIntrinsicHeight());
            goalsStatusTextView.setCompoundDrawables(flagIcon, null, flagIcon, null);
        }
    }

    private void observeButtonStates() {
        viewModel.canUndoLiveData().observe(this, canUndo -> {
            if (undoBtn != null) {
                undoBtn.setEnabled(gameButtonsEnabled && Boolean.TRUE.equals(canUndo));
            }
        });

        viewModel.getCanReplayLiveData().observe(this, canReplay -> {
            if (replayBtn != null) {
                replayBtn.setEnabled(gameButtonsEnabled && Boolean.TRUE.equals(canReplay));
            }
        });

        viewModel.getIsTimerStartedLiveData().observe(this, hasStarted -> {
            updatePauseButtonEnabledState();
        });

        viewModel.getIsPausedLiveData().observe(this, isPaused -> {
            updatePauseButtonUI(Boolean.TRUE.equals(isPaused));
            updatePauseButtonEnabledState();
        });
    }

    private void observeElapsedTime() {
        viewModel.getElapsedTimeLiveData().observe(this, elapsed -> {
            if (!isSolved) {
                updateTimerDisplay(elapsed);
            }
        });
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
        outState.putBoolean("pause_enabled", pauseBtn.isEnabled());
        outState.putInt("selected_level", viewModel.getCurrentLevelIndex());

        // Convert Point list to SerializablePoint list
        ArrayList<SerializablePoint> serializableTrail = new ArrayList<>();
        for (Point p : viewModel.getPlaybackTrail()) {
            serializableTrail.add(new SerializablePoint(p.x, p.y));
        }
        outState.putSerializable("move_trail", serializableTrail);
        outState.putBoolean("is_solved", isSolved);
        outState.putBoolean("is_muted", isMuted);
        viewModel.saveTimerStateToBundle(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
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

        if (titleTextView != null) {
            titleTextView.setText(title); // landscape
        } else {
            setTitle(title); // portrait
        }
    }

    private void playMoveSound() {
        playSoundEffect(moveSoundId);
    }

    private void playBackMoves() {
        List<Point> trail = viewModel.getPlaybackTrail();
        if (trail.isEmpty() || isPlayingBack) return;

        isPlayingBack = true;
        resetBtn.setEnabled(false);
        undoBtn.setEnabled(false);

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
        timerTextView.setText(getString(R.string.time_format, "00:00"));

        Integer row = viewModel.getEyeballRowLiveData().getValue();
        Integer col = viewModel.getEyeballColLiveData().getValue();

        if (row != null && col != null) {
            viewModel.clearPlaybackTrail();
            viewModel.addMoveToTrail(new Point(col, row));
        }
    }

    private void updateTimerDisplay(long elapsedMillis) {
        if (timerTextView == null) return;

        int seconds = (int) (elapsedMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String time = String.format(Locale.US, "%02d:%02d", minutes, seconds);
        timerTextView.setText(getString(R.string.time_format, time));
    }

    private void resetLevel() {
        isSolved = false;
        viewModel.stopTimer();
        viewModel.resetTimer();
        viewModel.clearPlaybackTrail();
        syncMazeViewFromViewModel();
        recordStartPosition();
        resetPausedStateUI();
    }

    private void setGameButtonsEnabled(boolean enabled) {
        gameButtonsEnabled = enabled;
        setButtonsExceptPauseEnabled(enabled);
        if (pauseBtn != null) pauseBtn.setEnabled(enabled);
    }

    private void setButtonsExceptPauseEnabled(boolean enabled) {
        if (resetBtn != null) resetBtn.setEnabled(enabled || isSolved); // Keep enabled if maze is solved
        if (undoBtn != null) undoBtn.setEnabled(enabled);
        if (replayBtn != null) replayBtn.setEnabled(enabled || isSolved);
    }

    private void updateGameButtonsState() {
        resetBtn.setEnabled(gameButtonsEnabled || isSolved);
        undoBtn.setEnabled(gameButtonsEnabled);
        pauseBtn.setEnabled(gameButtonsEnabled);
    }

    private void restoreTimerState(Bundle savedInstanceState) {
        viewModel.restoreTimerStateFromBundle(savedInstanceState);
        applyTimerStateFromViewModel();
    }

    private void applyTimerStateFromViewModel() {
        if (isSolved) {
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
        if (muteBtn == null) return;

        int icon = muted ? R.drawable.baseline_volume_off_24 : R.drawable.baseline_volume_up_24;
        String description = muted ? getString(R.string.mute) : getString(R.string.unmute);

        muteBtn.setImageResource(icon);
        muteBtn.setContentDescription(description);
    }

    private void applyMuteState() {
        updateMuteIcon(isMuted);
    }

    private void playSoundEffect(int soundId) {
        if (!isMuted && soundsLoaded && soundPool != null) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.pauseTimer();
    }
}
