package com.freshcheck.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.InputStream;

public class CameraImageHelper {
    private static final String TAG = "CameraImageHelper";

    public static Uri createTempImageUri(Context context) {
        try {
            File storageDir = new File(context.getCacheDir(), "camera_photos");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            File imageFile = new File(storageDir, "capture_" + System.currentTimeMillis() + ".jpg");
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    imageFile
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to create temp image file: " + e.getMessage());
            return null;
        }
    }

    public static Bitmap loadOptimizedBitmap(Context context, Uri uri, int maxDim) {
        if (context == null || uri == null) return null;

        Bitmap bitmap = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source, new ImageDecoder.OnHeaderDecodedListener() {
                    @Override
                    public void onHeaderDecoded(ImageDecoder decoder, ImageDecoder.ImageInfo info, ImageDecoder.Source source) {
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        if (maxDim > 0 && (info.getSize().getWidth() > maxDim || info.getSize().getHeight() > maxDim)) {
                            int w = info.getSize().getWidth();
                            int h = info.getSize().getHeight();
                            float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
                            decoder.setTargetSize(Math.round(w * ratio), Math.round(h * ratio));
                        }
                    }
                });
            } else {
                InputStream is = context.getContentResolver().openInputStream(uri);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, opts);
                if (is != null) is.close();

                int sampleSize = 1;
                if (maxDim > 0) {
                    while (opts.outWidth / (sampleSize * 2) >= maxDim || opts.outHeight / (sampleSize * 2) >= maxDim) {
                        sampleSize *= 2;
                    }
                }

                BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
                decodeOpts.inSampleSize = sampleSize;
                decodeOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                InputStream is2 = context.getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(is2, null, decodeOpts);
                if (is2 != null) is2.close();
            }

            if (bitmap != null) {
                // Ensure software bitmap
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                }

                // Correct rotation using Exif
                bitmap = rotateBitmapIfRequired(context, uri, bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding bitmap from Uri: " + e.getMessage());
            try {
                // Last-resort fallback
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Fallback bitmap decode failed: " + ex.getMessage());
            }
        }

        return bitmap;
    }

    private static Bitmap rotateBitmapIfRequired(Context context, Uri uri, Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return bitmap;

            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            input.close();

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);
                default:
                    return bitmap;
            }
        } catch (Exception e) {
            return bitmap;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        if (rotatedImg != img) {
            img.recycle();
        }
        return rotatedImg;
    }
}
