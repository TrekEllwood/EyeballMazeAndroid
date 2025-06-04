package nz.ac.ara.tre46.eyeballmaze;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
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
import android.widget.Toast;

import android.graphics.Point;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.graphics.Insets;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.ui.TutorialVideoDialogFragment;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;
import nz.ac.ara.tre46.eyeballmaze.utils.SerializablePoint;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;
    private TextView titleTextView, goalsStatusTextView, moveCounterTextView, timerTextView;
    private MediaPlayer moveSfx, winSfx, badMoveSfx;
    private Button resetBtn, undoBtn, replayBtn;
    private ImageButton pauseBtn, muteBtn, howToBtn;
    private Runnable playbackRunnable;
    private final Handler timerHandler = new Handler(), playbackHandler = new Handler();
    private final Deque<Point> movePlaybackTrail = new ArrayDeque<>();
    private long startTime = 0L, pausedTime = 0L, solveTimeMillis = 0L;
    private boolean
            gameButtonsEnabled = true,
            isMuted = false,
            isPaused = false,
            hasTimerStarted = false,
            isSolved = false,
            isPlayingBack = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPaused) {
                long elapsed = System.currentTimeMillis() - startTime;
                updateTimerDisplay(elapsed);
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            ArrayList<SerializablePoint> trailData =
                    (ArrayList<SerializablePoint>) savedInstanceState.getSerializable("move_trail");

            if (trailData != null) {
                movePlaybackTrail.clear();
                for (SerializablePoint sp : trailData) {
                    movePlaybackTrail.add(new Point(sp.x, sp.y));
                }
            }

            restoreTimerState(savedInstanceState);
            isMuted = savedInstanceState.getBoolean("is_muted", false);
        }

//        setContentView(R.layout.activity_main); // Only need if using activity_main.xml

        moveSfx = MediaPlayer.create(this, R.raw.move);
        winSfx = MediaPlayer.create(this, R.raw.win);
        badMoveSfx = MediaPlayer.create(this, R.raw.bad);

