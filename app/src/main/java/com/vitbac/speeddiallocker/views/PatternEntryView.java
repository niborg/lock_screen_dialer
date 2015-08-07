package com.vitbac.speeddiallocker.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.vitbac.speeddiallocker.R;

/**
 * Created by nick on 8/5/15.
 */
public class PatternEntryView extends PasscodeEntryView implements View.OnTouchListener{

    private static final String TAG = "PatternEntryView";
    protected int mButtonMarkedColor;
    protected int mDrawColor;
    protected float mDrawWidth;
    protected int mAnimTime;

    private DrawView mPatternDrawView;
    private DrawView mTouchDrawView;

    private View mLastKey;
    private String mPatternEntered;

    public PatternEntryView (Context context) {
        super(context);
        init();
    }

    public PatternEntryView (Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributeArray = context.obtainStyledAttributes(attrs,
                R.styleable.PatternEntryView, 0, 0);

        mButtonMarkedColor = attributeArray.getInt(
                R.styleable.PatternEntryView_markedColor,
                getResources().getColor(R.color.default_pattern_marked_color)
        );
        mDrawColor = attributeArray.getColor(
                R.styleable.PatternEntryView_drawColor,
                getResources().getColor(R.color.default_pattern_draw_color)
        );
        mAnimTime = attributeArray.getInt(R.styleable.PatternEntryView_animTime, 500);
        mDrawWidth = attributeArray.getFloat(R.styleable.PatternEntryView_drawWidth, 5f);
        init();
        attributeArray.recycle();
    }

    private void init() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mButtonMarkedColor = prefs.getInt(
                getContext().getString(R.string.key_select_pattern_button_pressed_color),
                mButtonMarkedColor);
        mDrawColor = prefs.getInt(
                getContext().getString(R.string.key_select_pattern_draw_color),
                mDrawColor);

