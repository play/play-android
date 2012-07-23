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

import static android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH;
import static android.speech.RecognizerIntent.EXTRA_CALLING_PACKAGE;
import static android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL;
import static android.speech.RecognizerIntent.EXTRA_MAX_RESULTS;
import static android.speech.RecognizerIntent.EXTRA_PROMPT;
import static android.speech.RecognizerIntent.EXTRA_RESULTS;
import static android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
import static com.github.play.app.MusicStreamService.EXTRA_STREAMING;
import static com.github.play.app.StatusService.EXTRA_UPDATE;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.LightDialog;
import com.github.kevinsawicki.wishlist.ViewFinder;
import com.github.kevinsawicki.wishlist.ViewUtils;
import com.github.play.R.drawable;
import com.github.play.R.id;
import com.github.play.R.layout;
import com.github.play.R.menu;
import com.github.play.R.string;
import com.github.play.core.DequeueSongTask;
import com.github.play.core.FetchSettingsTask;
import com.github.play.core.FetchStatusTask;
import com.github.play.core.PlayPreferences;
import com.github.play.core.PlayService;
import com.github.play.core.QueueStarsTask;
import com.github.play.core.QueueSubjectTask;
import com.github.play.core.Song;
import com.github.play.core.SongCallback;
import com.github.play.core.SongResult;
import com.github.play.core.StarSongTask;
import com.github.play.core.StatusUpdate;
import com.github.play.core.StreamingInfo;
import com.github.play.core.UnstarSongTask;
import com.github.play.widget.PlayListAdapter;
import com.github.play.widget.SongArtWrapper;
import com.github.play.widget.Toaster;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Activity to view what is playing and listen to music
 */
