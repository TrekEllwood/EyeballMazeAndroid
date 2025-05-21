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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.SquareType;
import nz.ac.ara.tre46.eyeballmaze.enums.Direction;
import nz.ac.ara.tre46.eyeballmaze.R;

public class MazeView extends View {
    private int eyeballRow = -1;
    private int eyeballCol = -1;
    private boolean currentGoal = false;
    private int boardRows = 0;
    private int boardCols = 0;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path reusablePath = new Path();
    private final RectF reusableRectF = new RectF();

    private final Set<String> goalKeys = new HashSet<>();
    private Bitmap eyeballBitmap = null;
    private final Matrix matrix = new Matrix();

    private float cellW, cellH;
    private OnCellTapListener tapListener;

    private TypeProvider typeProvider;
    private ColorProvider colorProvider;
    private ShapeProvider shapeProvider;

    private String goalLabel;

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
        goalLabel = getResources().getString(R.string.goal);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4);

        fillPaint.setStyle(Paint.Style.FILL);

        goalTextPaint.setColor(android.graphics.Color.BLACK);
        goalTextPaint.setTextSize(32);
        goalTextPaint.setTextAlign(Paint.Align.LEFT);

        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(8);
        goalPaint.setColor(android.graphics.Color.YELLOW);

        eyeballBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
    }

    public void setBoardSize(int rows, int cols) {
        this.boardRows = rows;
        this.boardCols = cols;
        invalidate();
    }


    /** Called by your Activity when the ViewModel’s eyeball‐row/col change */
    public void setEyeballPosition(int row, int col) {
        this.eyeballRow = row;
        this.eyeballCol = col;
        invalidate();
    }

    /** Called by your Activity when the ViewModel’s goal‐flag changes */
    public void setCurrentSquareIsGoal(boolean isGoal) {
        this.currentGoal = isGoal;
        invalidate();
    }

    public void setGoalPositions(Set<Point> goals) {
        goalKeys.clear();
        for (Point p : goals) {
            goalKeys.add(p.y + "," + p.x); // row,col format
        }
        invalidate();
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

        if (boardRows == 0 || boardCols == 0 || colorProvider == null || shapeProvider == null) return;

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

                // If this cell is a goal, draw "Goal" text
                String key = r + "," + c;
                if (goalKeys.contains(key)) {
                    goalTextPaint.setColor(android.graphics.Color.WHITE);
                    goalTextPaint.setTextAlign(Paint.Align.CENTER);
                    goalTextPaint.setTextSize(cellH * 0.3f); // Adjust size per cell

                    Paint.FontMetrics fontMetrics = goalTextPaint.getFontMetrics() ;
                    float textOffset = (fontMetrics.ascent + fontMetrics.descent) / 2f;

                    float cx = left + cellW / 2f;
                    float cy = top + cellH / 2f;
                    canvas.drawText(goalLabel, cx, cy - textOffset, goalTextPaint);
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
    }

    private int toAndroidColor(Color c) {
        return switch (c) {
            case RED    -> android.graphics.Color.RED;
            case GREEN  -> android.graphics.Color.GREEN;
            case BLUE   -> android.graphics.Color.BLUE;
            case YELLOW -> android.graphics.Color.YELLOW;
            case PURPLE -> android.graphics.Color.MAGENTA;
            default     -> android.graphics.Color.WHITE;
        };
    }

    private Direction currentDirection = Direction.RIGHT;

    public void setDirection(Direction d) {
        this.currentDirection = d;
        invalidate();
    }

    private void drawShape(Canvas canvas, Shape shape,
                           float left, float top,
                           float w, float h) {
        float cx = left + w/2, cy = top + h/2;
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
                canvas.drawLine(cx-size, cy-size, cx+size, cy+size, linePaint);
                canvas.drawLine(cx-size, cy+size, cx+size, cy-size, linePaint);
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
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60);
                    float x = cx + size * (float)Math.cos(a);
                    float y = cy + size * (float)Math.sin(a);
                    canvas.drawCircle(x, y, size/3, linePaint);
                }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        if (board == null || tapListener == null) return false;
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
    public boolean performClick() { // accessibility services
        super.performClick();
        // Could do something here if needed for accessibility
        return true;
    }

    public interface OnCellTapListener {
        void onCellTapped(int row, int col);
    }

    public interface TypeProvider {
        SquareType getType(int row, int col);
    }

    public interface ColorProvider {
        Color getColor(int row, int col);
    }

    public interface ShapeProvider {
        Shape getShape(int row, int col);
    }

    public void setTypeProvider(TypeProvider provider) {
        this.typeProvider = provider;
    }

    public void setColorProvider(ColorProvider provider) {
        this.colorProvider = provider;
    }

    public void setShapeProvider(ShapeProvider provider) {
        this.shapeProvider = provider;
    }
}
