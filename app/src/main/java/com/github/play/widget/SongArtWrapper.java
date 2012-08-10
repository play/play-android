/*
 * Copyright 2012 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.play.widget;

import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.github.kevinsawicki.http.HttpRequest.CHARSET_UTF8;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

import com.github.play.R.id;
import com.github.play.core.PlayService;
import com.github.play.core.Song;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * View wrapper to load and display art for a {@link Song}
 */
public class SongArtWrapper {

	/**
	 * Get cached art for song
	 *
	 * @param context
	 * @param song
	 * @return art or null if not available locally
	 */
	public static Bitmap getCachedArt(final Context context, final Song song) {
		File file = getArtFile(getArtDirectory(context), song);
		if (!isValid(file))
			return null;

		Options options = new Options();
		options.inDither = false;
		options.inPreferredConfig = ARGB_8888;
		Bitmap decoded = BitmapFactory.decodeFile(file.getAbsolutePath(),
				options);
		if (decoded == null)
			Log.d(TAG, "Decoding " + file.getName() + " failed");
		return decoded;
	}

	private static final String TAG = "SongArtWrapper";

	private static final int DIGEST_LENGTH = 40;

	private static final Executor EXECUTORS = Executors.newFixedThreadPool(1);

	private static final int MAX_RECENT = 50;

	private static final int MAX_SIZE_DP = 80;

	private static final MessageDigest SHA1_DIGEST;

	private static final String ART_FOLDER = "art";

	/**
	 * Version of art to display
	 * <p>
	 * This counter should be incremented when {@link #MAX_SIZE_DP} changes or
	 * if old art should be cleared and re-downloaded
	 */
	private static final int ART_VERSION = 1;

