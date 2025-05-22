package nz.ac.ara.tre46.eyeballmaze;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import android.graphics.Point;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;
import nz.ac.ara.tre46.eyeballmaze.utils.SerializablePoint;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;
    private TextView titleTextView;
    private TextView goalsStatusTextView;
    private TextView moveCounterTextView;
    private MediaPlayer moveSfx;
    private MediaPlayer winSfx;
    private MediaPlayer badMoveSfx;
    private boolean isMuted = false;
    private final Deque<Point> movePlaybackTrail = new ArrayDeque<>();

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
        }

        setContentView(R.layout.activity_main);

        moveSfx = MediaPlayer.create(this, R.raw.move);
        winSfx = MediaPlayer.create(this, R.raw.win);
        badMoveSfx = MediaPlayer.create(this, R.raw.bad);

        viewModel = new ViewModelProvider(
                this,
                new EyeballMazeViewModelFactory(getApplicationContext())
        ).get(EyeballMazeViewModel.class);

        if (savedInstanceState == null) {
            viewModel.setLevel(0);  // Only on first launch
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
            int topInset = isLandscape ? 0 : insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        // MazeView
        mazeView = new MazeView(this);
        Bitmap eyeballBmp = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
        mazeView.setEyeballBitmap(eyeballBmp);

        mazeView.setOnCellTapListener((row, col) -> {
            if (viewModel.canMoveTo(row, col)) {
//                viewModel.clickToMoveToward(row, col);
                movePlaybackTrail.add(new Point(col, row)); // Save successful move
            } else {
                // Show sound + visual feedback for invalid move
                if (!isMuted && badMoveSfx != null) {
                    badMoveSfx.seekTo(0);
                    badMoveSfx.start();
                }

                mazeView.setFailedMoveAt(row, col);
//                mazeView.postDelayed(() -> mazeView.clearFailedMove(), 500);
            }

            viewModel.clickToMoveToward(row, col); // Handles messages
        });

        LinearLayout.LayoutParams mazeParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : 0,
                0.8f
        );
        root.addView(mazeView, mazeParams);

        // Controls container
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(isLandscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

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

        Button resetBtn = new Button(this);
        resetBtn.setText(getString(R.string.reset));
        resetBtn.setOnClickListener(v -> {
            viewModel.resetMaze();
            mazeView.setGoalPositions(viewModel.getGoalPoints());
            movePlaybackTrail.clear();
            syncMazeViewFromViewModel();
            recordStartPosition();
        });

        Button undoBtn = new Button(this);
        undoBtn.setText(getString(R.string.undo));
        undoBtn.setOnClickListener(v -> {
            viewModel.undo();
            syncMazeViewFromViewModel();

            if (!movePlaybackTrail.isEmpty()) {
                movePlaybackTrail.removeLast();
            }
        });
        viewModel.canUndoLiveData().observe(this, canUndo -> undoBtn.setEnabled(Boolean.TRUE.equals(canUndo)));

        Button muteBtn = new Button(this);
        muteBtn.setText(getString(R.string.mute));
        int muteWidthDp = dpToPx(15);
        float density = getResources().getDisplayMetrics().density;
        int muteWidthPx = (int) (muteWidthDp * density + 0.5f);

        LinearLayout.LayoutParams muteParams = new LinearLayout.LayoutParams(muteWidthPx, LayoutParams.WRAP_CONTENT);
        muteBtn.setLayoutParams(muteParams);
        muteBtn.setOnClickListener(v -> {
            isMuted = !isMuted;
            muteBtn.setText(getString(isMuted ? R.string.unmute : R.string.mute));

            float volume = isMuted ? 0f : 1f;
            if (moveSfx != null) moveSfx.setVolume(volume, volume);
            if (winSfx != null) winSfx.setVolume(volume, volume);
            if (badMoveSfx != null) badMoveSfx.setVolume(volume, volume);
        });

        Button replayBtn = new Button(this);
        replayBtn.setText(getString(R.string.replay));
        replayBtn.setOnClickListener(v -> playBackMoves());

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

            LinearLayout verticalLayout = new LinearLayout(this);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);
            verticalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            ));

            LinearLayout centerControls = new LinearLayout(this);
            centerControls.setOrientation(LinearLayout.VERTICAL);
            centerControls.setGravity(Gravity.CENTER);
            centerControls.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    0,
                    1f
            ));

            centerControls.addView(moveCounterTextView);
            centerControls.addView(resetBtn);
            centerControls.addView(undoBtn);
            centerControls.addView(replayBtn);
            centerControls.addView(muteBtn);
            centerControls.addView(spinnerRow);

            verticalLayout.addView(titleTextView);
            verticalLayout.addView(goalsStatusTextView);
            verticalLayout.addView(centerControls);

            controls.addView(verticalLayout);
        } else {
            controls.setGravity(Gravity.CENTER);
//            controls.setPadding(16, 16, 16, 16);

            LinearLayout verticalLayout = new LinearLayout(this);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);
            verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL);

            verticalLayout.addView(goalsStatusTextView);
            verticalLayout.addView(moveCounterTextView);

            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);
            buttonRow.setGravity(Gravity.CENTER_HORIZONTAL);

            buttonRow.addView(resetBtn);
            buttonRow.addView(undoBtn);
            buttonRow.addView(replayBtn);
            buttonRow.addView(muteBtn);

            verticalLayout.addView(buttonRow);
            verticalLayout.addView(spinnerRow);

            controls.addView(verticalLayout);
        }

        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT,
                0.3f // how much screen to take up 30%
        );
        root.addView(controls, controlsParams);

        viewModel.getMoveCount().observe(this, count -> moveCounterTextView.setText(getString(R.string.moves_format, count)));

        setContentView(root);
        syncMazeViewFromViewModel();
        recordStartPosition();

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
                    levelSpinner.setEnabled(false);
                    viewModel.setLevel(position);
                    syncMazeViewFromViewModel();
                    movePlaybackTrail.clear();
                    recordStartPosition();
                    levelSpinner.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set up observers (unchanged from before)
        mazeView.setColorProvider(viewModel::getColorAt);
        mazeView.setShapeProvider(viewModel::getShapeAt);
        mazeView.setTypeProvider(viewModel::getTypeAt);
        mazeView.setGoalPositions(viewModel.getGoalPoints());

        viewModel.getEyeballRow().observe(this, row -> {
            Integer col = viewModel.getEyeballCol().getValue();
            if (col != null) {
                mazeView.setEyeballPosition(row, col);
                mazeView.setGoalPositions(viewModel.getGoalPoints());

//                if (movePlaybackTrail.isEmpty()) {
//                    movePlaybackTrail.add(new Point(col, row));
//                }
            }
        });

        viewModel.getEyeballCol().observe(this, col -> {
            Integer row = viewModel.getEyeballRow().getValue();
            if (row != null) {
                mazeView.setEyeballPosition(row, col);
            }
        });

        viewModel.getEyeballDir().observe(this, mazeView::setDirection);
        viewModel.isCurrentGoal().observe(this, isGoal -> {
            mazeView.setCurrentSquareIsGoal(isGoal);
            mazeView.invalidate();
        });

        viewModel.getMoveHappened().observe(this, happened -> {
            if (Boolean.TRUE.equals(happened)) {
                playMoveSound();
                viewModel.clearMoveHappened();
            }
        });

        viewModel.getMoveStatus().observe(this, message -> {
            if (message != null && message != Message.OK) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.move_blocked, message.name()),
                        Snackbar.LENGTH_SHORT
                ).show();
                viewModel.clearMoveStatus();
            }
        });

        viewModel.getGoalsRemaining().observe(this, remaining -> {
            if (goalsStatusTextView == null) return;

            if (remaining <= 0) {
                goalsStatusTextView.setText(getString(R.string.solved));

                if (!isMuted && winSfx != null) {
                    winSfx.start();
                }

                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.solved)
                        .setMessage(R.string.next_level)
                        .setPositiveButton(R.string.select, (dialog, which) -> {
                            int next = viewModel.getCurrentLevelIndex() + 1;
                            if (next < viewModel.getLevelCount()) {
                                viewModel.setLevel(next);
                                levelSpinner.setSelection(viewModel.getCurrentLevelIndex());
                                syncMazeViewFromViewModel();
                                movePlaybackTrail.clear();
                                recordStartPosition();
                            } else {
                                new AlertDialog.Builder(this)
                                        .setTitle(R.string.no_more_levels)
                                        .setMessage(R.string.no_more_levels_message)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            }
                        })
                        .setNegativeButton(R.string.reset, (dialog, which) -> {
                            viewModel.resetMaze();
                            mazeView.setGoalPositions(viewModel.getGoalPoints());
                        })
                        .show();
            } else {
//                goalsStatusTextView.setText(
//                        getResources().getQuantityString(R.plurals.goals_remaining, remaining, remaining)
//                );
                String star = getString(R.string.goal);
                String goalText = getString(R.string.goals_remaining_star, star, remaining);
                goalsStatusTextView.setText(goalText);
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
        mazeView.setTypeProvider(viewModel::getTypeAt);
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

    @SuppressWarnings("SameParameterValue")
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void playBackMoves() {
        if (movePlaybackTrail.isEmpty()) return;

        new Thread(() -> {
            for (Point p : movePlaybackTrail) {
                int row = p.y;
                int col = p.x;

                runOnUiThread(() -> {
                    mazeView.setEyeballPosition(row, col);
                    mazeView.invalidate();
                });

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void recordStartPosition() {
        Integer row = viewModel.getEyeballRow().getValue();
        Integer col = viewModel.getEyeballCol().getValue();

        if (row != null && col != null) {
            movePlaybackTrail.clear();
            movePlaybackTrail.add(new Point(col, row));
        }
    }
}