        mPatternDrawView = (DrawView) findViewById(R.id.pattern_canvas);
        mTouchDrawView = (DrawView) findViewById(R.id.touch_canvas);
    }

    protected int getLayout() {
        return R.layout.view_pattern_entry;
    }

    protected View[] initKeys() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mButtonMarkedColor = prefs.getInt(
                getContext().getString(R.string.key_select_pattern_button_pressed_color),
                getResources().getColor(R.color.default_pattern_marked_color));

        View[] views = new View[9];

        views[0] = findViewById(R.id.pattern_button_1);
        views[1] = findViewById(R.id.pattern_button_2);
        views[2] = findViewById(R.id.pattern_button_3);
        views[3] = findViewById(R.id.pattern_button_4);
        views[4] = findViewById(R.id.pattern_button_5);
        views[5] = findViewById(R.id.pattern_button_6);
        views[6] = findViewById(R.id.pattern_button_7);
        views[7] = findViewById(R.id.pattern_button_8);
        views[8] = findViewById(R.id.pattern_button_9);

        for (int i = 0; i < 9; i++) {
            views[i].setTag(new KeyNumber(i + 1));
            views[i].setOnTouchListener(this);

            View markerView = getCorrespondingMarkerView(views[i]);
            LayerDrawable layerList = (LayerDrawable) getResources()
                    .getDrawable(R.drawable.pattern_button_marked);
            GradientDrawable shape = (GradientDrawable) layerList
                    .findDrawableByLayerId(R.id.pattern_button_marked);
            /*StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed}, layerList);
            sld.addState(new int[]{},
                    getResources().getDrawable(R.drawable.pattern_button_unmarked));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                views[i].setBackground(sld);
            } else {
                views[i].setBackgroundDrawable(sld);
            }*/

            // We thought mutate was critical here, but it caused a strange error where
            // one of the buttons' color would not be changed.  By commenting out mutate(), the
            // problem goes away...
            //sld.mutate();

            //int strokeWidth = (int) BitmapToViewHelper.convertDpToPixel(1, this);
           // shape.setStroke(strokeWidth, mButtonColor);

            if (mButtonMarkedColor != -1) {
                //shape.setColor(mButtonMarkedColor);
                //markerView.setBackgroundColor(mButtonMarkedColor);
                //GradientDrawable shape = (GradientDrawable) getResources().getDrawable(R.drawable.pattern_button_marked);
                shape.setColor(mButtonMarkedColor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    markerView.setBackground(layerList);
                } else {
                    markerView.setBackgroundDrawable(layerList);
                }
            }
            markerView.setVisibility(View.INVISIBLE);

            if (mFont != null) {
                // TODO: do I need to try/catch this?
                ((Button) views[i]).setTypeface(Typeface.create(mFont, Typeface.NORMAL));
            }
        }

        return views;
    }

    protected View getCorrespondingMarkerView(View patternButton) throws IllegalArgumentException{
        try {
            ViewGroup parent = (ViewGroup) patternButton.getParent();
            if (parent.getChildCount() != 2) {
                // The encapsulating RL should only have two children
                throw new IllegalArgumentException("Pattern button's parent has "
                        + parent.getChildCount() + " children; should only have 2 children.");
            }
            View markerView = parent.getChildAt(0);
            if (markerView.getId() == patternButton.getId()) {
                markerView = parent.getChildAt(1);
            }
            if (markerView == null || markerView.getId() == View.NO_ID ) {
                throw new IllegalArgumentException("Adjacent marker view does not exist or does not have an Id");
            }

            return markerView;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Pattern button " + getKeyNumber(patternButton)
                    + " has invalid parent; must be ViewGroup");
        }
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (super.onTouch(view, event)){
            return true;
        }

        int keyNum = getKeyPressed(view.getId(), -1);
        //
        if (keyNum == -1) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Now handle pattern logic
                mLastKey = view;
                mPatternEntered += keyNum;
                // Draw the pattern
                markView(view);
                break;

            case MotionEvent.ACTION_MOVE:
                int x = (int)event.getRawX(), y =(int)event.getRawY();
                if (isTouchOutsideView(mLastKey, x, y)) {

                    boolean drawToTouch = true;  // Indicates we should draw to the user's touch
                    View touchedKey = getTouchedKey(x, y);
                    if (touchedKey != null && !mPatternEntered.contains(getKeyNumber(touchedKey))) {
                        vibrate(1);
                        markView(touchedKey);
                        drawLineToView(mLastKey, touchedKey);

                        mPatternDrawView.invalidate();
                        mTouchDrawView.clearLines();
                        mTouchDrawView.invalidate();
                        drawToTouch = false;

                        mLastKey = touchedKey;
                        mPatternEntered += getKeyNumber(touchedKey);
                    }
                    // Now based on what happened, we decide whether to draw to touch
                    if (drawToTouch) {
                        Paint p = new Paint();
                        p.setColor(mDrawColor);
                        p.setStrokeWidth(3f);

                        int[] startCoord = new int[2];
                        mLastKey.getLocationOnScreen(startCoord);
                        mTouchDrawView.clearLines();
                        mTouchDrawView.addLineWithAbsoluteCoords(
                                startCoord[0] + mLastKey.getWidth() / 2f,
                                startCoord[1] + mLastKey.getHeight() / 2f,
                                event.getRawX(),
                                event.getRawY(),
                                p);
                        mTouchDrawView.invalidate();
                    }
                }
                // If we are not outside the last key, we don't draw anything
                break;

            case MotionEvent.ACTION_UP:
                mTouchDrawView.clearLines();
                mTouchDrawView.invalidate();

                if (matchesPasscode(mPatternEntered)) {
                    //Log.d(TAG, "Correct passcode called");
                   onPasscodeCorrect();
                } else {
                   onPasscodeFail();
                }
                break;

        }

        return true;
    }

    private void markView(View view) {

        View markerView = getCorrespondingMarkerView(view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // get center for circle clipping
            int cx = markerView.getWidth()/2;
            int cy = markerView.getHeight()/2;

            //get final radius of for the clipping circle
            int finalRadius = Math.max(markerView.getWidth(), markerView.getHeight());

            // create the animator for this view
            Animator anim = ViewAnimationUtils.createCircularReveal(markerView, cx, cy, 0, finalRadius);
            anim.setDuration(mAnimTime);

            // make the view visible and start the animation
            markerView.setVisibility(View.VISIBLE);
            //view.setVisibility(View.INVISIBLE);
            anim.start();


            //Log.d(TAG, "markerView is x=" + markerView.getX() + " y=" +markerView.getY() + " width=" + markerView.getWidth());
        } else {
            markerView.setVisibility(View.VISIBLE);
            markerView.setAlpha(0f);
            markerView.animate().alpha(1f).setDuration(mAnimTime);
        }
        // Make the button disappear regardless of version
        view.animate().alpha(0f).setDuration(mAnimTime);
    }

    private void unmarkView(View view) {
        final View markerView = getCorrespondingMarkerView(view);
        markerView.setAlpha(1f);
        markerView.animate()
                .alpha(0f)
                .setDuration(mAnimTime).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                markerView.setVisibility(View.INVISIBLE);
            }

        });
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(mAnimTime);
    }

    public void clearPattern() {
        for (int i=0; i < mKeys.length; i++) {
            unmarkView(mKeys[i]);
        }
    }

    private void drawLineToView(View start, View end) throws IllegalArgumentException{
        int startNum = getKeyNumber(start, -1), endNum = getKeyNumber(end, -1);
        if (startNum == -1 || endNum == -1) {
            throw new IllegalArgumentException("View arguments invalid; start="
                    + startNum + " end=" + endNum);
        }

        int[] startCoord = new int[2];
        int[] endCoord = new int[2];
        start.getLocationOnScreen(startCoord);
        end.getLocationOnScreen(endCoord);

        float startX = startCoord[0] + start.getWidth() / 2f;
        float startY = startCoord[1] + start.getHeight() / 2f;
        float endX = endCoord[0] + end.getWidth() / 2f;
        float endY = endCoord[1] + end.getHeight() / 2f;
        if (!lineRequiresArc(startNum, endNum)) {
            // Draw a line
            Paint p = new Paint();
            p.setColor(mDrawColor);
            p.setStrokeWidth(mDrawWidth);
            mPatternDrawView.addLineWithAbsoluteCoords(
                    startX,
                    startY,
                    endX,
                    endY,
                    p);
        } else {
            // Now we must draw an arc
            drawArc(startNum, endNum, startX, startY, endX, endY);
        }
    }

    /**
     * Draws an arc between start and end.  Integers a and b represent the digits touched, with "a"
     * first
     *
     * @param a
     * @param b
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    private void drawArc(int a, int b, float startX, float startY, float endX, float endY) {
        float left, top, right, bottom, startAngle, sweepAngle;
        float rotation = 0;
        // the dimensions of the oval's sides
        left = startX < endX ? startX : endX;
        right = endX > startX ? endX : startX;
        top = startY < endY ? startY : endY;
        bottom = endY > startY ? endY : startY;

        // Need to modify the width of the oval to suit the way the arc will be drawn
        int diff = Math.abs(a - b);
        boolean isRotatedArc = false;  // Flag so right function is called later
        if (diff == 2) {
            // Make horizontal arc
            top -= getSpaceAvailableY();
            bottom += getSpaceAvailableY();
        } else if (diff == 6) {
            // Make vertical arc
            left -= getSpaceAvailableX();
            right += getSpaceAvailableX();
        } else {
            // Make diagonal arc, needing rotated oval!
            isRotatedArc = true;
            // Get dimensions of the RectF
            float height = (float) Math.sqrt((left - right) * (left - right)
                    + (top - bottom) * (top - bottom));
            int width = getCenterKey().getWidth(); // add some padding
            int[] centerCoords = new int[2];
            getCenterKey().getLocationOnScreen(centerCoords); // the center button
            int diagPadding = getResources()
                    .getInteger(R.integer.pattern_diagonal_drawing_padding);
            // Reassign ltrb to be a column in the middle
            rotation = getRotation(a, b, right - left, bottom - top);
            left = centerCoords[0] - diagPadding;
            right = centerCoords[0] + width + diagPadding;
            top = centerCoords[1] - height / 2 + width / 2;
            bottom = centerCoords[1] + height / 2 + width / 2;
        }

        startAngle = getStartAngle(a, b);
        sweepAngle = getSweepAngle(a, b);

        /*Log.d(TAG, "left = " + left + " top = " + top + " right = "
                + right + " bottom = " + bottom + " startAngle = "
                + startAngle + " sweepAngle = " + sweepAngle);*/

        if (startAngle != -1 && sweepAngle != -1) {
            Paint p = new Paint();
            p.setColor(mDrawColor);
            // TODO: set stroke width in attributes
            p.setStrokeWidth(mDrawWidth);
            p.setAntiAlias(true);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStyle(Paint.Style.STROKE);
            if (!isRotatedArc) {
                mPatternDrawView
                        .addArcWithAbsoluteCoords(left, top, right, bottom,
                                startAngle, sweepAngle, false, p);
            } else {
                mPatternDrawView
                        .addRotatedArcWithAbsoluteCoords(left, top,
                                right, bottom, rotation, startAngle,
                                sweepAngle, false, p);
            }
        } else {
            Log.e(TAG, "Error drawing arc; startAngle or sweepAngle invalid");
        }
    }

    /**
     * Returns true if, based on a square 9 digit keypad, int a and b requires an arc to draw a line
     * between them without traversing another digit.
     *
     * @param a
     * @param b
     * @return
     */
    private boolean lineRequiresArc(int a, int b) {
        int difference = Math.abs(a - b);
        switch (difference) {
            case 6:
                return true;
            case 4:
                if (a + b != 10) {
                    return false;
                }
                // Continue on, must be 3 & 7 so return true!
            case 8:
                return true;
            case 2:
                if ((a % 3 == 1 && b % 3 == 0) || (b % 3 == 1 && a % 3 == 0)) {
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * Method assumes that an arc is appropriate already
     *
     * @param a
     * @param b
     * @return
     */
    private float getStartAngle(int a, int b) {
        int difference = Math.abs(a - b);
        switch (difference) {
            case 2:
                if (a < b) {
                    return 180;
                } else {
                    return 0;
                }

            case 6:
                if (a < b) {
                    return 270;
                } else {
                    return 90;
                }

            case 4:
                if (a + b == 10) {
                    if (a < b) {
                        return 315;
                    } else {
                        return 135;
                    }

                }
                break;
            case 8:
                if (a < b) {
                    return 225;
                } else {
                    return 45;
                }
        }
        return -1;
    }

    private float getSweepAngle(int a, int b) {
        int difference = Math.abs(a - b);
        switch (difference) {
            case 2:
                if (a < b) {
                    // Note: Samsung Galaxy S4 exhibited strange error if we tried to put the arc
                    // b/t 7 and 9 below the digits in that it would continue to display the line
                    // after it was cleared.  This implementation is therefore not ideal, but
                    // necessary unless we want to go obscure bug chasing on what appears to be
                    // one device
                    return 180;
                } else {
                    return -180;
                }

            case 6:
                if ((a < b && (a == 1 || a == 2)) || (b < a && a == 9)) {
                    return -180;
                } else {
                    return 180;
                }

            case 4:
                if (a + b != 10) {
                    break;
                }
            case 8:
                return 180;
        }
        return -1;
    }

    /**
     * Returns oval rotation based on configuring the dominating length of the oval in the y direction
     *
     * @param a
     * @param b
     * @return
     */
    private float getRotation(int a, int b, float width, float height) {

        int difference = Math.abs(a - b);
        int multiplier;
        switch (difference) {
            case 8:
                //return -45;
                multiplier = -1;
                break;
            case 4:
                if (a + b == 10) {
                    //return 45;
                    multiplier = 1;
                    break;
                }
            default:
                return 0;
        }

        float returnValue = (float) Math.toDegrees(Math.atan(width / height)) * multiplier;
        Log.d(TAG, "rotation is " + returnValue);
        return returnValue;

    }

    private float getSpaceAvailableX() {
        int margin = (int) getResources().getDimension(R.dimen.pattern_buttons_layout_margin);
        int width = mKeys[0].getWidth();
        int colSpacer = findViewById(R.id.pattern_col_spacer).getWidth();
        return margin + width / 2 + colSpacer / 2;
    }

    private float getSpaceAvailableY() {
        int margin = (int) getResources().getDimension(R.dimen.pattern_buttons_layout_margin);
        int height = mKeys[0].getHeight();
        int rowSpacer = findViewById(R.id.pattern_row_spacer).getHeight();
        return margin + height / 2 + rowSpacer / 2;
    }

    private View getTouchedKey(int x, int y) {
        for (int i = 0; i < mKeys.length; i++) {
            int[] coord = new int[2];
            mKeys[i].getLocationOnScreen(coord);
            Rect r = new Rect(
                    coord[0],
                    coord[1],
                    coord[0] + mKeys[i].getWidth(),
                    coord[1] + mKeys[i].getHeight());
            if (r.contains(x, y)) {
                return mKeys[i];
            }
        }
        return null;
    }

    private View getCenterKey() {
        return mKeys[4];
    }

    private void vibrate (int length) {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(length);
    }

}