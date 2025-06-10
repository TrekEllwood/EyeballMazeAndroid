package nz.ac.ara.tre46.eyeballmaze.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.HashSet;
import java.util.Set;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.R;

public class MazeView extends View {
    private int eyeballRow = -1, eyeballCol = -1, boardRows = 0, boardCols = 0, failedRow = -1, failedCol = -1;
    private boolean currentGoal = false, isTouchEnabled = true;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint failedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Path reusablePath = new Path();
    private final RectF reusableGoalRect = new RectF();
    private final Set<String> goalKeys = new HashSet<>();
    private final Matrix matrix = new Matrix();
    private float cellW, cellH;
    private Runnable clearFailedRunnable;
    private Bitmap eyeballBitmap = null, goalLabelIcon;
    private OnCellTapListener tapListener;
    private ColorProvider colorProvider;
    private ShapeProvider shapeProvider;

    public MazeView(Context context) {
        super(context);
        init();
    }

    public MazeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MazeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.baseline_flag_24);
        if (drawable != null) {
            drawable.setTint(android.graphics.Color.BLACK); // Force black for MazeView
            Bitmap bmp = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bmp);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            goalLabelIcon = bmp;
        }

        setContentDescription(getContext().getString(R.string.goal));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4);

        fillPaint.setStyle(Paint.Style.FILL);

        goalTextPaint.setColor(android.graphics.Color.BLACK);
        goalTextPaint.setTextSize(32);
        goalTextPaint.setTextAlign(Paint.Align.LEFT);

        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(8);
        goalPaint.setColor(android.graphics.Color.YELLOW);

        failedPaint.setColor(android.graphics.Color.RED);
        failedPaint.setAlpha(150); // semi-transparent
        failedPaint.setStyle(Paint.Style.FILL);

        eyeballBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
    }

    public void setBoardSize(int rows, int cols) {
        this.boardRows = rows;
        this.boardCols = cols;
        invalidate();
    }


    /**
     * Called by Activity when the ViewModel’s eyeball‐row/col change
     */
    public void setEyeballPosition(int row, int col) {
        if (this.eyeballRow != row || this.eyeballCol != col) {
            int prevRow = this.eyeballRow;
            int prevCol = this.eyeballCol;

            this.eyeballRow = row;
            this.eyeballCol = col;

            invalidateCell(prevRow, prevCol);
            invalidateCell(row, col);
        }
    }

    /**
     * Called by Activity when the ViewModel’s goal‐flag changes
     */
    public void setCurrentSquareIsGoal(boolean isGoal) {
        this.currentGoal = isGoal;
        invalidateCell(eyeballRow, eyeballCol);
    }

    public void setGoalPositions(Set<Point> goals) {
        goalKeys.clear();
        for (Point p : goals) {
            goalKeys.add(p.y + "," + p.x);
            invalidateCell(p.y, p.x);
        }
    }

    public void setEyeballBitmap(Bitmap bmp) {
        this.eyeballBitmap = bmp;
    }

    public void setOnCellTapListener(OnCellTapListener listener) {
        this.tapListener = listener;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (boardRows == 0 || boardCols == 0 || colorProvider == null || shapeProvider == null)
            return;

        int rows = boardRows;
        int cols = boardCols;

        float rawCellW = (float) getWidth() / cols;
        float rawCellH = (float) getHeight() / rows;
        float squareSize = Math.min(rawCellW, rawCellH);
        cellW = cellH = squareSize;

        float offsetX = (getWidth() - cols * cellW) / 2f;
        float offsetY = (getHeight() - rows * cellH) / 2f;

        // draw grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float left = offsetX + c * cellW;
                float top = offsetY + r * cellH;
                float right = left + cellW;
                float bottom = top + cellH;

                if (colorProvider != null) {
                    Color color = colorProvider.getColor(r, c);
                    fillPaint.setColor(toAndroidColor(color));
                    canvas.drawRect(left, top, right, bottom, fillPaint);
                }

                if (shapeProvider != null) {
                    Shape shape = shapeProvider.getShape(r, c);
                    drawShape(canvas, shape, left, top, cellW, cellH);
                }

                // cell border
                canvas.drawRect(left, top, right, bottom, linePaint);

                // If this cell is a goal
                String key = r + "," + c;
                if (goalKeys.contains(key)) {
                    goalTextPaint.setColor(android.graphics.Color.WHITE);
                    goalTextPaint.setTextAlign(Paint.Align.CENTER);
                    goalTextPaint.setTextSize(cellH * 0.3f); // Adjust size per cell

                    if (goalLabelIcon != null) {
                        float iconSize = cellH * 0.3f;
                        float iconLeft = left + cellW * 0.02f;
                        float iconTop = top + cellH * 0.02f;
                        reusableGoalRect.set(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
                        canvas.drawBitmap(goalLabelIcon, null, reusableGoalRect, null);
//                        canvas.drawText("Goal", cx, cy + iconSize / 2f + 12, goalTextPaint); // Add the text
                    }

                }
            }
        }

        // highlight goal cell
        if (currentGoal && eyeballRow >= 0 && eyeballCol >= 0) {
            float left = offsetX + eyeballCol * cellW;
            float top = offsetY + eyeballRow * cellH;
            float right = left + cellW;
            float bottom = top + cellH;
            canvas.drawRect(left, top, right, bottom, goalPaint);
        }

        if (eyeballRow >= 0 && eyeballCol >= 0 && eyeballBitmap != null) {
            float cx = offsetX + eyeballCol * cellW + cellW / 2f;
            float cy = offsetY + eyeballRow * cellH + cellH / 2f;
            float size = Math.min(cellW, cellH) * 0.8f;

            matrix.reset();

            // Center the image
            matrix.postTranslate(-eyeballBitmap.getWidth() / 2f, -eyeballBitmap.getHeight() / 2f);

            // Rotate around center
            float angle = switch (currentDirection) {
                case UP -> 270;
                case DOWN -> 90;
                case LEFT -> 180;
                default -> 0;
            };
            matrix.postRotate(angle);

            // Scale to cell size and translate to position
            float scale = size / eyeballBitmap.getWidth();
            matrix.postScale(scale, scale);
            matrix.postTranslate(cx, cy);

            canvas.drawBitmap(eyeballBitmap, matrix, null);
        }

        // Draw failed move marker
        if (failedRow >= 0 && failedCol >= 0) {
            float cx = offsetX + failedCol * cellW + cellW / 2f;
            float cy = offsetY + failedRow * cellH + cellH / 2f;
            float radius = Math.min(cellW, cellH) * 0.4f;

            canvas.drawCircle(cx, cy, radius, failedPaint);
        }
    }

    private int toAndroidColor(Color c) {
        return switch (c) {
            case RED -> ContextCompat.getColor(getContext(), R.color.soft_red);
            case GREEN -> ContextCompat.getColor(getContext(), R.color.soft_green);
            case BLUE -> ContextCompat.getColor(getContext(), R.color.soft_blue);
            case YELLOW -> ContextCompat.getColor(getContext(), R.color.soft_yellow);
            case PURPLE -> ContextCompat.getColor(getContext(), R.color.soft_purple);
            default -> android.graphics.Color.WHITE;
        };
    }

    private Direction currentDirection = Direction.RIGHT;

    public void setDirection(Direction d) {
        this.currentDirection = d;
        invalidateCell(eyeballRow, eyeballCol);
    }

    private void drawShape(Canvas canvas, Shape shape,
                           float left, float top,
                           float w, float h) {
        float cx = left + w / 2, cy = top + h / 2;
        float size = Math.min(w, h) * 0.4f;

        reusablePath.reset();

        switch (shape) {
            case DIAMOND -> {
                reusablePath.moveTo(cx, cy - size);
                reusablePath.lineTo(cx - size, cy);
                reusablePath.lineTo(cx, cy + size);
                reusablePath.lineTo(cx + size, cy);
                reusablePath.close();
                canvas.drawPath(reusablePath, linePaint);
            }
            case CROSS -> {
                canvas.drawLine(cx - size, cy, cx + size, cy, linePaint);
                canvas.drawLine(cx, cy - size, cx, cy + size, linePaint);
            }
            case STAR -> {
                for (int i = 0; i < 5; i++) {
                    double a1 = Math.toRadians(-90 + 72 * i);
                    double a2 = Math.toRadians(-90 + 72 * i + 36);
                    float x1 = cx + size * (float) Math.cos(a1);
                    float y1 = cy + size * (float) Math.sin(a1);
                    float x2 = cx + size / 2 * (float) Math.cos(a2);
                    float y2 = cy + size / 2 * (float) Math.sin(a2);
                    if (i == 0) reusablePath.moveTo(x1, y1);
                    else reusablePath.lineTo(x1, y1);
                    reusablePath.lineTo(x2, y2);
                }
                reusablePath.close();
                canvas.drawPath(reusablePath, linePaint);
            }
            case FLOWER -> {
                float petalRadius = size / 3f;
                float petalDistance = size / 2f;

                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60);
                    float x = cx + petalDistance * (float) Math.cos(angle);
                    float y = cy + petalDistance * (float) Math.sin(angle);
                    canvas.drawCircle(x, y, petalRadius, linePaint);
                }

                canvas.drawCircle(cx, cy, size / 5f, linePaint);
            }
            case LIGHTNING -> {
                reusablePath.moveTo(cx - size / 2, cy - size);
                reusablePath.lineTo(cx, cy - size / 3);
                reusablePath.lineTo(cx - size / 4, cy - size / 3);
                reusablePath.lineTo(cx + size / 2, cy + size);
                canvas.drawPath(reusablePath, linePaint);
            }
            case BLANK -> {
                // nothing
            }
        }
    }

    public void setTouchEnabled(boolean enabled) {
        isTouchEnabled = enabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouchEnabled) return false;

        if (boardRows == 0 || boardCols == 0 || tapListener == null) return false;
        if (event.getAction() != MotionEvent.ACTION_DOWN) return false;

        int rows = boardRows;
        int cols = boardCols;

        // Recalculate square cell size
        float rawCellW = (float) getWidth() / cols;
        float rawCellH = (float) getHeight() / rows;
        float squareSize = Math.min(rawCellW, rawCellH);
        cellW = cellH = squareSize;

        // Offset to center the grid
        float offsetX = (getWidth() - cols * cellW) / 2f;
        float offsetY = (getHeight() - rows * cellH) / 2f;

        // Adjust for offset
        float touchX = event.getX() - offsetX;
        float touchY = event.getY() - offsetY;

        int col = (int) (touchX / cellW);
        int row = (int) (touchY / cellH);

        if (col < 0 || col >= cols || row < 0 || row >= rows) return false;

        tapListener.onCellTapped(row, col);
        performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public interface OnCellTapListener {
        void onCellTapped(int row, int col);
    }

    public interface ColorProvider {
        Color getColor(int row, int col);
    }

    public interface ShapeProvider {
        Shape getShape(int row, int col);
    }

    public void setColorProvider(ColorProvider provider) {
        this.colorProvider = provider;
    }

    public void setShapeProvider(ShapeProvider provider) {
        this.shapeProvider = provider;
    }

    public void setFailedMoveAt(int row, int col) {
        this.failedRow = row;
        this.failedCol = col;
        invalidateCell(failedRow, failedCol);

        if (clearFailedRunnable != null) {
            handler.removeCallbacks(clearFailedRunnable);
        }

        clearFailedRunnable = this::clearFailedMove;
        handler.postDelayed(clearFailedRunnable, 500);
    }

    public void clearFailedMove() {
        this.failedRow = -1;
        this.failedCol = -1;
        invalidateCell(failedRow, failedCol);
    }

    private void invalidateCell(int row, int col) {
        if (row < 0 || col < 0 || cellW == 0 || cellH == 0) return;

        float offsetX = (getWidth() - boardCols * cellW) / 2f;
        float offsetY = (getHeight() - boardRows * cellH) / 2f;

        float left = offsetX + col * cellW;
        float top = offsetY + row * cellH;
        float right = left + cellW;
        float bottom = top + cellH;

        postInvalidateOnAnimation(
                (int) left,
                (int) top,
                (int) right,
                (int) bottom
        );
    }
}
