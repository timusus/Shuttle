package com.simplecity.amp_library.ui.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.R;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static com.simplecity.amp_library.utils.AnimUtils.lerp;
import static com.simplecity.amp_library.utils.ListUtils.emptyIfNull;
import static java.lang.StrictMath.*;
import static java.util.concurrent.TimeUnit.SECONDS;

public class SnowfallView extends View {

    private static final String TAG = SnowfallView.class.getSimpleName();

    /** The Remote Config key used to determine if it snows */
    private static final String LET_IT_SNOW = "let_it_snow";
    /** Forecast a <= 1% chance of snowing */
    private static final float LUCKY = 0.01f;

    /** Wait a few seconds before displaying snow */
    private static final long SNOWFALL_DELAY = SECONDS.toMillis(5);

    /** The number of snowflakes to generate */
    private static final int NUM_FLAKES = 200;

    /** Default min and max snowflake alpha */
    private static final int MIN_ALPHA = 150;
    private static final int MAX_ALPHA = 250;

    /** Default min and max snowflake angle */
    private static final double MIN_ANGLE = 80d;
    private static final double MAX_ANGLE = 100d;

    /** Default min and max snowflake velocity */
    private static final float MIN_SPEED = 2f;
    private static final float MAX_SPEED = 8f;

    /** Default min and max snowflake size */
    private static final float MIN_SIZE = 2f;
    private static final float MAX_SIZE = 15f;

    /** Used to paint each snowflake */
    private final Paint snowPaint = new Paint(ANTI_ALIAS_FLAG);
    /** The snowflakes currently falling */
    private final List<Snowflake> snowflakes = new ArrayList<>(0);
    /** Used to randomly generate snowflake params */
    private final Random snowRng = new SecureRandom();
    /** Used to delay snowfall */
    final Handler snowHandler = new Handler();

    /** Used to determine if we let it snow */
    final FirebaseRemoteConfig remoteConfig;

    public SnowfallView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        snowPaint.setColor(Color.WHITE);
        snowPaint.setStyle(Paint.Style.FILL);

        remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setDefaults(R.xml.remote_config_defaults);
        remoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Snowflake snowflake : emptyIfNull(snowflakes)) {
            if (round(snowflake.snowY()) > getHeight()) {
                snowflake.reset();
            }
            snowPaint.setAlpha(snowflake.alpha);
            canvas.drawCircle(snowflake.snowX(), snowflake.snowY(), snowflake.snowR, snowPaint);
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        clear();
        super.onDetachedFromWindow();
    }

    public void letItSnow() {
        if (snowflakes.isEmpty()) {
            if (lerp(0f, 1f, snowRng.nextFloat()) <= LUCKY) {
                fetchSnowConfig();
            }
        }
    }

    public void clear() {
        snowHandler.removeCallbacksAndMessages(null);
        snowflakes.clear();
        invalidate();
    }

    private void fetchSnowConfig() {
        remoteConfig.fetch().addOnCompleteListener((Activity) getContext(), task -> {
            if (task.isSuccessful()) {
                remoteConfig.activateFetched();
                if (remoteConfig.getBoolean(LET_IT_SNOW)) {
                    snowHandler.removeCallbacksAndMessages(null);
                    snowHandler.postDelayed(this::generateSnow, SNOWFALL_DELAY);
                    Log.d(TAG, "let it snow!");
                }
            }
        });
    }

    void generateSnow() {
        for (int i = 0; i < NUM_FLAKES; i++) {
            final double angle = toRadians(lerp(MIN_ANGLE, MAX_ANGLE, snowRng.nextDouble()));
            final float speed = lerp(MIN_SPEED, MAX_SPEED, snowRng.nextFloat());
            final float velX = (float) ((double) speed * cos(angle));
            final float velY = (float) ((double) speed * sin(angle));
            final float size = lerp(MIN_SIZE, MAX_SIZE, snowRng.nextFloat());
            final float startX = lerp(0f, (float) getWidth(), snowRng.nextFloat());
            float startY = lerp(0f, (float) getHeight(), snowRng.nextFloat());
            startY -= (float) getHeight() - size;
            final int alpha = lerp(MIN_ALPHA, MAX_ALPHA, snowRng.nextInt());
            snowflakes.add(new Snowflake(startX, startY, velX, velY, size, alpha));
        }
        invalidate();
    }

    static final class Snowflake {

        float snowX;
        float snowY;

        final float startX;
        final float startY;
        final float velX;
        final float velY;
        final float snowR;
        final int alpha;

        Snowflake(float startX, float startY, float velX, float velY, float snowR, int alpha) {
            snowX = startX;
            snowY = startY;
            this.startX = startX;
            this.startY = startY;
            this.velX = velX;
            this.velY = velY;
            this.snowR = snowR;
            this.alpha = alpha;
        }

        float snowX() {
            return snowX += velX;
        }

        float snowY() {
            return snowY += velY;
        }

        void reset() {
            snowX = startX;
            snowY = startY;
        }

    }

}
