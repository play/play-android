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
package com.github.play.app;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED;
import static android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

/**
 * Service that plays music
 */
public class MusicStreamService extends Service implements OnPreparedListener,
		OnErrorListener {

	/**
	 * Action to use for broadcasting updates
	 */
	public static final String UPDATE = "com.github.play.action.STREAMING_UPDATE";

	/**
	 * Intent extra denoting whether music is currently streaming
	 */
	public static final String EXTRA_STREAMING = "streaming";

	/**
	 * Start streaming service to given URL
	 *
	 * @param context
	 * @param url
	 */
	public static void start(final Context context, final String url) {
		Intent intent = new Intent(ACTION);
		if (!TextUtils.isEmpty(url))
			intent.putExtra(EXTRA_URL, url);
		context.startService(intent);
	}

	/**
	 * Start streaming service
	 *
	 * @param context
	 */
	public static void start(final Context context) {
		start(context, null);
	}

	/**
	 * Stop service
	 *
	 * @param context
	 */
	public static void stop(final Context context) {
		context.stopService(new Intent(ACTION));
	}

	/**
	 * Action name for this service
	 */
	private static final String ACTION = "com.github.play.action.STREAM";

	private static final String EXTRA_URL = "url";

	private static final String TAG = "MusicStreamService";

	private MediaPlayer player;

	private boolean prepared;

	private String url;

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.d(TAG, "Destroying music stream service");

		if (player != null) {
			player.release();
			player = null;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, "Creating music stream service");

		player = new MediaPlayer();
		player.setOnPreparedListener(this);
		player.setOnErrorListener(this);
		player.setAudioStreamType(STREAM_MUSIC);
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		int result = super.onStartCommand(intent, flags, startId);

		if (intent == null)
			return result;

		String intentUrl = intent.getStringExtra(EXTRA_URL);
		if (!TextUtils.isEmpty(intentUrl) && !intentUrl.equals(url)) {
			prepareAsync(intentUrl);
			return result;
		}

		broadcastStatus(prepared);

		return result;
	}

	private void broadcastStatus(final boolean streaming) {
		Intent intent = new Intent(UPDATE);
		intent.putExtra(EXTRA_STREAMING, streaming);
		sendBroadcast(intent);
	}

	/**
	 * Prepare a connection to the given URL
	 *
	 * @param url
	 */
	private void prepareAsync(final String url) {
		Log.d(TAG, "Preparing streaming connection to: " + url);

		try {
			player.setDataSource(url);
			this.url = url;
			player.prepareAsync();
		} catch (IOException e) {
			Log.d(TAG, "Exception configuring streaming", e);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "Exception configuring streaming", e);
		} catch (IllegalStateException e) {
			Log.d(TAG, "Exception configuring streaming", e);
		} catch (SecurityException e) {
			Log.d(TAG, "Exception configuring streaming", e);
		}
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	public void onPrepared(final MediaPlayer mp) {
		Log.d(TAG, "Media player stream prepared");
		try {
			mp.start();
			prepared = true;
			broadcastStatus(true);
		} catch (IllegalStateException e) {
			Log.d(TAG, "Starting media player failed", e);
		}
	}

	public boolean onError(final MediaPlayer mp, final int what, final int extra) {
		switch (what) {
		case MEDIA_ERROR_SERVER_DIED:
			Log.d(TAG, "Media server died");
			break;
		case MEDIA_ERROR_UNKNOWN:
			Log.d(TAG, "Unknown media player error");
			break;
		default:
			Log.d(TAG, "Media player error: " + what + " Extra: " + extra);
			break;
		}

		prepared = false;
		broadcastStatus(false);
		return false;
	}
}
