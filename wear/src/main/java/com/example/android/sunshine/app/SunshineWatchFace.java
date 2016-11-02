/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTimeTextPaint;
        Paint mDateTextPaint;
        Paint mWeatherBitmapPaint;
        Paint mLowTextPaint;
        Paint mHighTextPaint;

        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffset;
        float mYOffset;
        float mLineHeight;

        int mWeatherId;
        Bitmap mWeatherBitmap;


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();

            mLineHeight = resources.getDimension(R.dimen.digital_line_height);

            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTimeTextPaint = new Paint();
            mTimeTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mHighTextPaint = new Paint();
            mHighTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mLowTextPaint = new Paint();
            mLowTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mDateTextPaint = new Paint();
            mDateTextPaint = createTextPaint(resources.getColor(R.color.digital_text_date));

            mWeatherBitmapPaint = new Paint();
            updateWeatherBitmap();

            mCalendar = Calendar.getInstance();
        }

        private void updateWeatherBitmap() {
            Resources resources = SunshineWatchFace.this.getResources();
            Drawable weatherBitmap;

            if (mWeatherId >= 200 && mWeatherId <= 232) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_storm, null);
            } else if (mWeatherId >= 300 && mWeatherId <= 321) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_light_rain, null);
            } else if (mWeatherId >= 500 && mWeatherId <= 504) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_rain, null);
            } else if (mWeatherId == 511) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_snow, null);
            } else if (mWeatherId >= 520 && mWeatherId <= 531) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_rain, null);
            } else if (mWeatherId >= 600 && mWeatherId <= 622) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_snow, null);
            } else if (mWeatherId >= 701 && mWeatherId <= 761) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_fog, null);
            } else if (mWeatherId == 761 || mWeatherId == 781) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_storm, null);
            } else if (mWeatherId == 800) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_clear, null);
            } else if (mWeatherId == 801) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_light_clouds, null);
            } else if (mWeatherId >= 802 && mWeatherId <= 804) {
                weatherBitmap = resources.getDrawable( R.drawable.ic_cloudy, null);
            } else { //default
                weatherBitmap = resources.getDrawable( R.drawable.ic_clear, null);
            }

            mWeatherBitmap = ((BitmapDrawable) weatherBitmap).getBitmap();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            float textDateSize = resources.getDimension(R.dimen.digital_text_date_size);
            float textTempSize = resources.getDimension(R.dimen.digital_text_temp_size);

            mTimeTextPaint.setTextSize(textSize);
            mHighTextPaint.setTextSize(textTempSize);
            mLowTextPaint.setTextSize(textTempSize);
            mDateTextPaint.setTextSize(textDateSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTimeTextPaint.setAntiAlias(!inAmbientMode);
                    mDateTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.

                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.

            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(WeatherDataService.WEATHER_DATA, Context.MODE_PRIVATE);
            String highTemp = sharedPref.getString(WeatherDataService.WEATHER_HIGH, getString(R.string.empty_weather));
            String lowTemp = sharedPref.getString(WeatherDataService.WEATHER_LOW, getString(R.string.empty_weather));
            float posCenter = bounds.centerX() - (mHighTextPaint.measureText(highTemp) / 2);
            float posCenterRight = (bounds.centerX() * 1.5f) - (mLowTextPaint.measureText(lowTemp) / 2);

            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);

            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

                //get weather data from shared preferences

                int tempWeatherId = sharedPref.getInt(WeatherDataService.WEATHER_ID, 800);

                if(tempWeatherId != mWeatherId){
                    mWeatherId = tempWeatherId;
                    updateWeatherBitmap();
                }

                float iconSize = (float) (bounds.width() / 4);
                float scale =  iconSize / (float) mWeatherBitmap.getWidth();
                mWeatherBitmap = Bitmap.createScaledBitmap(mWeatherBitmap,
                        (int) (mWeatherBitmap.getWidth() * scale),
                        (int) (mWeatherBitmap.getHeight() * scale), true);


                float posCenterLeft = ((bounds.width() / 3)) / 1.5f - (mWeatherBitmap.getWidth() / 2); //minus half width of scaled bitmpa
                canvas.drawBitmap(mWeatherBitmap, posCenterLeft, (mYOffset + mLineHeight * 6) - (mWeatherBitmap.getHeight() / 1.5f), null);

            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String timeText = String.format("%d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE));
            String date = new SimpleDateFormat("EEE, MMM d yyyy").format(mCalendar.getTime()).toUpperCase();



            canvas.drawText(timeText, bounds.centerX() - (mTimeTextPaint.measureText(timeText)) / 2, mYOffset, mTimeTextPaint);
            canvas.drawText(date, bounds.centerX() - (mDateTextPaint.measureText(date)) / 2, mYOffset + mLineHeight * 2, mDateTextPaint);
            canvas.drawText(highTemp, posCenter, mYOffset + mLineHeight * 6, mHighTextPaint);
            canvas.drawText(lowTemp, posCenterRight, mYOffset + mLineHeight * 6, mLowTextPaint);


        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }


}
