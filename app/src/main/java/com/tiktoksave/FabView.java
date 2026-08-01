package com.tiktoksave;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Draggable floating download button attached to the current activity.
 *  - tap       -> download the video/photo currently on screen
 *  - long-press -> download all posts in the current feed
 *  - drag      -> move it anywhere
 */
@SuppressLint("ViewConstructor")
public final class FabView extends TextView {

    private static FabView instance;
    private static ViewGroup currentHost;

    private float downX, downY;
    private int startMarginL, startMarginT;
    private boolean moved;
    private boolean longPressed;

    public static void attach(android.app.Activity activity) {
        if (!Settings.fabEnabled()) return;
        try {
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof ViewGroup)) return;
            final ViewGroup host = (ViewGroup) content;
            if (instance != null) {
                if (instance.getParent() == host) return; // already attached here
                detach();
            }
            instance = new FabView(activity);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            int m = dp(activity, 12);
            int bottom = dp(activity, 96);
            lp.gravity = Gravity.BOTTOM | Gravity.END;
            lp.setMargins(m, 0, m, bottom);
            host.addView(instance, lp);
            currentHost = host;
        } catch (Throwable ignored) {
        }
    }

    public static void detach() {
        try {
            if (instance != null && instance.getParent() != null) {
                ((ViewGroup) instance.getParent()).removeView(instance);
            }
            instance = null;
            currentHost = null;
        } catch (Throwable ignored) {
        }
    }

    private FabView(Context ctx) {
        super(ctx);
        setText("\u2913"); // downwards arrow to bar
        setTextColor(Color.WHITE);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        setGravity(Gravity.CENTER);
        int size = dp(ctx, 52);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xDD000000);
        bg.setStroke(dp(ctx, 2), 0x88FF3B5C);
        if (Build.VERSION.SDK_INT >= 21) setElevation(dp(ctx, 8));
        setBackground(bg);
        setClickable(true);
        setLongClickable(true);
        setContentDescription("Download TikTok");
        setOnClickListener(v -> Downloader.downloadCurrent(ctx));
        setOnLongClickListener(v -> {
            longPressed = true;
            Downloader.downloadFeed(ctx);
            return true;
        });
        setOnTouchListener(dragListener);
    }

    private final OnTouchListener dragListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = ev.getRawX();
                    downY = ev.getRawY();
                    moved = false;
                    longPressed = false;
                    FrameLayout.LayoutParams lp0 = (FrameLayout.LayoutParams) v.getLayoutParams();
                    startMarginL = lp0.leftMargin;
                    startMarginT = lp0.topMargin;
                    return false; // let click/long-click still fire
                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getRawX() - downX;
                    float dy = ev.getRawY() - downY;
                    if (Math.abs(dx) > dp(getContext(), 8) || Math.abs(dy) > dp(getContext(), 8)) {
                        moved = true;
                        if (v.getParent() instanceof ViewGroup) {
                            ViewGroup host = (ViewGroup) v.getParent();
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
                            int newLeft = Math.max(0, startMarginL + (int) dx);
                            int newTop = Math.max(0, startMarginT + (int) dy);
                            lp.gravity = Gravity.TOP | Gravity.START;
                            lp.leftMargin = newLeft;
                            lp.topMargin = newTop;
                            v.setLayoutParams(lp);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved && !longPressed) {
                        v.performClick();
                    }
                    return true;
            }
            return false;
        }
    };

    private static int dp(Context ctx, int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics()));
    }
}
