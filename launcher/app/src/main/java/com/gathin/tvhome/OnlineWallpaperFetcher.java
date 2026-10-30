package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads and rotates ultra-high-resolution 8K/4K curated wallpapers from online in the background.
 * Automatically cycles wallpapers every interval, caching locally to reduce bandwidth.
 * Invokes failsafe callback if online fetch or decoding fails, allowing dynamic
 * app-based gradient reactions to take over seamlessly.
 */
public final class OnlineWallpaperFetcher {

    public interface Callback {
        void onWallpaperLoaded(Bitmap bitmap);
        void onWallpaperFailed();
    }

    private static final String TAG = "OnlineWallpaper";

    private static final String[] WALLPAPER_IDS = new String[]{
            "1506744038136-46273834b3fb",
            "1511884642898-4c92249e20b6",
            "1470071459604-3b5ec3a7fe05"
    };

    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final int targetWidth;
    private final int targetHeight;
    private final long intervalMs;
    private final File cacheDir;

    private boolean running = false;
    private int currentIndex = -1;

    private final Runnable rotateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            fetchNext();
            mainHandler.postDelayed(this, intervalMs);
        }
    };

    private final boolean shuffle;

    public static volatile Bitmap latestBitmap;

    public static File getCacheDirectory(Context context) {
        File dir = new File(context.getFilesDir(), "online_wallpapers");
        if (dir.exists() && !dir.isDirectory()) dir.delete();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public OnlineWallpaperFetcher(Context context, int targetWidth, int targetHeight, long intervalMs, boolean shuffle, Callback callback) {
        this.targetWidth = targetWidth > 0 ? targetWidth : 1920;
        this.targetHeight = targetHeight > 0 ? targetHeight : 1080;
        this.intervalMs = intervalMs;
        this.shuffle = shuffle;
        this.callback = callback;
        this.cacheDir = getCacheDirectory(context);
    }

    public void start() {
        if (running) return;
        running = true;
        fetchNext();
        if (intervalMs > 0) {
            mainHandler.postDelayed(rotateRunnable, intervalMs);
        }
    }

    public void stop() {
        running = false;
        mainHandler.removeCallbacks(rotateRunnable);
    }

    public void fetchNext() {
        executor.execute(() -> {
            if (cacheDir.exists() && !cacheDir.isDirectory()) cacheDir.delete();
            if (!cacheDir.exists()) cacheDir.mkdirs();

            int nextIdx;
            if (shuffle) {
                nextIdx = (currentIndex + 1 + random.nextInt(WALLPAPER_IDS.length - 1)) % WALLPAPER_IDS.length;
            } else {
                nextIdx = (currentIndex + 1) % WALLPAPER_IDS.length;
            }
            currentIndex = nextIdx;
            String url = "https://images.unsplash.com/photo-" + WALLPAPER_IDS[nextIdx] + "?w=3840&q=80";
            File targetFile = new File(cacheDir, "wp_" + nextIdx + ".jpg");

            boolean success = false;
            if (targetFile.exists() && targetFile.length() > 5000) {
                success = true;
            } else {
                File tmpFile = new File(cacheDir, "wp_" + nextIdx + ".tmp");
                File parent = tmpFile.getParentFile();
                if (parent != null) {
                    if (parent.exists() && !parent.isDirectory()) parent.delete();
                    if (!parent.exists()) parent.mkdirs();
                }
                try {
                    String currentUrl = url;
                    HttpURLConnection conn = null;
                    for (int redirect = 0; redirect < 3; redirect++) {
                        conn = (HttpURLConnection) new URL(currentUrl).openConnection();
                        conn.setInstanceFollowRedirects(true);
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(20000);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) CleanMyTV/1.0");
                        conn.connect();
                        int code = conn.getResponseCode();
                        if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                            String location = conn.getHeaderField("Location");
                            conn.disconnect();
                            if (location != null && !location.isEmpty()) {
                                currentUrl = location;
                                continue;
                            }
                        }
                        break;
                    }
                    if (conn != null && conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        try (InputStream in = conn.getInputStream();
                             FileOutputStream out = new FileOutputStream(tmpFile)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            out.flush();
                        }
                        if (tmpFile.length() > 5000) {
                            if (targetFile.exists()) targetFile.delete();
                            success = tmpFile.renameTo(targetFile);
                            if (!success && tmpFile.exists()) {
                                // Fallback rename if filesystem rename fails
                                targetFile = tmpFile;
                                success = true;
                            }
                        }
                    }
                    if (conn != null) conn.disconnect();
                } catch (Exception e) {
                    Log.w(TAG, "Online wallpaper download failed: " + e.getMessage());
                    success = false;
                } finally {
                    if (tmpFile.exists() && targetFile != tmpFile) tmpFile.delete();
                }
            }

            if (!success || !targetFile.exists()) {
                File[] cached = cacheDir.listFiles((d, n) -> n.endsWith(".jpg") && new File(d, n).length() > 5000);
                if (cached != null && cached.length > 0) {
                    targetFile = cached[random.nextInt(cached.length)];
                    success = true;
                }
            }

            if (!success || !targetFile.exists()) {
                mainHandler.post(callback::onWallpaperFailed);
                return;
            }

            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(targetFile.getAbsolutePath(), opts);
                int sample = 1;
                while ((opts.outWidth / (sample * 2)) >= targetWidth && (opts.outHeight / (sample * 2)) >= targetHeight) {
                    sample *= 2;
                }
                opts.inJustDecodeBounds = false;
                opts.inSampleSize = sample;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bmp = BitmapFactory.decodeFile(targetFile.getAbsolutePath(), opts);
                if (bmp != null) {
                    latestBitmap = bmp;
                    mainHandler.post(() -> callback.onWallpaperLoaded(bmp));
                } else {
                    mainHandler.post(callback::onWallpaperFailed);
                }
            } catch (Throwable t) {
                Log.w(TAG, "Decode failed: " + t.getMessage());
                mainHandler.post(callback::onWallpaperFailed);
            }
        });
    }
}
