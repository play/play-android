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

import static android.app.Notification.FLAG_ONGOING_EVENT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.emorym.android_pusher.Pusher;
import com.emorym.android_pusher.PusherCallback;
import com.github.play.R.drawable;
import com.github.play.core.Song;
import com.github.play.core.SongPusher;
import com.github.play.core.StatusUpdate;
import com.github.play.widget.SongArtWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service to receive push notifications about the currently playing song and
 * queued songs
 */
public class StatusService extends Service {

	/**
	 * Action to use for broadcasting updates
	 */
	public static final String UPDATE = "com.github.play.action.STATUS_UPDATE";

	/**
	 * Intent extra key to a {@link StatusUpdate} handle
	 */
	public static final String EXTRA_UPDATE = "update";

	/**
	 * Start service with application key
	 *
	 * @param context
	 * @param applicationKey
	 * @param nowPlaying
	 */
	public static void start(final Context context,
			final String applicationKey, final Song nowPlaying) {
		Intent intent = new Intent(ACTION);
		intent.putExtra(EXTRA_KEY, applicationKey);
		intent.putExtra(EXTRA_SONG, nowPlaying);
		context.startService(intent);
	}

	/**
	 * Start service with application key
	 *
	 * @param context
	 * @param applicationKey
	 * @param sendNotification
	 * @param nowPlaying
	 */
	public static void start(final Context context,
			final String applicationKey, final boolean sendNotification,
			final Song nowPlaying) {
		Intent intent = new Intent(ACTION);
		intent.putExtra(EXTRA_KEY, applicationKey);
		intent.putExtra(EXTRA_NOTIFY, sendNotification);
		intent.putExtra(EXTRA_SONG, nowPlaying);
		context.startService(intent);
	}

	/**
	 * Start service with application key
	 *
	 * @param context
	 */
	public static void stop(final Context context) {
		context.stopService(new Intent(ACTION));
	}

	/**
	 * Action to use for intents
	 */
	private static final String ACTION = "com.github.play.action.STATUS";

	private static final String EXTRA_KEY = "applicationKey";

	private static final String EXTRA_NOTIFY = "notify";

	private static final String EXTRA_SONG = "song";

	private static final String TAG = "StatusService";

	private static Song parseSong(final JSONObject object) {
		String id = object.optString("id");
		if (id == null)
			id = "";

		String artist = object.optString("artist");
		if (artist == null)
			artist = "";

		String album = object.optString("album");
		if (album == null)
			album = "";

		String name = object.optString("name");
		if (name == null)
			name = "";

		return new Song(id, name, artist, album, object.optBoolean("starred"));
	}

	private final Executor backgroundThread = Executors.newFixedThreadPool(1);

	private final PusherCallback callback = new PusherCallback() {

		public void onEvent(JSONObject eventData) {
			JSONObject nowPlaying = eventData.optJSONObject("now_playing");
			if (nowPlaying == null)
				return;

			JSONArray upcomingSongs = eventData.optJSONArray("songs");
			if (upcomingSongs == null)
				return;

			Song playing = parseSong(nowPlaying);

			List<Song> parsedSongs = new ArrayList<Song>(upcomingSongs.length());
			for (int i = 0; i < upcomingSongs.length(); i++) {
				JSONObject song = upcomingSongs.optJSONObject(i);
				if (song == null)
					continue;
				parsedSongs.add(parseSong(song));
			}
			Song[] queued = parsedSongs.toArray(new Song[parsedSongs.size()]);

			Intent intent = new Intent(UPDATE);
			intent.putExtra(EXTRA_UPDATE, new StatusUpdate(playing, queued));
			sendBroadcast(intent);

			updateNotification(playing);
		}
	};

	private Pusher pusher;

	private String applicationKey;

	private boolean sendNotification;

	private boolean notificationSent;

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.d(TAG, "Destroying status service");

		destroyPusher(pusher);
		stopForeground(true);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, "Creating status service");
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		if (intent != null) {
			String intentKey = intent.getStringExtra(EXTRA_KEY);
			if (!TextUtils.isEmpty(intentKey)
					&& !intentKey.equals(applicationKey)) {
				destroyPusher(pusher);
				createPusher(intentKey);
			}

			boolean updateNotification = intent.hasExtra(EXTRA_NOTIFY);
			if (updateNotification)
				sendNotification = intent.getBooleanExtra(EXTRA_NOTIFY, false);
			Song song = (Song) intent.getSerializableExtra(EXTRA_SONG);
			if (updateNotification && !sendNotification) {
				clearNotification();
			} else if (song != null)
				updateNotification(song);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	private void destroyPusher(final Pusher pusher) {
		if (pusher != null)
			backgroundThread.execute(new Runnable() {

				public void run() {
					pusher.disconnect();
				}
			});
	}

	private void createPusher(String applicationKey) {
		this.applicationKey = applicationKey;

		final Pusher pusher = new SongPusher(applicationKey);
		backgroundThread.execute(new Runnable() {

			public void run() {
				pusher.subscribe("now_playing_updates").bind(
						"update_now_playing", callback);
			}
		});
		this.pusher = pusher;
	}

	private CharSequence getTickerText(final Song song) {
		StringBuilder text = new StringBuilder();
		text.append(song.name);
		if (!TextUtils.isEmpty(song.artist))
			text.append(" by ").append(song.artist);
		return text;
	}

	private CharSequence getContentText(final Song song) {
		StringBuilder text = new StringBuilder();
		text.append(song.name);
		if (!TextUtils.isEmpty(song.album))
			text.append(" from ").append(song.album);
		return text;
	}

	private Notification createBigNotification(final Context context,
			final Song song, final PendingIntent intent) {
		Builder builder = new Builder(context);
		builder.setOngoing(true);
		builder.setSmallIcon(drawable.notification);
		builder.setTicker(getTickerText(song));
		builder.setContentTitle(song.artist);
		builder.setLargeIcon(SongArtWrapper.getCachedArt(context, song));
		CharSequence contextText = getContentText(song);
		builder.setContentText(contextText);
		builder.setContentIntent(intent);
		return new BigTextStyle(builder).bigText(contextText).build();
	}

	@SuppressWarnings("deprecation")
	private Notification createNotification(final Context context,
			final Song song, final PendingIntent intent) {
		Notification notification = new Notification();
		notification.icon = drawable.notification;
		notification.flags |= FLAG_ONGOING_EVENT;
		notification.tickerText = getTickerText(song);
		if (SDK_INT >= HONEYCOMB)
			notification.largeIcon = SongArtWrapper.getCachedArt(context, song);
		notification.setLatestEventInfo(context, song.artist,
				getContentText(song), intent);
		return notification;
	}

	private NotificationManager getNotificationManager() {
		return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	private void clearNotification() {
		if (notificationSent) {
			stopForeground(true);
			notificationSent = false;
		}
	}

	private void updateNotification(Song song) {
		if (!sendNotification)
			return;

		Context context = getApplicationContext();
		PendingIntent intent = PendingIntent.getActivity(context, 0,
				new Intent(context, PlayActivity.class), FLAG_UPDATE_CURRENT);

		Notification notification;
		if (SDK_INT >= JELLY_BEAN)
			notification = createBigNotification(context, song, intent);
		else
			notification = createNotification(context, song, intent);

		if (notificationSent)
			getNotificationManager().notify(1, notification);
		else {
			notificationSent = true;
			startForeground(1, notification);
		}
	}
}