//        applyMuteState();

        viewModel = new ViewModelProvider(
                this,
                new EyeballMazeViewModelFactory(getApplicationContext())
        ).get(EyeballMazeViewModel.class);

        if (savedInstanceState == null) {
            viewModel.initializeLevelFromPreferences();
        }

        boolean isLandscape = getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sysInsets.left, sysInsets.top, sysInsets.right, sysInsets.bottom);
            return insets;
        });

        // MazeView
        mazeView = new MazeView(this);
        int tint = ContextCompat.getColor(this, R.color.iconTint);
        Bitmap eyeballBmp = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
        mazeView.setEyeballBitmap(eyeballBmp);

        mazeView.setOnCellTapListener((row, col) -> {
            if (isSolved) return;

            if (viewModel.canMoveTo(row, col)) {
                movePlaybackTrail.add(new Point(col, row)); // Save successful move
                viewModel.updateCanReplay(movePlaybackTrail);

                if (!hasTimerStarted) {
                    startTime = System.currentTimeMillis();
                    timerHandler.post(timerRunnable);
                    hasTimerStarted = true;
                    isPaused = false;
                    setGameButtonsEnabled(true);
                }
            } else {
                // Show sound + visual feedback for invalid move
                if (!isMuted && badMoveSfx != null) {
                    badMoveSfx.seekTo(0);
                    badMoveSfx.start();
                }

                mazeView.setFailedMoveAt(row, col);
            }

            viewModel.clickToMoveToward(row, col); // Handles messages
        });

        // Controls container
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(isLandscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        // Spinner row: label + spinner side by side
        LinearLayout spinnerRow = new LinearLayout(this);
        spinnerRow.setOrientation(LinearLayout.HORIZONTAL);
        spinnerRow.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView spinnerLabel = new TextView(this);
        spinnerLabel.setText(getString(R.string.choose_maze));
        spinnerLabel.setTextSize(16);
        spinnerLabel.setPadding(0, 0, 16, 0);  // space between label and spinner

        Spinner levelSpinner = new Spinner(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        levelSpinner.setLayoutParams(spinnerParams);

        spinnerRow.addView(spinnerLabel);
        spinnerRow.addView(levelSpinner);

        resetBtn = new Button(this);
        resetBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        resetBtn.setText(getString(R.string.reset));
        resetBtn.setOnClickListener(v -> {
            viewModel.resetMaze();
            mazeView.setGoalPositions(viewModel.getGoalPoints());
            pauseBtn.setEnabled(false);
            goToNextLevel();
            viewModel.updateCanReplay(movePlaybackTrail);
        });

        undoBtn = new Button(this);
        undoBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        undoBtn.setText(getString(R.string.undo));
        undoBtn.setOnClickListener(v -> {
            viewModel.undo();
            syncMazeViewFromViewModel();

            if (!movePlaybackTrail.isEmpty()) {
                movePlaybackTrail.removeLast();
                viewModel.updateCanReplay(movePlaybackTrail);
            }
        });
        viewModel.canUndoLiveData().observe(this, canUndo -> {
            if (undoBtn != null) {
                undoBtn.setEnabled(gameButtonsEnabled && Boolean.TRUE.equals(canUndo));
            }
        });

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

        applyMuteState();

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

        replayBtn = new Button(this);
        replayBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        replayBtn.setText(getString(R.string.replay));
        replayBtn.setOnClickListener(v -> playBackMoves());
        viewModel.getCanReplayLiveData().observe(this, canReplay -> {
            if (replayBtn != null) {
                replayBtn.setEnabled(gameButtonsEnabled && Boolean.TRUE.equals(canReplay));
            }
        });

        pauseBtn = new ImageButton(this);
        pauseBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        pauseBtn.setEnabled(false);
        pauseBtn.setBackground(null);
        pauseBtn.setOnClickListener(v -> {
            if (isPaused) {
                // Resume
                long pausedDuration = System.currentTimeMillis() - pausedTime;
                startTime += pausedDuration;
                isPaused = false;
                setGameButtonsEnabled(true);
                resetPausedStateUI();
                timerHandler.post(timerRunnable);
            } else {
                // Pause
                pausedTime = System.currentTimeMillis();
                isPaused = true;
                setGameButtonsEnabled(false);
                setPausedStateUI();
                timerHandler.removeCallbacks(timerRunnable);
                cancelPlaybackAndJumpToEnd();
            }
        });

        timerTextView = new TextView(this);
        timerTextView.setTextSize(16);
        timerTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        goalsStatusTextView = new TextView(this);
        goalsStatusTextView.setTextSize(16);
        goalsStatusTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        goalsStatusTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        moveCounterTextView = new TextView(this);
        moveCounterTextView.setTextSize(16);
        moveCounterTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        if (isLandscape) {
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

            FlexboxLayout row1 = new FlexboxLayout(this);
            row1.setFlexDirection(FlexDirection.ROW);
            row1.setFlexWrap(FlexWrap.WRAP);
            row1.setJustifyContent(JustifyContent.CENTER);
            row1.setPadding(0, 8, 0, 8);

            FlexboxLayout.LayoutParams btnParams = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(8, 0, 8, 0);

            resetBtn.setLayoutParams(btnParams);
            undoBtn.setLayoutParams(btnParams);

            row1.addView(resetBtn);
            row1.addView(undoBtn);
            row1.addView(replayBtn);
            verticalLayout.addView(row1);

            LinearLayout row3 = new LinearLayout(this);
            row3.setOrientation(LinearLayout.HORIZONTAL);
            row3.setGravity(Gravity.CENTER_HORIZONTAL);
            row3.setPadding(0, 8, 0, 0);
            row3.addView(pauseBtn);
            row3.addView(muteBtn);
            row3.addView(howToBtn);
            verticalLayout.addView(row3);

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

            // Add to controls
            controls.addView(scrollWrapper, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            ));
        } else {
            ScrollView scrollView = new ScrollView(this);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            ));

            LinearLayout verticalLayout = new LinearLayout(this);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);
            verticalLayout.setPadding(16, 16, 16, 16);
            verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            scrollView.addView(verticalLayout);

            verticalLayout.addView(goalsStatusTextView);
            verticalLayout.addView(moveCounterTextView);
            verticalLayout.addView(timerTextView);

            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_HORIZONTAL);
            row1.setPadding(0, 8, 0, 8);
            row1.addView(resetBtn);
            row1.addView(undoBtn);
            row1.addView(replayBtn);

            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setGravity(Gravity.CENTER_HORIZONTAL);
            row2.setPadding(0, 8, 0, 8);
            row2.addView(pauseBtn);
            row2.addView(muteBtn);
            row2.addView(howToBtn);

            verticalLayout.addView(row1);
            verticalLayout.addView(row2);
            verticalLayout.addView(spinnerRow);

            controls.removeAllViews();
            controls.setGravity(Gravity.NO_GRAVITY); // Fill space
            controls.addView(scrollView);
        }

        float mazeWeight = 0.5f;
        float controlsWeight = 0.5f;

        // Add mazeView using weight
        LinearLayout.LayoutParams mazeParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : 0,
                isLandscape ? mazeWeight : 1.0f
        );
        root.addView(mazeView, mazeParams);

        // Add controls using weight
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT,
                isLandscape ? controlsWeight : 0f
        );
        root.addView(controls, controlsParams);

        if (savedInstanceState != null) {
            setGameButtonsEnabled(!isPaused);
        }

        ColorStateList tintList = ContextCompat.getColorStateList(this, R.color.icon_tint);
        ImageViewCompat.setImageTintList(pauseBtn, tintList);
        ImageViewCompat.setImageTintList(muteBtn, tintList);

        viewModel.getMoveCountLiveData().observe(this, count -> moveCounterTextView.setText(getString(R.string.moves_format, count)));

        // Set this layout as content view
        setContentView(root);
        syncMazeViewFromViewModel();

        // Force maze to be square based on actual height (in landscape)
        mazeView.post(() -> {
            if (isLandscape) {
                int squareSize = mazeView.getHeight();  // determined by layout weight
                ViewGroup.LayoutParams params = mazeView.getLayoutParams();
                // Force square shape
                params.width = squareSize;
                params.height = squareSize;
                mazeView.setLayoutParams(params);
            } else {
                // In portrait: fit based on available space below controls
                int screenHeight = root.getHeight();
                int controlsHeight = controls.getHeight();
                int availableHeight = screenHeight - controlsHeight;
                int screenWidth = root.getWidth();
                int size = Math.min(screenWidth, availableHeight);

                ViewGroup.LayoutParams params = mazeView.getLayoutParams();
                params.width = size;
                params.height = size;
                mazeView.setLayoutParams(params);
            }

            mazeView.invalidate();
        });

        if (savedInstanceState == null) {
            recordStartPosition(); // only reset timer on first launch, not rotation
        }

        // Spinner setup
        List<String> levelLabels = new ArrayList<>();
        for (int i = 0; i < viewModel.getLevelCount(); i++) {
            levelLabels.add("Maze " + viewModel.getMazeIdAt(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levelLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);
        levelSpinner.setSelection(viewModel.getCurrentLevelIndex());

        AtomicBoolean isFirstSpinnerSelection = new AtomicBoolean(true);
        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstSpinnerSelection.getAndSet(false)) return;

                if (position != viewModel.getCurrentLevelIndex()) {
                    cancelPlaybackAndJumpToEnd();
                    levelSpinner.setEnabled(false);
                    viewModel.setLevel(position);
                    goToNextLevel();
                    pauseBtn.setEnabled(false);
                    levelSpinner.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Observers
        mazeView.setColorProvider(viewModel::getColorAt);
        mazeView.setShapeProvider(viewModel::getShapeAt);
//        mazeView.setTypeProvider(viewModel::getTypeAt);
        mazeView.setGoalPositions(viewModel.getGoalPoints());

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
        viewModel.isCurrentGoalLiveData().observe(this, isGoal -> {
            mazeView.setCurrentSquareIsGoal(isGoal);
            mazeView.invalidate();
        });

        viewModel.getMoveHappenedLiveData().observe(this, happened -> {
            if (Boolean.TRUE.equals(happened)) {
                playMoveSound();
                viewModel.clearMoveHappened();
            }
        });

        viewModel.getMoveStatusLiveData().observe(this, message -> {
            if (message != null && message != Message.OK) {
                Snackbar snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.move_blocked, message.name()),
                        Snackbar.LENGTH_SHORT
                );
                // Move position
                View snackbarView = snackbar.getView();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                params.gravity = Gravity.CENTER;
                params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
                snackbarView.setLayoutParams(params);
                snackbar.show();

                viewModel.clearMoveStatusLiveData();
            }
        });

        viewModel.getGoalsRemainingLiveData().observe(this, remaining -> {
            if (goalsStatusTextView == null) return;

            if (remaining <= 0) {
                if (isSolved) {
                    goalsStatusTextView.setText(getString(R.string.solved));
                    replayBtn.setEnabled(movePlaybackTrail.size() > 1);
                    pauseBtn.setEnabled(false);
                    setGameButtonsEnabled(false);
                    return;
                }

                isSolved = true;
                solveTimeMillis = isPaused ? pausedTime - startTime : System.currentTimeMillis() - startTime;
                updateTimerDisplay(solveTimeMillis);
                goalsStatusTextView.setText(getString(R.string.solved));

                if (!isMuted && winSfx != null) {
                    winSfx.start();
                }

                final String solveTimeFormatted = String.format(Locale.US, "%02d:%02d",
                        (solveTimeMillis / 1000) / 60,
                        (solveTimeMillis / 1000) % 60
                );

                String message = getString(R.string.solved) + "\n" + getString(R.string.time_format, solveTimeFormatted);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                Snackbar snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.level_complete),
                        Snackbar.LENGTH_LONG
                );

                snackbar.setAction(getString(R.string.next), v -> {
                    int next = viewModel.getCurrentLevelIndex() + 1;
                    if (next < viewModel.getLevelCount()) {
                        cancelPlaybackAndJumpToEnd();
                        viewModel.setLevel(next);
                        levelSpinner.setSelection(viewModel.getCurrentLevelIndex());
                        goToNextLevel();
                    } else {
                        Toast endToast = Toast.makeText(MainActivity.this, getString(R.string.no_more_levels), Toast.LENGTH_LONG);
                        endToast.show();
                    }

                    restoreTimerState();
                });

                snackbar.show();

                timerHandler.removeCallbacks(timerRunnable);
                isPaused = true;
                setGameButtonsEnabled(false);
                hasTimerStarted = false;
                pauseBtn.setEnabled(false);
                return;
            }

            String goalText = getResources().getQuantityString(R.plurals.goals_remaining, remaining, remaining);
            goalsStatusTextView.setText(goalText);

            Drawable flagIcon = AppCompatResources.getDrawable(this, R.drawable.baseline_flag_circle_24);
            if (flagIcon != null) {
                flagIcon.setTint(ContextCompat.getColor(this, R.color.iconTint));
                flagIcon.setBounds(0, 0, flagIcon.getIntrinsicWidth(), flagIcon.getIntrinsicHeight());
                goalsStatusTextView.setCompoundDrawables(flagIcon, null, flagIcon, null);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_level", viewModel.getCurrentLevelIndex());

        // Convert Point list to SerializablePoint list
        ArrayList<SerializablePoint> serializableTrail = new ArrayList<>();
        for (Point p : movePlaybackTrail) {
            serializableTrail.add(new SerializablePoint(p.x, p.y));
        }

        outState.putSerializable("move_trail", serializableTrail);
        // Keep time
        outState.putLong("start_time", startTime);
        outState.putBoolean("has_timer_started", hasTimerStarted);
        outState.putBoolean("is_paused", isPaused);
        outState.putLong("paused_time", pausedTime);
        outState.putBoolean("is_solved", isSolved);
        outState.putLong("solve_time_millis", solveTimeMillis);
        outState.putBoolean("is_muted", isMuted);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (moveSfx != null) {
            moveSfx.release();
            moveSfx = null;
        }
        if (winSfx != null) {
            winSfx.release();
            winSfx = null;
        }
        if (badMoveSfx != null) {
            badMoveSfx.release();
            badMoveSfx = null;
        }
    }

    private void syncMazeViewFromViewModel() {
        mazeView.setBoardSize(viewModel.getBoardHeight(), viewModel.getBoardWidth());
        mazeView.setGoalPositions(viewModel.getGoalPoints());
        mazeView.setColorProvider(viewModel::getColorAt);
        mazeView.setShapeProvider(viewModel::getShapeAt);
//        mazeView.setTypeProvider(viewModel::getTypeAt);
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
        if (!isMuted && moveSfx != null) {
            moveSfx.seekTo(0);
            moveSfx.start();
        }
    }

    private void playBackMoves() {
        if (movePlaybackTrail.isEmpty() || isPlayingBack) return;

        isPlayingBack = true;
        resetBtn.setEnabled(false);
        undoBtn.setEnabled(false);

        final List<Point> trail = new ArrayList<>(movePlaybackTrail);
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
            for (Point p : movePlaybackTrail) {
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
        hasTimerStarted = false;
        timerHandler.removeCallbacks(timerRunnable);
        timerTextView.setText(getString(R.string.time_format, "00:00"));

        Integer row = viewModel.getEyeballRowLiveData().getValue();
        Integer col = viewModel.getEyeballColLiveData().getValue();

        if (row != null && col != null) {
            movePlaybackTrail.clear();
            movePlaybackTrail.add(new Point(col, row));
            viewModel.updateCanReplay(movePlaybackTrail);
        }
    }

    private void updateTimerDisplay(long elapsedMillis) {
        int seconds = (int) (elapsedMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String time = String.format(Locale.US, "%02d:%02d", minutes, seconds);
        timerTextView.setText(getString(R.string.time_format, time));

        if (pauseBtn != null) {
            pauseBtn.setEnabled(elapsedMillis > 0);
        }
    }

    private void restoreTimerState() {
        if (!hasTimerStarted || pauseBtn == null) {
            updateTimerDisplay(solveTimeMillis);
            pauseBtn.setEnabled(false);
            resetPausedStateUI();
            return;
        }

        long elapsed;
        if (isPaused && !isSolved) {
            elapsed = pausedTime - startTime;
            setPausedStateUI();
        } else {
            elapsed = System.currentTimeMillis() - startTime;
            resetPausedStateUI();
            timerHandler.post(timerRunnable);
        }

        updateTimerDisplay(elapsed);
        pauseBtn.setEnabled(elapsed > 0 && !isSolved);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        if (hasTimerStarted && !isPaused) {
            pausedTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreTimerState();
    }

    private void goToNextLevel() {
        isSolved = false;
        isPaused = false;
        hasTimerStarted = false;
        solveTimeMillis = 0L;
        movePlaybackTrail.clear();
        syncMazeViewFromViewModel();
        recordStartPosition();
        resetPausedStateUI();
    }

    private void setPausedStateUI() {
        pauseBtn.setImageResource(R.drawable.baseline_play_circle_24);
        pauseBtn.setContentDescription(getString(R.string.resume));
        mazeView.setVisibility(View.INVISIBLE);
    }

    private void resetPausedStateUI() {
        pauseBtn.setImageResource(R.drawable.baseline_pause_circle_24);
        pauseBtn.setContentDescription(getString(R.string.pause));
        mazeView.setVisibility(View.VISIBLE);
    }

    private void setMuteStateUI() {
        muteBtn.setImageResource(R.drawable.baseline_volume_up_24);
        muteBtn.setContentDescription(getString(R.string.unmute));
    }

    private void resetMuteStateUI() {
        muteBtn.setImageResource(R.drawable.baseline_volume_off_24);
        muteBtn.setContentDescription(getString(R.string.mute));
    }

    private void setGameButtonsEnabled(boolean enabled) {
        gameButtonsEnabled = enabled;
        if (resetBtn != null)
            resetBtn.setEnabled(enabled || isSolved); // Keep enabled if maze is solved
        if (undoBtn != null) undoBtn.setEnabled(enabled);
        if (replayBtn != null) replayBtn.setEnabled(enabled || isSolved);
    }

    private void updateGameButtonsState() {
        resetBtn.setEnabled(gameButtonsEnabled || isSolved);
        undoBtn.setEnabled(gameButtonsEnabled);
    }

    private void restoreTimerState(Bundle savedInstanceState) {
        startTime = savedInstanceState.getLong("start_time", 0L);
        hasTimerStarted = savedInstanceState.getBoolean("has_timer_started", false);
        isPaused = savedInstanceState.getBoolean("is_paused", false);
        pausedTime = savedInstanceState.getLong("paused_time", 0L);
        isSolved = savedInstanceState.getBoolean("is_solved", false);
        solveTimeMillis = savedInstanceState.getLong("solve_time_millis", 0L);
    }

    private void applyMuteState() {
        float volume = isMuted ? 0f : 1f;

        if (moveSfx != null) moveSfx.setVolume(volume, volume);
        if (winSfx != null) winSfx.setVolume(volume, volume);
        if (badMoveSfx != null) badMoveSfx.setVolume(volume, volume);

        if (isMuted) {
            resetMuteStateUI();
        } else {
            setMuteStateUI();
        }
    }
}
