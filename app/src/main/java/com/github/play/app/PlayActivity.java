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

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static com.github.play.app.StatusService.UPDATE;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.play.R.drawable;
import com.github.play.R.id;
import com.github.play.R.layout;
import com.github.play.R.menu;
import com.github.play.R.string;
import com.github.play.core.FetchSettingsTask;
import com.github.play.core.FetchStatusTask;
import com.github.play.core.PlayPreferences;
import com.github.play.core.PlayService;
import com.github.play.core.Song;
import com.github.play.core.SongCallback;
import com.github.play.core.StarSongTask;
import com.github.play.core.StatusUpdate;
import com.github.play.core.UnstarSongTask;
import com.github.play.widget.NowPlayingViewWrapper;
import com.github.play.widget.PlayListAdapter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Activity to view what is playing and listen to music
 */
public class PlayActivity extends SherlockActivity implements SongCallback {

	private static final String TAG = "PlayActivity";

	private static final String STREAM_URL = "streamUrl";

	private static final String APPLICATION_KEY = "applicationKey";

	private static final int REQUEST_SETTINGS = 1;

	private final AtomicReference<PlayService> playService = new AtomicReference<PlayService>();

	private NowPlayingViewWrapper nowPlayingItemView;

	private PlayListAdapter playListAdapter;

	private boolean streaming = false;

	private MenuItem playItem;

	private PlayPreferences settings;

	private String streamUrl;

	private String applicationKey;

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			StatusUpdate update = (StatusUpdate) intent
					.getSerializableExtra("update");
			onUpdate(update.playing, update.queued);
		}
	};

	private OnClickListener starListener = new OnClickListener() {

		public void onClick(View v) {
			Object tag = v.getTag();
			if (!(tag instanceof Song))
				return;

			Song song = (Song) tag;
			if (song.starred)
				unstarSong(song);
			else
				starSong(song);
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(receiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(STREAM_URL, streamUrl);
		outState.putSerializable(APPLICATION_KEY, applicationKey);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.main);

		ListView list = (ListView) findViewById(android.R.id.list);

		View nowPlayingView = findViewById(id.now_playing);
		nowPlayingItemView = new NowPlayingViewWrapper(nowPlayingView,
				playService, starListener);

		playListAdapter = new PlayListAdapter(layout.queued,
				getLayoutInflater(), playService, starListener);
		list.setAdapter(playListAdapter);

		if (savedInstanceState != null) {
			streamUrl = savedInstanceState.getString(STREAM_URL);
			applicationKey = savedInstanceState.getString(APPLICATION_KEY);
		}

		settings = new PlayPreferences(this);

		if (settings.getUrl() != null && settings.getLogin() != null) {
			playService.set(new PlayService(settings.getUrl(), settings
					.getLogin()));
			refreshSongs();
		} else
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_SETTINGS);

		if (streamUrl == null || applicationKey == null)
			new FetchSettingsTask(playService) {

				protected void onPostExecute(PlaySettings result) {
					if (result.streamUrl != null) {
						streamUrl = result.streamUrl;
						applicationKey = result.applicationKey;
						startStream();
					} else
						onError(result.exception);
				}

			}.execute();
		else
			startStream();

		registerReceiver(receiver, new IntentFilter(UPDATE));
	}

	private void startStream() {
		Log.d(TAG, "Starting stream");

		if (playItem != null) {
			playItem.setIcon(drawable.action_pause);
			playItem.setTitle(string.pause);
		}

		Context context = getApplicationContext();
		MusicStreamService.start(context, streamUrl);
		StatusService.start(context, applicationKey);

		streaming = true;
	}

	private void stopStream() {
		Log.d(TAG, "Stopping stream");

		if (playItem != null) {
			playItem.setIcon(drawable.action_play);
			playItem.setTitle(string.play);
		}

		Context context = getApplicationContext();
		MusicStreamService.stop(context);
		StatusService.stop(context);

		streaming = false;
	}

	public void onUpdate(final Song playing, final Song[] queued) {
		runOnUiThread(new Runnable() {

			public void run() {
				updateSongs(playing, queued);
			}
		});
	}

	private void updateSongs(Song playing, Song[] queued) {
		nowPlayingItemView.update(playing);
		playListAdapter.setItems(queued);
	}

	private void refreshSongs() {
		new FetchStatusTask(playService, this).execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case id.m_pause:
			if (settings.getUrl() != null && settings.getLogin() != null)
				if (streaming)
					stopStream();
				else
					startStream();
			return true;
		case id.m_refresh:
			refreshSongs();
			return true;
		case id.m_settings:
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_SETTINGS);
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		getSupportMenuInflater().inflate(menu.main, optionsMenu);
		playItem = optionsMenu.findItem(id.m_pause);

		if (streaming) {
			playItem.setIcon(drawable.action_pause);
			playItem.setTitle(string.pause);
		} else {
			playItem.setIcon(drawable.action_play);
			playItem.setTitle(string.play);
		}

		return true;
	}

	public void onError(IOException e) {
		Log.d(TAG, "Play server exception", e);

		Toast.makeText(
				getApplicationContext(),
				MessageFormat.format(
						getString(string.error_contacting_play_server),
						e.getMessage()), LENGTH_LONG).show();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
			playService.set(new PlayService(settings.getUrl(), settings
					.getLogin()));
			startStream();
			refreshSongs();
			return;
		} else
			super.onActivityResult(requestCode, resultCode, data);
	}

	private void starSong(final Song song) {
		Toast.makeText(
				getApplicationContext(),
				MessageFormat
						.format(getString(string.starring_song), song.name),
				LENGTH_SHORT).show();
		new StarSongTask(playService) {

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null)
					Toast.makeText(
							getApplicationContext(),
							MessageFormat.format(
									getString(string.starring_failed),
									song.name), LENGTH_LONG).show();
				else
					refreshSongs();
			}
		}.execute(song);
	}

	private void unstarSong(final Song song) {
		Toast.makeText(
				getApplicationContext(),
				MessageFormat.format(getString(string.unstarring_song),
						song.name), LENGTH_SHORT).show();
		new UnstarSongTask(playService) {

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null)
					Toast.makeText(
							getApplicationContext(),
							MessageFormat.format(
									getString(string.unstarring_failed),
									song.name), LENGTH_SHORT).show();
				else
					refreshSongs();
			}
		}.execute(song);
	}
}