package nz.ac.ara.tre46.eyeballmaze;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.models.Game;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Create the Game and pre-configure the board size (safe default)
        Game game = new Game();
        game.addLevel(10, 10); // Size could be dynamically detected later

        // 2. Prepare the ViewModel with the ready Game
        EyeballMazeViewModelFactory factory = new EyeballMazeViewModelFactory(game);
        viewModel = new ViewModelProvider(this, factory).get(EyeballMazeViewModel.class);

        // 3. Load level file content into the already-prepared Game/ViewModel
        String[] levelRows = loadLevelFromRaw(this, R.raw.level01);
        viewModel.loadLevelFromText(levelRows, 0, 0, "r");

        // 4. Setup the UI
        setupUI();
    }

    private void setupUI() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mazeView = new MazeView(this);
        root.addView(mazeView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout controls = new LinearLayout(this);
        FrameLayout.LayoutParams ctrlParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ctrlParams.gravity = Gravity.BOTTOM;
        controls.setLayoutParams(ctrlParams);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button left  = new Button(this); left.setText("←");
        Button up    = new Button(this); up.setText("↑");
        Button down  = new Button(this); down.setText("↓");
        Button right = new Button(this); right.setText("→");

        controls.addView(left);
        controls.addView(up);
        controls.addView(down);
        controls.addView(right);
        root.addView(controls);

        setContentView(root);

        viewModel.getBoard().observe(this, board -> {
            if (board == null || board.length == 0 || board[0] == null) return;
            boolean[][] goalMap = new boolean[board.length][board[0].length];
            for (int r = 0; r < board.length; r++)
                for (int c = 0; c < board[0].length; c++)
                    goalMap[r][c] = viewModel.hasGoalAt(r, c);
            mazeView.setGoalMap(goalMap);
        });
        viewModel.getEyeballRow().observe(this, r -> {
            Integer c = viewModel.getEyeballCol().getValue();
            if (c != null) mazeView.setEyeballPosition(r, c);
        });
        viewModel.getEyeballCol().observe(this, c -> {
            Integer r = viewModel.getEyeballRow().getValue();
            if (r != null) mazeView.setEyeballPosition(r, c);
        });

        up.setOnClickListener(v -> viewModel.moveEyeball(Direction.UP));
        down.setOnClickListener(v -> viewModel.moveEyeball(Direction.DOWN));
        left.setOnClickListener(v -> viewModel.moveEyeball(Direction.LEFT));
        right.setOnClickListener(v -> viewModel.moveEyeball(Direction.RIGHT));
    }

    private String[] loadLevelFromRaw(Context context, int resId) {
        try (InputStream is = context.getResources().openRawResource(resId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
            return lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private static class MazeView extends View {
        private boolean[][] goalMap;
        private int eyeballRow, eyeballCol;
        private final Paint paintGrid = new Paint();
        private final Paint paintGoal = new Paint();
        private final Paint paintEye  = new Paint();

        public MazeView(Context ctx) {
            super(ctx);
            paintGrid.setColor(android.graphics.Color.BLACK);
            paintGrid.setStyle(Paint.Style.STROKE);
            paintGoal.setColor(android.graphics.Color.RED);
            paintEye .setColor(android.graphics.Color.BLUE);
        }

        public void setGoalMap(boolean[][] goalMap) {
            this.goalMap = goalMap;
            invalidate();
        }

        public void setEyeballPosition(int row, int col) {
            this.eyeballRow = row;
            this.eyeballCol = col;
            invalidate();
        }

//        @Override
//        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
//            if (goalMap == null) return;
//            int rows = goalMap.length, cols = goalMap[0].length;
//            int w = getWidth(), h = getHeight();
//            int cellSize = Math.min(w / cols, h / rows);
//            for (int r = 0; r <= rows; r++) canvas.drawLine(0, r * cellSize, cols * cellSize, r * cellSize, paintGrid);
//            for (int c = 0; c <= cols; c++) canvas.drawLine(c * cellSize, 0, c * cellSize, rows * cellSize, paintGrid);
//            float gr = cellSize * 0.2f;
//            for (int r = 0; r < rows; r++)
//                for (int c = 0; c < cols; c++)
//                    if (goalMap[r][c]) canvas.drawCircle((c + 0.5f) * cellSize, (r + 0.5f) * cellSize, gr, paintGoal);
//            float er = cellSize * 0.4f;
//            canvas.drawCircle((eyeballCol + 0.5f) * cellSize, (eyeballRow + 0.5f) * cellSize, er, paintEye);
//        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (goalMap == null) return;

            int rows = goalMap.length, cols = goalMap[0].length;
            int w = getWidth(), h = getHeight();
            int cellSize = Math.min(w / cols, h / rows);

            // Centering offsets
            int offsetX = (w - (cols * cellSize)) / 2;
            int offsetY = (h - (rows * cellSize)) / 2;

            // Draw grid lines
            for (int r = 0; r <= rows; r++)
                canvas.drawLine(offsetX, offsetY + r * cellSize, offsetX + cols * cellSize, offsetY + r * cellSize, paintGrid);

            for (int c = 0; c <= cols; c++)
                canvas.drawLine(offsetX + c * cellSize, offsetY, offsetX + c * cellSize, offsetY + rows * cellSize, paintGrid);

            // Draw goals
            float gr = cellSize * 0.2f;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (goalMap[r][c]) {
                        float x = offsetX + (c + 0.5f) * cellSize;
                        float y = offsetY + (r + 0.5f) * cellSize;
                        canvas.drawCircle(x, y, gr, paintGoal);
                    }
                }
            }

            // Draw eyeball
            float er = cellSize * 0.4f;
            float ex = offsetX + (eyeballCol + 0.5f) * cellSize;
            float ey = offsetY + (eyeballRow + 0.5f) * cellSize;
            canvas.drawCircle(ex, ey, er, paintEye);
        }
    }
}
