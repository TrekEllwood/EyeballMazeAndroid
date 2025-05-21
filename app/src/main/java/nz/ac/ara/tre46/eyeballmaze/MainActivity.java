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
import androidx.lifecycle.ViewModelProvider;

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

        // Controls
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(isLandscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        Spinner levelSpinner = new Spinner(this);
        Button resetBtn = new Button(this);
        resetBtn.setText(getString(R.string.reset));
        resetBtn.setOnClickListener(v -> {
            viewModel.resetMaze();
            mazeView.setGoalPositions(viewModel.getGoalPoints());
            updateGoalStatus();
        });

        goalsStatusTextView = new TextView(this);
        goalsStatusTextView.setTextSize(16);
        goalsStatusTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        goalsStatusTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        if (isLandscape) {
            // Landscape layout
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

            verticalLayout.addView(titleTextView);
            verticalLayout.addView(goalsStatusTextView);

            LinearLayout centerControls = new LinearLayout(this);
            centerControls.setOrientation(LinearLayout.VERTICAL);
            centerControls.setGravity(Gravity.CENTER);
            centerControls.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    0,
                    1f
            ));

            centerControls.addView(levelSpinner);
            centerControls.addView(resetBtn);
            verticalLayout.addView(centerControls);

            controls.addView(verticalLayout);
        } else {
            // Portrait layout
            controls.setGravity(Gravity.CENTER);
            controls.setPadding(16, 16, 16, 16);

            LinearLayout verticalLayout = new LinearLayout(this);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);
            verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL);

            verticalLayout.addView(goalsStatusTextView);
            verticalLayout.addView(levelSpinner);
            verticalLayout.addView(resetBtn);

            controls.addView(verticalLayout);
        }

        // Add controls to root
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT,
                0.2f
        );
        root.addView(controls, controlsParams);

        syncMazeViewFromViewModel();

        // Build level spinner adapter
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

        setContentView(root);

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
            updateGoalStatus();
            mazeView.invalidate();
        });

        viewModel.getMoveStatus().observe(this, message -> {
            if (message != null && message != Message.OK) {
                Snackbar.make(root, "Move blocked: " + message.name(), Snackbar.LENGTH_SHORT).show();
                viewModel.clearMoveStatus();
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
        updateGoalStatus();
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

    private void updateGoalStatus() {
        if (goalsStatusTextView == null) return;

        Set<Point> goalPoints = viewModel.getGoalPoints();
        int goalsRemaining = goalPoints.size();

        Integer row = viewModel.getEyeballRow().getValue();
        Integer col = viewModel.getEyeballCol().getValue();

        // Subtract if eyeball is currently on a goal square
        if (row != null && col != null && goalPoints.contains(new Point(col, row))) {
            goalsRemaining -= 1;
        }

        if (goalsRemaining <= 0) {
            goalsStatusTextView.setText(getString(R.string.solved));
        } else {
            goalsStatusTextView.setText(
                    getResources().getQuantityString(R.plurals.goals_remaining,
                            goalsRemaining,
                            goalsRemaining
                    )
            );
        }
    }
}
