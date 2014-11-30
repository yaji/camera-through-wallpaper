package com.yaji.viewfinder;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;

public class ImageUtil {

    /**
     * YUV420 to BMP
     * 
     * @param rgb
     * @param yuv420sp
     * @param width
     * @param height
     */
    public static final void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    /*
     * Save bitmap to image file.
     */
    public static void saveBitmapAsFile(String filepath, Bitmap bitmap, Bitmap.CompressFormat format) {
        try {
            // bitmap to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(format, 100, baos);
            baos.flush();
            byte[] byteArray = baos.toByteArray();
            baos.close();

            // byte array to file
            FileOutputStream out = new FileOutputStream(filepath);
            out.write(byteArray);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Request MediaScanner to scan the file to be saved right now, and to add it to the media DB.
     */
    public static void scanFile(Context c, String filepath) {
        String[] paths = new String[] { filepath };
        String[] mimeTypes = new String[] { "image/jpeg" };
        MediaScannerConnection.scanFile(c, paths, mimeTypes, null);
    }
}