public class PlayActivity extends SherlockActivity implements SongCallback,
		OnItemClickListener {

	/**
	 * Action for broadcasting that the queue has been updated
	 */
	public static final String ACTION_QUEUE = "com.github.play.action.QUEUE_UPDATE";

	private static final String TAG = "PlayActivity";

	private static final String STREAMING_INFO = "streamingInfo";

	private static final int REQUEST_SETTINGS = 1;

	private static final int REQUEST_SPEECH = 2;

	private ListView listView;

	private View loadingView;

	private final AtomicReference<PlayService> playService = new AtomicReference<PlayService>();

	private View nowPlayingView;

	private PlayListAdapter playListAdapter;

	private boolean streaming;

	private boolean queueEmpty = true;

	private MenuItem playItem;

	private MenuItem speakItem;

	private MenuItem refreshItem;

	private MenuItem searchItem;

	private MenuItem playStarsItem;

	private PlayPreferences settings;

	private StreamingInfo streamingInfo;

	private Song nowPlaying;

	private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			StatusUpdate update = (StatusUpdate) intent
					.getSerializableExtra(EXTRA_UPDATE);
			onUpdate(update.playing, update.queued);
		}
	};

	private final BroadcastReceiver queueReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			runOnUiThread(new Runnable() {

				public void run() {
					refreshSongs();
				}
			});
		}
	};

	private final BroadcastReceiver streamReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			final boolean streaming = intent.getBooleanExtra(EXTRA_STREAMING,
					false);
			runOnUiThread(new Runnable() {

				public void run() {
					setStreaming(streaming);
				}
			});
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (streaming && hasSettings())
			StatusService.start(getApplicationContext(),
					streamingInfo.pusherKey, true);
		else
			StatusService.stop(getApplicationContext());

		unregisterReceiver(updateReceiver);
		unregisterReceiver(streamReceiver);
		unregisterReceiver(queueReceiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(STREAMING_INFO, streamingInfo);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.main);

		loadingView = findViewById(id.ll_loading);

		playListAdapter = new PlayListAdapter(this, layout.queued, playService);

		listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(this);

		LayoutInflater inflater = getLayoutInflater();

		nowPlayingView = inflater.inflate(layout.now_playing, null);
		nowPlayingView.setLongClickable(true);
		playListAdapter.initialize(nowPlayingView);
		listView.addHeaderView(nowPlayingView, null, false);
		listView.setAdapter(playListAdapter);

		if (savedInstanceState != null)
			streamingInfo = (StreamingInfo) savedInstanceState
					.getSerializable(STREAMING_INFO);

		settings = new PlayPreferences(this);

		if (hasSettings()) {
			playService.set(new PlayService(settings.getUrl(), settings
					.getToken()));
			load();
		} else
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQUEST_SETTINGS);

		registerReceiver(updateReceiver, new IntentFilter(StatusService.UPDATE));
		registerReceiver(streamReceiver, new IntentFilter(
				MusicStreamService.UPDATE));
		registerReceiver(queueReceiver, new IntentFilter(ACTION_QUEUE));
	}

	private void setStreaming(final boolean streaming) {
		this.streaming = streaming;
		updatePlayMenuItem();
	}

	private void updatePlayMenuItem() {
		if (playItem == null)
			return;

		if (streaming)
			playItem.setIcon(drawable.action_pause).setTitle(string.pause);
		else
			playItem.setIcon(drawable.action_play).setTitle(string.play);
	}

	private void startStream() {
		if (!hasSettings() || streamingInfo == null)
			return;

		Log.d(TAG, "Starting stream");

		setStreaming(true);

		Context context = getApplicationContext();
		MusicStreamService.start(context, streamingInfo.streamUrl);

		refreshSongs();
	}

	private void load() {
		if (streamingInfo == null && hasSettings())
			new FetchSettingsTask(playService) {

				protected void onPostExecute(PlaySettings result) {
					if (result.streamingInfo != null) {
						streamingInfo = result.streamingInfo;
						load();
					} else
						onError(result.exception);
				}

			}.execute();
		else if (isReady()) {
			setMenuItemsEnabled(true);
			Context context = getApplicationContext();
			MusicStreamService.start(context);
			StatusService.start(context, streamingInfo.pusherKey, false);
			refreshSongs();
		}
	}

	private void setMenuItemsEnabled(final boolean enabled) {
		if (playItem != null)
			playItem.setEnabled(enabled && streamingInfo != null);
		if (speakItem != null)
			speakItem.setEnabled(enabled);
		if (refreshItem != null)
			refreshItem.setEnabled(enabled);
		if (searchItem != null)
			searchItem.setEnabled(enabled);
		if (playStarsItem != null)
			playStarsItem.setEnabled(enabled);
	}

	private void stopStream() {
		if (!hasSettings())
			return;

		Log.d(TAG, "Stopping stream");

		setStreaming(false);

		MusicStreamService.stop(getApplicationContext());
	}

	public void onUpdate(final Song playing, final Song[] queued) {
		runOnUiThread(new Runnable() {

			public void run() {
				nowPlaying = playing;
				updateSongs(playing, queued);
			}
		});
	}

	private void updateSongs(final Song playing, final Song[] queued) {
		queueEmpty = playing == null && (queued == null || queued.length == 0);

		playListAdapter.update(-1, nowPlayingView, playing);
		playListAdapter.setItems(queued);

		ViewUtils.setGone(loadingView, true);
		ViewUtils.setGone(listView, false);
	}

	private void refreshSongs() {
		if (!isReady())
			return;

		if (queueEmpty) {
			ViewUtils.setGone(loadingView, false);
			ViewUtils.setGone(listView, true);
		}

		new FetchStatusTask(playService, this).execute();
	}

	private boolean hasSettings() {
		return settings.getUrl() != null && settings.getToken() != null;
	}

	private boolean isReady() {
		return hasSettings() && playService.get() != null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case id.m_pause:
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
			return true;
		case id.m_speak:
			promptForSpeech();
			return true;
		case id.m_search:
			onSearchRequested();
			return true;
		case id.m_play_stars:
			playStars();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		getSupportMenuInflater().inflate(menu.main, optionsMenu);

		playItem = optionsMenu.findItem(id.m_pause);
		updatePlayMenuItem();

		refreshItem = optionsMenu.findItem(id.m_refresh);
		speakItem = optionsMenu.findItem(id.m_speak);
		searchItem = optionsMenu.findItem(id.m_search);
		playStarsItem = optionsMenu.findItem(id.m_play_stars);

		if (isReady())
			setMenuItemsEnabled(true);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		List<ResolveInfo> activities = getPackageManager()
				.queryIntentActivities(new Intent(ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.isEmpty())
			menu.removeItem(id.m_speak);

		return true;
	}

	public void onError(IOException e) {
		Log.d(TAG, "Play server exception", e);

		ViewUtils.setGone(loadingView, true);

		Toaster.showLong(this, string.error_contacting_play_server,
				e.getMessage());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
			stopStream();
			if (hasSettings()) {
				playService.set(new PlayService(settings.getUrl(), settings
						.getToken()));
				streamingInfo = null;
				load();
			}
			return;
		}

		if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK
				&& data != null) {
			ArrayList<String> results = data
					.getStringArrayListExtra(EXTRA_RESULTS);
			if (!results.isEmpty())
				selectSubject(results);
			else
				Toaster.showShort(this, string.speech_not_recognized);
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void starSong(final Song song) {
		if (!isReady())
			return;

		Toaster.showShort(this, string.starring_song, song.name);

		new StarSongTask(playService) {

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null)
					Toaster.showLong(PlayActivity.this, string.starring_failed,
							song.name);
				else
					refreshSongs();
			}
		}.execute(song);
	}

	private void unstarSong(final Song song) {
		if (!isReady())
			return;

		Toaster.showShort(this, string.unstarring_song, song.name);

		new UnstarSongTask(playService) {

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null)
					Toaster.showShort(PlayActivity.this,
							string.unstarring_failed, song.name);
				else
					refreshSongs();
			}
		}.execute(song);
	}

	private void dequeueSong(final Song song) {
		if (!isReady())
			return;

		Toaster.showShort(PlayActivity.this, string.removing_song, song.name);
		new DequeueSongTask(playService) {

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null)
					Toaster.showShort(PlayActivity.this,
							string.removing_song_failed, song.name);
				else
					refreshSongs();
			}
		}.execute(song);
	}

	private void playStars() {
		if (!isReady())
			return;

		new QueueStarsTask(playService) {

			@Override
			protected void onPostExecute(SongResult result) {
				super.onPostExecute(result);

				String message;
				if (result.exception != null)
					message = getString(string.queueing_stars_failed);
				else if (result.queued.length > 1)
					message = MessageFormat.format(
							getString(string.multiple_songs_queued),
							result.queued.length);
				else if (result.queued.length == 1)
					message = getString(string.single_song_queued);
				else
					message = getString(string.no_songs_found);

				Toaster.showShort(PlayActivity.this, message);

				refreshSongs();
			}
		}.execute();
	}

	private void promptForSpeech() {
		if (!isReady())
			return;

		Intent intent = new Intent(ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(EXTRA_CALLING_PACKAGE, getClass().getPackage()
				.getName());
		intent.putExtra(EXTRA_PROMPT, getString(string.speech_prompt));
		intent.putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(EXTRA_MAX_RESULTS, 5);

		startActivityForResult(intent, REQUEST_SPEECH);
	}

	private void selectSubject(List<String> results) {
		Builder builder = new Builder(this);
		builder.setCancelable(true);
		final String[] subjects = results.toArray(new String[results.size()]);
		builder.setSingleChoiceItems(
				results.toArray(new String[results.size()]), -1,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						queueSubject(subjects[which]);
					}
				});
		builder.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	private void queueSubject(final String subject) {
		if (!isReady())
			return;

		Toaster.showShort(this, string.adding_subject_to_the_queue, subject);

		new QueueSubjectTask(playService) {

			@Override
			protected void onPostExecute(SongResult result) {
				super.onPostExecute(result);

				String message;
				if (result.exception != null)
					message = MessageFormat.format(
							getString(string.queueing_subject_failed), subject);
				else if (result.queued.length > 1)
					message = MessageFormat.format(
							getString(string.multiple_songs_queued),
							result.queued.length);
				else if (result.queued.length == 1)
					message = getString(string.single_song_queued);
				else
					message = getString(string.no_songs_found);

				Toaster.showShort(PlayActivity.this, message);

				refreshSongs();
			}

		}.execute(subject);

	}

	public void onItemClick(AdapterView<?> listView, View view, int position,
			long itemId) {
		final Song song;
		if (position == 0)
			song = nowPlaying;
		else
			song = (Song) listView.getItemAtPosition(position);
		if (song == null)
			return;

		final LightDialog dialog = LightDialog.create(this);
		View dialogView = getLayoutInflater().inflate(layout.song_dialog, null);
		ViewFinder finder = new ViewFinder(dialogView);
		finder.setText(id.tv_album, song.album);
		finder.setText(id.tv_artist, song.artist);
		finder.setText(id.tv_song, song.name);
		if (song.starred) {
			finder.setText(id.tv_star, string.unstar_this_song);
			finder.setDrawable(id.iv_star_icon, drawable.action_unstar);
		} else {
			finder.setText(id.tv_star, string.star_this_song);
			finder.setDrawable(id.iv_star_icon, drawable.action_star);
		}
		finder.onClick(id.rl_star_area, new Runnable() {

			public void run() {
				dialog.dismiss();
				if (song.starred)
					unstarSong(song);
				else
					starSong(song);
			}
		});
		finder.onClick(id.rl_remove_area, new Runnable() {

			public void run() {
				dialog.dismiss();
				dequeueSong(song);
			}
		});
		new SongArtWrapper(this, playService).update(
				finder.imageView(id.iv_art), song);
		dialog.setView(dialogView);
		dialog.show();
	}
}