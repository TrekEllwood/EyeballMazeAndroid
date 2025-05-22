package nz.ac.ara.tre46.eyeballmaze;

import android.graphics.Point;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;
    private TextView titleTextView;
    private TextView goalsStatusTextView;
    private TextView moveCounterTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // MazeView
        mazeView = new MazeView(this);
        Bitmap eyeballBmp = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
        mazeView.setEyeballBitmap(eyeballBmp);
        mazeView.setOnCellTapListener((row, col) -> viewModel.clickToMoveToward(row, col));

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
        });

        Button undoBtn = new Button(this);
        undoBtn.setText(getString(R.string.undo));
        undoBtn.setOnClickListener(v -> {
            viewModel.undo();
            syncMazeViewFromViewModel();
        });
        viewModel.canUndoLiveData().observe(this, canUndo -> undoBtn.setEnabled(Boolean.TRUE.equals(canUndo)));

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
            centerControls.addView(spinnerRow);

            verticalLayout.addView(titleTextView);
            verticalLayout.addView(goalsStatusTextView);
            verticalLayout.addView(centerControls);

            controls.addView(verticalLayout);
        } else {
            controls.setGravity(Gravity.CENTER);
            controls.setPadding(16, 16, 16, 16);

            LinearLayout verticalLayout = new LinearLayout(this);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);
            verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL);

            verticalLayout.addView(goalsStatusTextView);
            verticalLayout.addView(moveCounterTextView);
            verticalLayout.addView(resetBtn);
            verticalLayout.addView(undoBtn);
            verticalLayout.addView(spinnerRow);

            controls.addView(verticalLayout);
        }

        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT,
                0.2f
        );
        root.addView(controls, controlsParams);

        viewModel.getMoveCount().observe(this, count -> moveCounterTextView.setText(getString(R.string.moves_format, count)));

        setContentView(root);
        syncMazeViewFromViewModel();

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

        viewModel.getMoveStatus().observe(this, message -> {
            if (message != null && message != Message.OK) {
                Snackbar.make(
                        root,
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

                new AlertDialog.Builder(this)
                        .setTitle(R.string.solved)
                        .setMessage(R.string.next_level)
                        .setPositiveButton(R.string.select, (dialog, which) -> {
                            int next = viewModel.getCurrentLevelIndex() + 1;
                            if (next < viewModel.getLevelCount()) {
                                viewModel.setLevel(next);
                                levelSpinner.setSelection(viewModel.getCurrentLevelIndex());
                                syncMazeViewFromViewModel();
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
}
