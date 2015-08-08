package reach.project.viewHelpers;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.squareup.picasso.Transformation;

/**
 * Created by ashish on 17/2/15.
 */
public class CircleTransform implements Transformation {
    @Override
    public Bitmap transform(Bitmap source) {

        final int size = Math.min(source.getWidth(), source.getHeight());
        final int x = (source.getWidth() - size) / 2;
        final int y = (source.getHeight() - size) / 2;
        final Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source)
            source.recycle();

        try {

            final Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());
            final Paint paint = new Paint();
            paint.setShader(new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            paint.setAntiAlias(true);
            final float r = size / 2f;
            new Canvas(bitmap).drawCircle(r, r, r, paint);
            squaredBitmap.recycle();
            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return squaredBitmap;
        }
    }

    @Override
    public String key() {
        return "circle";
    }
}
