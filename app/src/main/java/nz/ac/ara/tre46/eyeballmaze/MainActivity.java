package nz.ac.ara.tre46.eyeballmaze;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
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
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;
import nz.ac.ara.tre46.eyeballmaze.interfaces.ILevelHolder;
import nz.ac.ara.tre46.eyeballmaze.models.Game;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModel;
import nz.ac.ara.tre46.eyeballmaze.viewmodel.EyeballMazeViewModelFactory;
import nz.ac.ara.tre46.eyeballmaze.view.MazeView;

public class MainActivity extends AppCompatActivity {
    private EyeballMazeViewModel viewModel;
    private MazeView mazeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Instantiate your concrete Game (implements both IGame & ILevelHolder)
        Game game = new Game();
//        if (game instanceof ILevelHolder) {
//            ILevelHolder levels = (ILevelHolder) game;
//            levels.addLevel(5, 5);
//            levels.setLevel(levels.getLevelCount() - 1);
//        }
        String[] levelLines = loadLevelFromRaw(this, R.raw.levels);
        game.loadLevelFromText(levelLines);

        // 2) Wrap it in the ViewModel via the Factory
//        IGame gameInterface = game;
        viewModel = new ViewModelProvider(
                this,
//                new EyeballMazeViewModelFactory(gameInterface)
                new EyeballMazeViewModelFactory((IGame) game)
        ).get(EyeballMazeViewModel.class);

        // 3) Build UI: full-screen MazeView + directional buttons
        FrameLayout root = new FrameLayout(this);
        mazeView = new MazeView(this);
        root.addView(
                mazeView,
                new FrameLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                )
        );

        // 4) Directional controls
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams ctrlParams = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        ctrlParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        controls.setGravity(Gravity.CENTER);

        Button up    = new Button(this); up.setText("↑");
        Button down  = new Button(this); down.setText("↓");
        Button left  = new Button(this); left.setText("←");
        Button right = new Button(this); right.setText("→");

        // 4) Hook up explicit listeners
        up.setOnClickListener(v    -> viewModel.moveEyeball(Direction.UP));
        down.setOnClickListener(v  -> viewModel.moveEyeball(Direction.DOWN));
        left.setOnClickListener(v  -> viewModel.moveEyeball(Direction.LEFT));
        right.setOnClickListener(v -> viewModel.moveEyeball(Direction.RIGHT));

        controls.addView(left);
        controls.addView(up);
        controls.addView(down);
        controls.addView(right);
        root.addView(controls, ctrlParams);

        setContentView(root);

        // 5) Observe LiveData and update MazeView

        // Board
        viewModel.getBoard().observe(this, board -> {
            mazeView.setBoard(board);
            mazeView.invalidate();
        });

        // Eyeball row & col
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

        // Current-goal highlight
        viewModel.isCurrentGoal().observe(this, isGoal -> {
            mazeView.setCurrentSquareIsGoal(isGoal);
            mazeView.invalidate();
        });
    }

    /**
     * Reads a raw‐resource text file, strips out comments and blank lines,
     * and returns the remaining lines as a String[].
     */
    private String[] loadLevelFromRaw(Context ctx, int resId) {
        List<String> lines = new ArrayList<>();
        try (InputStream is = ctx.getResources().openRawResource(resId);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String l;
            while ((l = br.readLine()) != null) {
                l = l.trim();
                if (l.isEmpty() || l.startsWith("#")) continue;
                lines.add(l);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines.toArray(new String[0]);
    }
}
