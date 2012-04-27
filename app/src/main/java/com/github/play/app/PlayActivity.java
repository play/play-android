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
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static com.github.play.app.StatusService.EXTRA_UPDATE;
import static com.github.play.app.StatusService.UPDATE;
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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
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
import com.github.play.core.DequeueSongTask;
import com.github.play.core.FetchSettingsTask;
import com.github.play.core.FetchStatusTask;
import com.github.play.core.PlayPreferences;
import com.github.play.core.PlayService;
import com.github.play.core.QueueSubjectTask;
import com.github.play.core.Song;
import com.github.play.core.SongCallback;
import com.github.play.core.StarSongTask;
import com.github.play.core.StatusUpdate;
import com.github.play.core.StreamingInfo;
import com.github.play.core.UnstarSongTask;
import com.github.play.widget.NowPlayingViewWrapper;
import com.github.play.widget.PlayListAdapter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Activity to view what is playing and listen to music
 */
public class PlayActivity extends SherlockActivity implements SongCallback {

	/**
	 * Action for broadcasting that the queue has been updated
	 */
	public static final String ACTION_QUEUE = "com.github.play.action.QUEUE_UPDATE";

	private static final String TAG = "PlayActivity";

	private static final String STREAMING_INFO = "streamingInfo";

	private static final int REQUEST_SETTINGS = 1;

	private static final int REQUEST_SPEECH = 2;

	private final AtomicReference<PlayService> playService = new AtomicReference<PlayService>();

	private NowPlayingViewWrapper nowPlayingItemView;

	private PlayListAdapter playListAdapter;

	private boolean streaming = false;

	private MenuItem playItem;

	private MenuItem speakItem;

	private MenuItem refreshItem;

	private MenuItem searchItem;

	private PlayPreferences settings;

	private StreamingInfo streamingInfo;

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

	private final OnClickListener starListener = new OnClickListener() {

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

	private final OnItemLongClickListener dequeueListener = new OnItemLongClickListener() {

		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			Song song = (Song) parent.getItemAtPosition(position);
			dequeueSong(song);
			return true;
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(updateReceiver);
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

		ListView list = (ListView) findViewById(android.R.id.list);
		LayoutInflater inflater = getLayoutInflater();

		list.setFastScrollEnabled(true);
		list.setOnItemLongClickListener(dequeueListener);

		View nowPlayingView = inflater.inflate(layout.now_playing, null);
		list.addHeaderView(nowPlayingView, null, false);
		nowPlayingItemView = new NowPlayingViewWrapper(nowPlayingView,
				playService, starListener);

		list.addHeaderView(inflater.inflate(layout.queue_divider, null), null,
				false);

		playListAdapter = new PlayListAdapter(layout.queued,
				getLayoutInflater(), playService, starListener);
		list.setAdapter(playListAdapter);

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

		registerReceiver(updateReceiver, new IntentFilter(UPDATE));
		registerReceiver(queueReceiver, new IntentFilter(ACTION_QUEUE));
	}

	private void startStream() {
		if (!hasSettings())
			return;

		Log.d(TAG, "Starting stream");

		if (playItem != null) {
			playItem.setIcon(drawable.action_pause);
			playItem.setTitle(string.pause);
		}

		Context context = getApplicationContext();
		MusicStreamService.start(context, streamingInfo.streamUrl);
		StatusService.start(context, streamingInfo.pusherKey);

		streaming = true;
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
			refreshSongs();
			startStream();
		}
	}

	private void setMenuItemsEnabled(final boolean enabled) {
		if (playItem != null)
			playItem.setEnabled(enabled);
		if (speakItem != null)
			speakItem.setEnabled(enabled);
		if (refreshItem != null)
			refreshItem.setEnabled(enabled);
		if (searchItem != null)
			searchItem.setEnabled(enabled);
	}

	private void stopStream() {
		if (!hasSettings())
			return;

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
		if (isReady())
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

		refreshItem = optionsMenu.findItem(id.m_refresh);
		speakItem = optionsMenu.findItem(id.m_speak);
		searchItem = optionsMenu.findItem(id.m_search);

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

		Toast.makeText(
				getApplicationContext(),
				MessageFormat.format(
						getString(string.error_contacting_play_server),
						e.getMessage()), LENGTH_LONG).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
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
				Toast.makeText(getApplicationContext(),
						string.speech_not_recognized, LENGTH_SHORT).show();
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void starSong(final Song song) {
		if (!isReady())
			return;

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
		if (!isReady())
			return;

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

	private void dequeueSong(final Song song) {
		if (!isReady())
			return;

		final Builder builder = new Builder(this);
		builder.setCancelable(true);
		builder.setTitle(string.title_confirm_remove);
		builder.setMessage(MessageFormat.format(
				getString(string.message_confirm_remove), song.name));
		builder.setNegativeButton(android.R.string.no, null);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						Toast.makeText(
								getApplicationContext(),
								MessageFormat.format(
										getString(string.removing_song),
										song.name), LENGTH_SHORT).show();
						new DequeueSongTask(playService) {

							@Override
							protected void onPostExecute(IOException result) {
								super.onPostExecute(result);

								if (result != null)
									Toast.makeText(
											getApplicationContext(),
											MessageFormat
													.format(getString(string.removing_song_failed),
															song.name),
											LENGTH_SHORT).show();
								else
									refreshSongs();
							}
						}.execute(song);
					}
				});
		builder.show();
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

		stopStream();
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

		Toast.makeText(
				getApplicationContext(),
				MessageFormat.format(
						getString(string.adding_subject_to_the_queue), subject),
				LENGTH_SHORT).show();

		new QueueSubjectTask(playService) {

			@Override
			protected void onPostExecute(QueueSubjectResult result) {
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

				Context context = getApplicationContext();
				Toast.makeText(context, message, LENGTH_SHORT).show();

				refreshSongs();
				startStream();
			}

		}.execute(subject);

	}
}