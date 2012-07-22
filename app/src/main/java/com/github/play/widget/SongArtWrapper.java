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

	private static final String TAG = "SongArtWrapper";

	private static final int DIGEST_LENGTH = 40;

	private static final Executor EXECUTORS = Executors.newFixedThreadPool(1);

	private static final int MAX_RECENT = 50;

	private static final int MAX_SIZE_DP = 60;

	private static final MessageDigest SHA1_DIGEST;

	static {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			digest = null;
		}
		SHA1_DIGEST = digest;
	}

	private static final Map<String, Drawable> RECENT_ART = new LinkedHashMap<String, Drawable>(
			MAX_RECENT, 1.0F) {

		private static final long serialVersionUID = -3434208982358063608L;

		protected boolean removeEldestEntry(Map.Entry<String, Drawable> eldest) {
			return size() >= MAX_RECENT;
		}
	};

	private static String digest(Song song) {
		if (SHA1_DIGEST == null)
			return null;

		byte[] value;
		try {
			value = (song.artist + '#' + song.album).getBytes(CHARSET_UTF8);
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

	private static Drawable getCachedArt(final Song song) {
		final String digest = digest(song);
		if (digest != null)
			synchronized (RECENT_ART) {
				return RECENT_ART.get(digest);
			}
		else
			return null;
	}

	private static void putCachedArt(final Song song, final Drawable bitmap) {
		if (bitmap == null)
			return;

		final String digest = digest(song);
		if (digest != null)
			synchronized (RECENT_ART) {
				RECENT_ART.put(digest, bitmap);
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

	private final File artFolder;

	private final AtomicReference<PlayService> service;

	private final int maxSize;

	private final Drawable transparent;

	/**
	 * Create view wrapper to display art for a {@link Song}
	 *
	 * @param context
	 * @param service
	 */
	public SongArtWrapper(Context context,
			final AtomicReference<PlayService> service) {
		artFolder = new File(context.getCacheDir(), "art");
		if (!artFolder.exists())
			artFolder.mkdirs();
		this.service = service;
		Resources resources = context.getResources();
		maxSize = Math.round(resources.getDisplayMetrics().density
				* MAX_SIZE_DP + 0.5F);
		transparent = resources.getDrawable(android.R.color.transparent);
	}

	/**
	 * Get art file for song
	 *
	 * @param song
	 * @return file
	 */
	protected File getArtFile(final Song song) {
		return new File(artFolder, digest(song) + ".png");
	}

	/**
	 * Is file non-null, existent and non-empty?
	 *
	 * @param file
	 * @return true if valid, false otherwise
	 */
	protected boolean isValid(final File file) {
		return file != null && file.exists() && file.length() > 0;
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
		LayerDrawable layers = (LayerDrawable) view.getDrawable();
		if (layers != null)
			if (art != null)
				layers.setDrawableByLayerId(id.i_album_art, art);
			else
				layers.setDrawableByLayerId(id.i_album_art, transparent);
		view.invalidate();
	}

	/**
	 * Update view with art for song album
	 *
	 * @param artView
	 * @param song
	 */
	public void update(final ImageView artView, final Song song) {
		if (song == null) {
			updateDrawable(artView, null);
			return;
		}

		Drawable cachedBitmap = getCachedArt(song);
		if (cachedBitmap != null) {
			updateDrawable(artView, cachedBitmap);
			return;
		}

		update(artView, null);
		artView.setTag(song.id);

		EXECUTORS.execute(new Runnable() {

			public void run() {
				Drawable image = getCachedArt(song);

				if (image == null) {
					File artFile = getArtFile(song);
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
						putCachedArt(song, image);
					}
				}

				final Drawable imageDrawable = image;
				artView.post(new Runnable() {

					public void run() {
						if (song.id.equals(artView.getTag()))
							updateDrawable(artView, imageDrawable);
					}
				});
			}
		});
	}
}
