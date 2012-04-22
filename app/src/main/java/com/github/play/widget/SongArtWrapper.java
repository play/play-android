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

import static com.github.kevinsawicki.http.HttpRequest.CHARSET_UTF8;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import com.github.play.core.PlayService;
import com.github.play.core.Song;
import com.github.play.widget.ItemListAdapter.ViewWrapper;

import java.io.File;
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
public class SongArtWrapper extends ViewWrapper<Song> {

	private static final int DIGEST_LENGTH = 40;

	private static final Executor EXECUTORS = Executors.newFixedThreadPool(1);

	private static final int MAX_RECENT = 50;

	private static final Map<String, Bitmap> RECENT_ART = new LinkedHashMap<String, Bitmap>(
			MAX_RECENT, 1.0F) {

		private static final long serialVersionUID = -3434208982358063608L;

		protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
			return size() >= MAX_RECENT;
		}
	};

	private static String digest(Song song) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		byte[] value;
		try {
			value = (song.artist + '#' + song.album).getBytes(CHARSET_UTF8);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
		byte[] digested = digest.digest(value);
		String hashed = new BigInteger(1, digested).toString(16);
		int padding = DIGEST_LENGTH - hashed.length();
		if (padding > 0) {
			char[] zeros = new char[padding];
			Arrays.fill(zeros, '0');
			hashed = new String(zeros) + hashed;
		}
		return hashed;
	}

	private static Bitmap getCachedArt(final Song song) {
		synchronized (RECENT_ART) {
			String digest = digest(song);
			return digest != null ? RECENT_ART.get(digest) : null;
		}
	}

	private static void putCachedArt(final Song song, final Bitmap bitmap) {
		if (bitmap == null)
			return;
		String digest = digest(song);
		if (digest == null)
			return;
		synchronized (RECENT_ART) {
			RECENT_ART.put(digest, bitmap);
		}
	}

	private final File artFolder;

	private final ImageView artView;

	private final AtomicReference<PlayService> service;

	/**
	 * Create view wrapper to display art for a {@link Song}
	 *
	 * @param view
	 * @param service
	 */
	public SongArtWrapper(final View view,
			final AtomicReference<PlayService> service) {
		artView = (ImageView) view;
		artFolder = new File(view.getContext().getCacheDir(), "art");
		if (!artFolder.exists())
			artFolder.mkdirs();
		this.service = service;
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

	public void update(final Song song) {
		Bitmap cachedBitmap = getCachedArt(song);
		if (cachedBitmap != null) {
			artView.setTag(null);
			artView.setImageBitmap(cachedBitmap);
			return;
		}

		artView.setTag(song.id);
		artView.setImageBitmap(null);

		EXECUTORS.execute(new Runnable() {

			public void run() {
				Bitmap bitmap = getCachedArt(song);
				if (bitmap == null) {
					File artFile = getArtFile(song);
					if (isValid(artFile) || service.get().getArt(song, artFile))
						bitmap = BitmapFactory.decodeFile(artFile
								.getAbsolutePath());
					putCachedArt(song, bitmap);
				}

				final Bitmap viewBitmap = bitmap;
				artView.post(new Runnable() {

					public void run() {
						if (!song.id.equals(artView.getTag()))
							return;

						artView.setTag(null);
						artView.setImageBitmap(viewBitmap);
					}
				});
			}
		});
	}
}