	static {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			digest = null;
		}
		SHA1_DIGEST = digest;
	}

	private static final SparseArray<Map<String, Drawable>> RECENT_ART = new SparseArray<Map<String, Drawable>>(
			2);

	private static final SparseArray<Drawable> EMPTY_ART = new SparseArray<Drawable>(
			2);

	private static String digest(Song song) {
		if (SHA1_DIGEST == null)
			return null;

		byte[] value;
		try {
			value = song.getAlbumId().getBytes(CHARSET_UTF8);
		} catch (UnsupportedEncodingException e) {
			return null;
		}

		byte[] digested;
		synchronized (SHA1_DIGEST) {
			SHA1_DIGEST.reset();
			digested = SHA1_DIGEST.digest(value);
		}
		String hashed = new BigInteger(1, digested).toString(16);
		int padding = DIGEST_LENGTH - hashed.length();
		if (padding > 0) {
			char[] zeros = new char[padding];
			Arrays.fill(zeros, '0');
			hashed = new String(zeros) + hashed;
		}
		return hashed;
	}

	private static Map<String, Drawable> createCacheMap() {
		return new LinkedHashMap<String, Drawable>(MAX_RECENT, 1.0F) {

			private static final long serialVersionUID = -3434208982358063608L;

			@Override
			protected boolean removeEldestEntry(
					Map.Entry<String, Drawable> eldest) {
				return size() >= MAX_RECENT;
			}
		};
	}

	private static Drawable getCachedArt(final int drawable, final Song song) {
		final String digest = digest(song);
		if (digest != null)
			synchronized (RECENT_ART) {
				Map<String, Drawable> cache = RECENT_ART.get(drawable);
				return cache != null ? cache.get(digest) : null;
			}
		else
			return null;
	}

	private static Drawable getEmptyArt(final int drawable,
			final Context context) {
		Drawable cached = EMPTY_ART.get(drawable);
		if (cached == null) {
			cached = context.getResources().getDrawable(drawable);
			EMPTY_ART.put(drawable, cached);
		}
		return cached;
	}

	private static void putCachedArt(final int drawable, final Song song,
			final Drawable bitmap) {
		if (bitmap == null)
			return;

		final String digest = digest(song);
		if (digest != null)
			synchronized (RECENT_ART) {
				Map<String, Drawable> cache = RECENT_ART.get(drawable);
				if (cache == null) {
					cache = createCacheMap();
					RECENT_ART.put(drawable, cache);
				}
				cache.put(digest, bitmap);
			}
	}

	private static Point getSize(final File file) {
		final Options options = new Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(file.getAbsolutePath(), options);
		if (options.outWidth <= 0 || options.outHeight <= 0)
			Log.d(TAG, "Decoding bounds of " + file.getName() + " failed");
		return new Point(options.outWidth, options.outHeight);
	}

	/**
	 * Get art file for song
	 *
	 * @param parent
	 * @param song
	 * @return file
	 */
	protected static File getArtFile(final File parent, final Song song) {
		return new File(parent, digest(song) + ".png");
	}

	/**
	 * Get art directory
	 *
	 * @param context
	 * @return directory for storing song art
	 */
	protected static File getArtDirectory(Context context) {
		File artFolder = new File(context.getCacheDir(), ART_FOLDER
				+ ART_VERSION);
		if (!artFolder.exists())
			artFolder.mkdirs();
		return artFolder;
	}

	/**
	 * Is file non-null, existent and non-empty?
	 *
	 * @param file
	 * @return true if valid, false otherwise
	 */
	protected static boolean isValid(final File file) {
		return file != null && file.exists() && file.length() > 0;
	}

	private final File artFolder;

	private final AtomicReference<PlayService> service;

	private final int maxSize;

	private final Activity activity;

	private boolean oldArtDeleted;

	/**
	 * Create view wrapper to display art for a {@link Song}
	 *
	 * @param activity
	 * @param service
	 */
	public SongArtWrapper(Activity activity,
			final AtomicReference<PlayService> service) {
		this.activity = activity;
		artFolder = getArtDirectory(activity);
		this.service = service;
		Resources resources = activity.getResources();
		maxSize = Math.round(resources.getDisplayMetrics().density
				* MAX_SIZE_DP + 0.5F);
	}

	/**
	 * Decode {@link Bitmap} from given {@link File}
	 *
	 * @param file
	 * @return bitmap
	 */
	protected Bitmap decode(final File file) {
		Point size = getSize(file);
		int currWidth = size.x;
		int currHeight = size.y;

		int scale = 1;
		while (currWidth >= maxSize || currHeight >= maxSize) {
			currWidth /= 2;
			currHeight /= 2;
			scale *= 2;
		}

		Options options = new Options();
		options.inDither = false;
		options.inSampleSize = scale;
		options.inPreferredConfig = ARGB_8888;
		Bitmap decoded = BitmapFactory.decodeFile(file.getAbsolutePath(),
				options);
		if (decoded == null)
			Log.d(TAG, "Decoding " + file.getName() + " failed");
		return decoded;
	}

	/**
	 * Write {@link Bitmap} to given {@link File}
	 *
	 * @param bitmap
	 * @param file
	 * @return bitmap
	 */
	protected Bitmap write(final Bitmap bitmap, final File file) {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			if (!bitmap.compress(PNG, 100, stream))
				Log.d(TAG, "Compressing " + file.getName() + " failed");
			return bitmap;
		} catch (FileNotFoundException e) {
			return bitmap;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException ignored) {
					// Ignored
				}
		}
	}

	private void updateDrawable(final ImageView view, final Drawable art) {
		view.setTag(null);
		view.setImageDrawable(art);
	}

	/**
	 * Delete file or directory include child files
	 *
	 * @param file
	 */
	private void delete(final File file) {
		if (!file.exists())
			return;

		if (file.isDirectory()) {
			Log.d(TAG, "Deleting art directory: " + file.getName());
			File[] children = file.listFiles();
			if (children != null)
				for (File child : children)
					delete(child);
		}
		file.delete();
	}

	/**
	 * Delete art in old folders
	 */
	private void deleteOldArt() {
		if (oldArtDeleted)
			return;

		File root = artFolder.getParentFile();
		delete(new File(root, ART_FOLDER));
		for (int i = 0; i < ART_VERSION; i++)
			delete(new File(root, ART_FOLDER + i));
		oldArtDeleted = true;
	}

	/**
	 * Update view with art for song album
	 *
	 * @param artView
	 * @param drawable
	 *            a layer drawable with an album art layer
	 * @param song
	 */
	public void update(final ImageView artView, final int drawable,
			final Song song) {
		update(artView, drawable, song, song);
	}

	/**
	 * Update view with art for song album
	 *
	 * @param artView
	 * @param drawable
	 *            a layer drawable with an album art layer
	 * @param song
	 * @param tag
	 */
	public void update(final ImageView artView, final int drawable,
			final Song song, final Object tag) {
		if (song == null) {
			updateDrawable(artView, getEmptyArt(drawable, activity));
			return;
		}

		Drawable cachedBitmap = getCachedArt(drawable, song);
		if (cachedBitmap != null) {
			updateDrawable(artView, cachedBitmap);
			return;
		}

		updateDrawable(artView, getEmptyArt(drawable, activity));
		artView.setTag(tag);

		EXECUTORS.execute(new Runnable() {

			public void run() {
				deleteOldArt();

				Drawable image = getCachedArt(drawable, song);

				if (image == null) {
					File artFile = getArtFile(artFolder, song);
					Bitmap bitmap = null;
					if (isValid(artFile))
						bitmap = decode(artFile);
					else if (service.get().getArt(song, artFile)) {
						bitmap = decode(artFile);
						if (bitmap != null)
							write(bitmap, artFile);
					}

					if (bitmap != null) {
						image = new BitmapDrawable(artView.getResources(),
								bitmap);
						LayerDrawable layers = (LayerDrawable) activity
								.getResources().getDrawable(drawable);
						layers.setDrawableByLayerId(id.i_album_art, image);
						putCachedArt(drawable, song, layers);
						image = layers;
					}
				}

				final Drawable imageDrawable = image;
				activity.runOnUiThread(new Runnable() {

					public void run() {
						if (tag.equals(artView.getTag()))
							if (imageDrawable != null)
								updateDrawable(artView, imageDrawable);
							else
								updateDrawable(artView,
										getEmptyArt(drawable, activity));
					}
				});
			}
		});
	}
}
