package com.pblweb.android.rockshot;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Matthew Tole on 11/08/13.
 */
public class ScreenshotGenerator {

    public final int SCREENSHOT_WIDTH = 144;
    public final int SCREENSHOT_HEIGHT = 168;
    public final int WRAPPED_WIDTH = 231;
    public final int WRAPPED_HEIGHT = 393;
    private final int WRAP_OFFSET_X = 44;
    private final int WRAP_OFFSET_Y = 105;
    private String TAG = ScreenshotGenerator.class.getSimpleName();

    public enum WrapperColor { Black, Red, Grey, Orange, White };

    protected Bitmap screenshot;
    protected int current_x;
    protected int current_y;
    protected int pixels_loaded;
    protected Resources resources;
    protected Bitmap overlay;

    public static final Map<WrapperColor, Integer> wrapper_map;
    static {
        Map<WrapperColor, Integer> tmp = new HashMap<WrapperColor, Integer>();
        tmp.put(WrapperColor.Black, R.drawable.wrapper_black);
        tmp.put(WrapperColor.Red, R.drawable.wrapper_red);
        tmp.put(WrapperColor.Grey, R.drawable.wrapper_grey);
        tmp.put(WrapperColor.Orange, R.drawable.wrapper_orange);
        tmp.put(WrapperColor.White, R.drawable.wrapper_white);
        wrapper_map = Collections.unmodifiableMap(tmp);
    }


    public ScreenshotGenerator(Resources resources) {
        this.screenshot = Bitmap.createBitmap(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, Bitmap.Config.ARGB_8888);
        this.current_x = 0;
        this.current_y = 0;
        this.pixels_loaded = 0;
        this.resources = resources;

        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inScaled = false;
        this.overlay = BitmapFactory.decodeResource(resources, R.drawable.wrapper_shadow, bfo);
    }

    public void addPixels(byte[] data) {
        for (int t = 0; t < data.length; t += 1) {
            for (int b = 0; b < 8; b += 1) {
                int pixel_color = (isBitSet(data[t], b) ? Color.WHITE : Color.BLACK);
                screenshot.setPixel(current_x, current_y, pixel_color);
                current_x += 1;
                if (current_x == SCREENSHOT_WIDTH) {
                    current_x  = 0;
                    current_y += 1;
                }
                this.pixels_loaded += 1;
            }
        }
    }

    public Bitmap getWrappedScreenshot(WrapperColor color, boolean overlay) {
        Bitmap screenshot_wrapped = Bitmap.createBitmap(WRAPPED_WIDTH, WRAPPED_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas sw_canvas = new Canvas(screenshot_wrapped);
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inScaled = false;
        Bitmap wrapper = BitmapFactory.decodeResource(resources, wrapper_map.get(color), bfo);
        sw_canvas.drawBitmap(wrapper, new Rect(0, 0, wrapper.getWidth(), wrapper.getHeight()), new RectF(0f, 0f, WRAPPED_WIDTH, WRAPPED_HEIGHT), null);
        sw_canvas.drawBitmap(this.screenshot, (float)WRAP_OFFSET_X, (float)WRAP_OFFSET_Y, null);
        if (overlay) {
            sw_canvas.drawBitmap(this.overlay, new Rect(0, 0, this.overlay.getWidth(), this.overlay.getHeight()),
                    new RectF(WRAP_OFFSET_X, WRAP_OFFSET_Y, WRAP_OFFSET_X + SCREENSHOT_WIDTH, WRAP_OFFSET_Y + SCREENSHOT_HEIGHT), null);
        }
        return screenshot_wrapped;
    }

    public Bitmap getWrappedScreenshot(WrapperColor color) {
        return getWrappedScreenshot(color, true);
    }

    public Bitmap getWrappedScreenshot() {
        return getWrappedScreenshot(WrapperColor.Black);
    }

    public Bitmap getScreenshot() {
        return screenshot;
    }

    private static Boolean isBitSet(byte b, int bit) {
        return (b & (1 << bit)) != 0;
    }

    public int getProgress() {
        return (int)(100 * ((float)this.pixels_loaded / ((float)SCREENSHOT_WIDTH * (float)SCREENSHOT_HEIGHT)));
    }


}
