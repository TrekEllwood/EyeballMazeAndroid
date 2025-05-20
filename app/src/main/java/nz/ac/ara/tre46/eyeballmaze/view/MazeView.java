package nz.ac.ara.tre46.eyeballmaze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import nz.ac.ara.tre46.eyeballmaze.enums.Color;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.models.Square;

public class MazeView extends View {
    private Square[][] board;
    private int eyeballRow = -1;
    private int eyeballCol = -1;
    private boolean currentGoal = false;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4);

        fillPaint.setStyle(Paint.Style.FILL);

        eyeballPaint.setStyle(Paint.Style.FILL);
        eyeballPaint.setColor(android.graphics.Color.BLACK);

        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(8);
        goalPaint.setColor(android.graphics.Color.YELLOW);
    }

    /** Called by your Activity when the ViewModel’s board LiveData changes */
    public void setBoard(Square[][] board) {
        this.board = board;
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (board == null) return;

        int rows = board.length;
        int cols = board[0].length;
        float cellW = (float) getWidth() / cols;
        float cellH = (float) getHeight() / rows;

        // draw grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float left = c * cellW;
                float top = r * cellH;
                float right = left + cellW;
                float bottom = top + cellH;

                // background color
                Square sq = board[r][c];
                fillPaint.setColor(toAndroidColor(sq.getColor()));
                canvas.drawRect(left, top, right, bottom, fillPaint);

                // shape
                drawShape(canvas, sq.getShape(), left, top, cellW, cellH);

                // cell border
                canvas.drawRect(left, top, right, bottom, linePaint);
            }
        }

        // highlight goal cell
        if (currentGoal && eyeballRow >= 0 && eyeballCol >= 0) {
            float left = eyeballCol * cellW;
            float top = eyeballRow * cellH;
            float right = left + cellW;
            float bottom = top + cellH;
            canvas.drawRect(left, top, right, bottom, goalPaint);
        }

        // draw eyeball
        if (eyeballRow >= 0 && eyeballCol >= 0) {
            float cx = eyeballCol * cellW + cellW/2;
            float cy = eyeballRow * cellH + cellH/2;
            float radius = Math.min(cellW, cellH) * 0.3f;
            canvas.drawCircle(cx, cy, radius, eyeballPaint);
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

    private void drawShape(Canvas canvas, Shape shape,
                           float left, float top,
                           float w, float h) {
        float cx = left + w/2, cy = top + h/2;
        float size = Math.min(w, h) * 0.4f;
        Path p = new Path();

        switch (shape) {
            case DIAMOND -> {
                p.moveTo(cx, cy - size);
                p.lineTo(cx - size, cy);
                p.lineTo(cx, cy + size);
                p.lineTo(cx + size, cy);
                p.close();
                canvas.drawPath(p, linePaint);
            }
            case CROSS -> {
                canvas.drawLine(cx-size, cy-size, cx+size, cy+size, linePaint);
                canvas.drawLine(cx-size, cy+size, cx+size, cy-size, linePaint);
            }
            case STAR -> {
                for (int i = 0; i < 5; i++) {
                    double a1 = Math.toRadians(-90 + 72*i);
                    double a2 = Math.toRadians(-90 + 72*i + 36);
                    float x1 = cx + size * (float)Math.cos(a1);
                    float y1 = cy + size * (float)Math.sin(a1);
                    float x2 = cx + size/2 * (float)Math.cos(a2);
                    float y2 = cy + size/2 * (float)Math.sin(a2);
                    if (i == 0) p.moveTo(x1, y1);
                    else p.lineTo(x1, y1);
                    p.lineTo(x2, y2);
                }
                p.close();
                canvas.drawPath(p, linePaint);
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
                p.moveTo(cx-size/2, cy-size);
                p.lineTo(cx, cy-size/3);
                p.lineTo(cx-size/4, cy-size/3);
                p.lineTo(cx+size/2, cy+size);
                canvas.drawPath(p, linePaint);
            }
            case BLANK -> {
                // nothing
            }
        }
    }
}
