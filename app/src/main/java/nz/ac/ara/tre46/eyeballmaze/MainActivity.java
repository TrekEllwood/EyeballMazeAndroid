package nz.ac.ara.tre46.eyeballmaze;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.enums.Message;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;
import nz.ac.ara.tre46.eyeballmaze.models.Game;
import nz.ac.ara.tre46.eyeballmaze.models.Square; // Only for DEBUGGING
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;
    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Instantiate Game and load level text
        game = new Game();
        String[] levelLines = loadLevelFromRaw(this, R.raw.levels);
        game.loadLevelFromText(levelLines);
        game.setLevel(0); // Force start on level 1 (index 0)

        // 2) ViewModel
        viewModel = new ViewModelProvider(this,
                new EyeballMazeViewModelFactory((IGame) game))
                .get(EyeballMazeViewModel.class);

        // DEBUG
        final boolean[] logged = { false }; // simple one‐time guard
        viewModel.getBoard().observe(this, board -> {
            if (logged[0] || board == null) return;
            logged[0] = true;

            for (int r = 0; r < board.length; r++) {
                for (int c = 0; c < board[0].length; c++) {
                    Square sq = board[r][c];
                    Log.d("INIT_BOARD", String.format(
                            "[%d,%d] = %s_%s",
                            r, c,
                            sq.getColor(),
                            sq.getShape()
                    ));
                }
            }
        });

        boolean isLandscape = getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE;


//        // 3) Build UI: MazeView + controls
////        FrameLayout root = new FrameLayout(this);
//        LinearLayout root = new LinearLayout(this);
//        root.setOrientation(LinearLayout.VERTICAL); // vertical only ??
//
//        mazeView = new MazeView(this);
//        root.addView(mazeView, new FrameLayout.LayoutParams(
//                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//
//        Bitmap eyeballBmp = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
//        mazeView.setEyeballBitmap(eyeballBmp);
//        mazeView.setOnCellTapListener((row, col) -> viewModel.clickToMoveToward(row, col));
//
//        LinearLayout controls = new LinearLayout(this);
//        controls.setOrientation(LinearLayout.HORIZONTAL);
//        FrameLayout.LayoutParams ctrlParams = new FrameLayout.LayoutParams(
//                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
//        );
//        ctrlParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
//        controls.setGravity(Gravity.CENTER);

        // 3) Build UI: MazeView + controls
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

// MazeView setup
        mazeView = new MazeView(this);
        Bitmap eyeballBmp = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
        mazeView.setEyeballBitmap(eyeballBmp);
        mazeView.setOnCellTapListener((row, col) -> viewModel.clickToMoveToward(row, col));

        LinearLayout.LayoutParams mazeParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : 0,
                0.8f  // weight for maze view (80%)
        );
        root.addView(mazeView, mazeParams);

// Controls layout
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(isLandscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(16, 16, 16, 16);

        Spinner levelSpinner = new Spinner(this);
        controls.addView(levelSpinner);

        Button resetBtn = new Button(this);
        resetBtn.setText(getString(R.string.reset));
        resetBtn.setOnClickListener(v -> viewModel.resetMaze());
        controls.addView(resetBtn);

// Add controls to root
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                isLandscape ? 0 : LayoutParams.MATCH_PARENT,
                isLandscape ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT,
                0.2f  // weight for controls (20%)
        );
        root.addView(controls, controlsParams);

// Build level titles like "Level 1", "Level 2", ...
        int levelCount = game.getLevelCount();
        List<String> levelLabels = new ArrayList<>();
        for (int i = 0; i < levelCount; i++) {
            levelLabels.add("Level " + (i + 1));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, levelLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);

// When user selects a level
        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstTime = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstTime) {
                    firstTime = false;
                    return; // ignore initial trigger
                }

                game.setLevel(position);
                viewModel.syncGameState(); // method in your ViewModel to update UI
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

// Set content view

//        Button up    = new Button(this); up.setText("↑");
//        Button down  = new Button(this); down.setText("↓");
//        Button left  = new Button(this); left.setText("←");
//        Button right = new Button(this); right.setText("→");
//
//        up.setOnClickListener(v    -> viewModel.moveEyeball(Direction.UP));
//        down.setOnClickListener(v  -> viewModel.moveEyeball(Direction.DOWN));
//        left.setOnClickListener(v  -> viewModel.moveEyeball(Direction.LEFT));
//        right.setOnClickListener(v -> viewModel.moveEyeball(Direction.RIGHT));
//
//        ImageButton moveBtn = new ImageButton(this);
//        moveBtn.setImageResource(R.drawable.eyeball); // actual image
//        moveBtn.setBackground(null);
//        moveBtn.setOnClickListener(v -> viewModel.clickToMoveInDirection());
//
//        controls.addView(left);
//        controls.addView(up);
//        controls.addView(down);
//        controls.addView(right);
//        root.addView(controls, ctrlParams);

//        Button resetBtn = new Button(this);
//        resetBtn.setText(getString(R.string.reset));
//        resetBtn.setOnClickListener(v -> viewModel.resetMaze());
//        controls.addView(resetBtn);
//        root.addView(controls, ctrlParams); // Adds all controls to view

        setContentView(root);

        // 4) Observe LiveData and push into MazeView

        // Bind LiveData to MazeView
        viewModel.getBoard().observe(this, board -> {
            mazeView.setBoard(board);
            mazeView.invalidate();
        });

        // Eyeball row + col
        viewModel.getEyeballRow().observe(this, row -> {
            Integer col = viewModel.getEyeballCol().getValue();
            if (col != null) {
                mazeView.setEyeballPosition(row, col);
                mazeView.invalidate();
            }
        });

        viewModel.getEyeballCol().observe(this, col -> {
            Integer row = viewModel.getEyeballRow().getValue();
            if (row != null) {
                mazeView.setEyeballPosition(row, col);
                mazeView.invalidate();
            }
        });

        viewModel.getEyeballDir().observe(this, dir -> {
            mazeView.setDirection(dir); // this is the new method
        });

        // Current‐goal highlight
        viewModel.isCurrentGoal().observe(this, isGoal -> {
            mazeView.setCurrentSquareIsGoal(isGoal);
            mazeView.invalidate();
        });

//        viewModel.getMoveStatus().observe(this, message -> {
//            if (message != null && message != Message.OK) {
//                Toast.makeText(this, "Move blocked: " + message.name(), Toast.LENGTH_SHORT).show();
//                viewModel.clearMoveStatus(); // avoid repeated messages
//            }
//        });

        viewModel.getMoveStatus().observe(this, message -> {
            if (message != null && message != Message.OK) {
                Snackbar.make(root, "Move blocked: " + message.name(), Snackbar.LENGTH_SHORT).show();
                viewModel.clearMoveStatus();
            }
        });
    }

    private String[] loadLevelFromRaw(Context ctx, int resId) {
        List<String> lines = new ArrayList<>();
        try (InputStream is = ctx.getResources().openRawResource(resId);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines.toArray(new String[0]);
    }
}
