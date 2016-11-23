package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Simon! (with lots of help from Jon) on 11/22/2016
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();
    private VelocityTracker _velocityTracker = null;
    private Random _rand = null;

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Circle;
    private float _minBrushRadius = 5;

    /* Constructors for ImpressionistView. Also initializes a velocity tracker and random.
       Velocity tracker used to change brush radius/brush width based on speed. Random is
       used for spray paint type of effect to get random pixel in circle radius of a point.
     */
    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
        _velocityTracker = VelocityTracker.obtain();
        _rand = new Random();
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
        _velocityTracker = VelocityTracker.obtain();
        _rand = new Random();
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
        _velocityTracker = VelocityTracker.obtain();
        _rand = new Random();
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(10);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //TODO
        _offScreenCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    /**
     * Obtain bitmap of drawing.
     */
    public Bitmap getBitmap() {
        return _offScreenBitmap;
    }

    /**
     * Overrides onDraw and also draws border to emphasize size of bitmap.
     */
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);

    }

    /**
     *  When user touches the screen, we will obtain the bitmap and get coordinate of where user
     *  touched. For the two cases:
     *      1) Action Down: We will obtain color at the pixel of where user touched the screen
     *      and draw single shape/point based on users selected brush size.
     *      2) Action Move: We will obtain historical points and get coordinates of the historical
     *      touches. For circle/square brush, we will also use the velocity tracker to determine
     *      the size of the brush based on user finger movement speed. For line brush, the stroke
     *      width is just set to a size of 5 and will not change according to speed. For the circle
     *      splatter, we will use a random function for x and y and generate random gaussean points to obtain
     *      a collection of random points within a circumference.
     *
     *      Source: Circle Splatter (Spray paint)
     *      http://stackoverflow.com/questions/11938632/creating-a-spray-effect-on-touch-draw-in-android
     */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to listen for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        Bitmap bitmap = _imageView.getDrawingCache();

        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();
        int pixel;

        // Action Down case and checks for different type of brushes.
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (validScreen(touchX, touchY)) {
                    pixel = bitmap.getPixel((int) touchX, (int) touchY);
                    _paint.setARGB(200, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
                    _paint.setStrokeWidth(5);
                    _defaultRadius = 5;
                    if (_brushType == BrushType.Circle) {
                        _offScreenCanvas.drawCircle(touchX, touchY, _defaultRadius, _paint);
                    } else if (_brushType == BrushType.Square) {
                        /* Put in right dimensions */
                        _offScreenCanvas.drawRect(touchX - _defaultRadius, touchY - _defaultRadius,
                                touchX + _defaultRadius, touchY + _defaultRadius, _paint);
                    } else if (_brushType == BrushType.Line) {
                        _paint.setStrokeWidth(5);
                        _offScreenCanvas.drawPoint(touchX, touchY, _paint);
                    } else if (_brushType == BrushType.CircleSplatter) {
                        int randomX = _rand.nextInt(20);
                        int randomY = _rand.nextInt(20);
                        _offScreenCanvas.drawPoint(touchX + randomX, touchY + randomY, _paint);
                    }
                    _velocityTracker.addMovement(motionEvent);
                }
                break;
            /* Action moves and checks for different type of brushes. Obtain historical points to
               track user movements. Circle and square brush change dynamically based on speed.
               Circle splatter will generate random x and y points within defaultRadius to create
               spray paint effect. Line brush will be set at 5 and will draw lines.
             */
            case MotionEvent.ACTION_MOVE:
                int history = motionEvent.getHistorySize();
                for (int i = 0; i < history; i++) {
                    float pastX = motionEvent.getHistoricalX(i);
                    float pastY = motionEvent.getHistoricalY(i);
                    if (validScreen(pastX, pastY)) {
                        pixel = bitmap.getPixel((int) pastX, (int) pastY);
                        _paint.setARGB(200, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
                        _velocityTracker.addMovement(motionEvent);
                        _velocityTracker.computeCurrentVelocity(1000);

                        int combinedVelocity = (int) Math.sqrt(Math.pow(_velocityTracker.getXVelocity(), 2) +
                                Math.pow(_velocityTracker.getYVelocity(), 2)) / 100;
                        if (combinedVelocity > 50) {
                            combinedVelocity = 50;
                        }
                        _defaultRadius = combinedVelocity;

                        if (_brushType == BrushType.Circle) {
                            _paint.setStrokeWidth(combinedVelocity);
                            _offScreenCanvas.drawCircle(pastX, pastY, _defaultRadius, _paint);
                        } else if (_brushType == BrushType.Square) {
                            _paint.setStrokeWidth(combinedVelocity);
                            _offScreenCanvas.drawRect(pastX - _defaultRadius, pastY - _defaultRadius,
                                    pastX + _defaultRadius, pastY + combinedVelocity, _paint);
                        } else if (_brushType == BrushType.Line) {
                            _paint.setStrokeWidth(5);
                            _offScreenCanvas.drawLine(pastX - _paint.getStrokeWidth(),
                                    pastY - _paint.getStrokeWidth(), pastX + _paint.getStrokeWidth(),
                                    pastY + _paint.getStrokeWidth(), _paint);
                        } else if (_brushType == BrushType.CircleSplatter) {
                            for (int j = 0; j < 20; j++) {
                                float randomX = (float) (pastX + _rand.nextGaussian()* _defaultRadius);
                                float randomY = (float) (pastY + _rand.nextGaussian()* _defaultRadius);
                                _offScreenCanvas.drawPoint(randomX, randomY, _paint);
                            }

                        }
                    }
                }
                break;
        }

        invalidate();
        return true;
    }

    /* Check if x and y coordinate are within a valid coordinate of the offScreenCanvas */
    private boolean validScreen(float x, float y) {
        if (x > 0 && x < _offScreenCanvas.getWidth() && y > 0 && y < _offScreenCanvas.getHeight()) {
            return true;
        }
        return false;
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

