package nz.ac.ara.tre46.eyeballmaze;

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
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;

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

        syncMazeViewFromViewModel();

        // Controls
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(isLandscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(16, 16, 16, 16);

        // Spinner
        Spinner levelSpinner = new Spinner(this);
        controls.addView(levelSpinner);

        // Reset button
        Button resetBtn = new Button(this);
        resetBtn.setText(getString(R.string.reset));
        resetBtn.setOnClickListener(v -> viewModel.resetMaze());
        controls.addView(resetBtn);

        // Add controls to root
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT,
                0.2f
        );
        root.addView(controls, controlsParams);

        // Build level spinner adapter
        List<String> levelLabels = new ArrayList<>();
        for (int i = 0; i < viewModel.getLevelCount(); i++) {
            levelLabels.add("Level " + (i + 1));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levelLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);

        // Set spinner to current level
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
//                    mazeView.invalidate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
//                mazeView.invalidate();
            }
        });

        viewModel.getEyeballCol().observe(this, col -> {
            Integer row = viewModel.getEyeballRow().getValue();
            if (row != null) {
                mazeView.setEyeballPosition(row, col);
//                mazeView.invalidate();
            }
        });

        viewModel.getEyeballDir().observe(this, mazeView::setDirection);

        viewModel.isCurrentGoal().observe(this, isGoal -> {
            mazeView.setCurrentSquareIsGoal(isGoal);
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
        mazeView.invalidate();
    }
}
