package nz.ac.ara.tre46.eyeballmaze.ui.components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;

import java.util.List;

import nz.ac.ara.tre46.eyeballmaze.R;

public class ControlPanelBuilder {
    public final TextView titleTextView, spinnerLabel, goalsStatusTextView, moveCounterTextView, timerTextView, leaderboardTextView;
    public final Button resetBtn, undoBtn, replayBtn;
    public final ImageButton pauseBtn, muteBtn, howToBtn;
    public final Spinner levelSpinner;
    public Drawable flagIcon;

    private final Context context;
    private View rootView;

    public ControlPanelBuilder(Context context) {
        this.context = context;

        resetBtn = createButton(R.string.reset);
        undoBtn = createButton(R.string.undo);
        replayBtn = createButton(R.string.replay);

        pauseBtn = createIconButton(R.drawable.baseline_pause_circle_24, R.string.pause);
        muteBtn = createIconButton(R.drawable.baseline_volume_up_24, R.string.mute);
        howToBtn = createIconButton(R.drawable.baseline_help_24, R.string.help);

        levelSpinner = new Spinner(context);
        spinnerLabel = new TextView(context);
        spinnerLabel.setText(context.getString(R.string.choose_maze));
        spinnerLabel.setTextSize(16f);
        spinnerLabel.setPadding(0, 0, 16, 0);

        goalsStatusTextView = createCenteredTextView();
        moveCounterTextView = createCenteredTextView();
        timerTextView = createCenteredTextView();
        leaderboardTextView = createLeaderboardTextView();
        titleTextView = new TextView(context);
        titleTextView.setTextSize(20f);
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        setupFlagIcon();
    }

    public LinearLayout buildSpinnerRow() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.addView(spinnerLabel);
        row.addView(levelSpinner);
        return row;
    }

    public View buildPortraitLayout(View spinnerRow) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = baseVerticalLayout();

        layout.addView(goalsStatusTextView);
        layout.addView(moveCounterTextView);
        layout.addView(timerTextView);
        layout.addView(leaderboardTextView);
        layout.addView(horizontalRow(resetBtn, undoBtn, replayBtn));
        layout.addView(horizontalRow(pauseBtn, muteBtn, howToBtn));
        layout.addView(spinnerRow);

        scrollView.addView(layout);
        this.rootView = scrollView;
        return scrollView;
    }

    public View buildLandscapeLayout(View spinnerRow) {
        LinearLayout layout = baseVerticalLayout();

        layout.addView(titleTextView);
        layout.addView(goalsStatusTextView);
        layout.addView(moveCounterTextView);
        layout.addView(timerTextView);
        layout.addView(leaderboardTextView);
        layout.addView(flexButtonRow(resetBtn, undoBtn, replayBtn));
        layout.addView(horizontalRow(pauseBtn, muteBtn, howToBtn));
        layout.addView(spinnerRow);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);

        FrameLayout wrapper = new FrameLayout(context);
        wrapper.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ViewCompat.setOnApplyWindowInsetsListener(wrapper, (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        FrameLayout.LayoutParams centered = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
        );

        wrapper.addView(scrollView, centered);
        this.rootView = wrapper;
        return wrapper;
    }

    private LinearLayout baseVerticalLayout() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        return layout;
    }

    private LinearLayout horizontalRow(View... views) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        for (View v : views) row.addView(v);
        return row;
    }

    private FlexboxLayout flexButtonRow(View... views) {
        FlexboxLayout row = new FlexboxLayout(context);
        row.setFlexDirection(FlexDirection.ROW);
        row.setFlexWrap(FlexWrap.WRAP);
        row.setJustifyContent(JustifyContent.CENTER);
        row.setPadding(0, 8, 0, 8);

        for (View v : views) {
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            v.setLayoutParams(params);
            row.addView(v);
        }

        return row;
    }

    private Button createButton(int textResId) {
        Button btn = new Button(context);
        btn.setText(context.getString(textResId));
        return btn;
    }

    private ImageButton createIconButton(int drawableResId, int descResId) {
        ImageButton btn = new ImageButton(context);
        btn.setImageResource(drawableResId);
        btn.setContentDescription(context.getString(descResId));
        btn.setBackground(null);
        btn.setColorFilter(ContextCompat.getColor(context, R.color.icon_tint), PorterDuff.Mode.SRC_IN);
        return btn;
    }

    private TextView createCenteredTextView() {
        TextView tv = new TextView(context);
        tv.setTextSize(16f);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        return tv;
    }

    private TextView createLeaderboardTextView() {
        TextView tv = new TextView(context);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setPadding(8, 4, 8, 4);
        return tv;
    }

    public void setLevelOptions(Context context, List<String> labels, int selectedIndex) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);
        levelSpinner.setSelection(selectedIndex);
    }

    private void setupFlagIcon() {
        flagIcon = AppCompatResources.getDrawable(context, R.drawable.baseline_flag_circle_24);
        if (flagIcon != null) {
            flagIcon.setTint(ContextCompat.getColor(context, R.color.icon_tint));
            flagIcon.setBounds(0, 0, flagIcon.getIntrinsicWidth(), flagIcon.getIntrinsicHeight());
            goalsStatusTextView.setCompoundDrawables(flagIcon, null, flagIcon, null);
        }
    }

    public View getRootView() {
        return rootView;
    }
}
