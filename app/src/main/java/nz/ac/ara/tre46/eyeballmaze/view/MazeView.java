package nz.ac.ara.tre46.eyeballmaze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import nz.ac.ara.tre46.eyeballmaze.models.Square;
import nz.ac.ara.tre46.eyeballmaze.enums.Shape;
import nz.ac.ara.tre46.eyeballmaze.enums.Color;

public class MazeView extends View {
    private Square[][] board;
    private int eyeballRow = -1;
    private int eyeballCol = -1;
    private boolean currentGoal = false;

    private Paint linePaint;
    private Paint fillPaint;
    private Paint eyeballPaint;
    private Paint goalPaint;

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
        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);

        eyeballPaint = new Paint();
        eyeballPaint.setStyle(Paint.Style.FILL);
        eyeballPaint.setColor(android.graphics.Color.BLACK);

        goalPaint = new Paint();
        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(8);
        goalPaint.setColor(android.graphics.Color.YELLOW);
    }

    public void setBoard(Square[][] board) {
        this.board = board;
        invalidate();
    }

    public void setEyeballPosition(int row, int col) {
        this.eyeballRow = row;
        this.eyeballCol = col;
        invalidate();
    }

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
        float cellWidth = (float) getWidth() / cols;
        float cellHeight = (float) getHeight() / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float left = c * cellWidth;
                float top = r * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;

                // Draw cell background
                Square sq = board[r][c];
                fillPaint.setColor(getAndroidColor(sq.getColor()));
                canvas.drawRect(left, top, right, bottom, fillPaint);

                // Draw shape in cell
                drawShape(canvas, sq.getShape(), left, top, cellWidth, cellHeight);

                // Draw cell border
                canvas.drawRect(left, top, right, bottom, linePaint);
            }
        }

        // Highlight current square if it's a goal
        if (currentGoal && eyeballRow >= 0 && eyeballCol >= 0) {
            float left = eyeballCol * cellWidth;
            float top = eyeballRow * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;
            canvas.drawRect(left, top, right, bottom, goalPaint);
        }

        // Draw the eyeball
        if (eyeballRow >= 0 && eyeballCol >= 0) {
            float cx = eyeballCol * cellWidth + cellWidth / 2;
            float cy = eyeballRow * cellHeight + cellHeight / 2;
            float radius = Math.min(cellWidth, cellHeight) * 0.3f;
            canvas.drawCircle(cx, cy, radius, eyeballPaint);
        }
    }

    private int getAndroidColor(Color mazeColor) {
        return switch (mazeColor) {
            case RED -> android.graphics.Color.RED;
            case GREEN -> android.graphics.Color.GREEN;
            case BLUE -> android.graphics.Color.BLUE;
            case YELLOW -> android.graphics.Color.YELLOW;
            case PURPLE -> android.graphics.Color.MAGENTA;
            case BLANK -> android.graphics.Color.LTGRAY;
            default -> android.graphics.Color.LTGRAY;
        };
    }

    private void drawShape(Canvas canvas, Shape shape,
                           float left, float top,
                           float width, float height) {
        float cx = left + width / 2;
        float cy = top + height / 2;
        float size = Math.min(width, height) * 0.4f;
        Path path = new Path();

        switch (shape) {
            case DIAMOND:
                path.moveTo(cx, cy - size);
                path.lineTo(cx - size, cy);
                path.lineTo(cx, cy + size);
                path.lineTo(cx + size, cy);
                path.close();
                canvas.drawPath(path, linePaint);
                break;
            case CROSS:
                canvas.drawLine(cx - size, cy - size, cx + size, cy + size, linePaint);
                canvas.drawLine(cx - size, cy + size, cx + size, cy - size, linePaint);
                break;
            case STAR:
                for (int i = 0; i < 5; i++) {
                    double angle = Math.toRadians(-90 + i * 72);
                    double angle2 = Math.toRadians(-90 + i * 72 + 36);
                    float x1 = cx + (float)(size * Math.cos(angle));
                    float y1 = cy + (float)(size * Math.sin(angle));
                    float x2 = cx + (float)(size/2 * Math.cos(angle2));
                    float y2 = cy + (float)(size/2 * Math.sin(angle2));
                    if (i == 0) path.moveTo(x1, y1);
                    else path.lineTo(x1, y1);
                    path.lineTo(x2, y2);
                }
                path.close();
                canvas.drawPath(path, linePaint);
                break;
            case FLOWER:
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60);
                    float x = cx + (float)(size * Math.cos(angle));
                    float y = cy + (float)(size * Math.sin(angle));
                    canvas.drawCircle(x, y, size/3, linePaint);
                }
                break;
            case LIGHTNING:
                path.moveTo(cx - size/2, cy - size);
                path.lineTo(cx, cy - size/3);
                path.lineTo(cx - size/4, cy - size/3);
                path.lineTo(cx + size/2, cy + size);
                canvas.drawPath(path, linePaint);
                break;
            case BLANK:
                // no shape
                break;
        }
    }
}
