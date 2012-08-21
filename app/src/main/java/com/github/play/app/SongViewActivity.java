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

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static com.github.play.app.PlayActivity.ACTION_QUEUE;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.Toaster;
import com.github.kevinsawicki.wishlist.ViewFinder;
import com.github.kevinsawicki.wishlist.ViewUtils;
import com.github.play.R.id;
import com.github.play.R.layout;
import com.github.play.R.menu;
import com.github.play.R.string;
import com.github.play.core.PlayPreferences;
import com.github.play.core.PlayService;
import com.github.play.core.QueueSongsTask;
import com.github.play.core.Song;
import com.github.play.core.SongResult;
import com.github.play.widget.SearchListAdapter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base activity to display a list of songs and optionally queue them
 */
public abstract class SongViewActivity extends SherlockActivity implements
		OnItemClickListener {

	private static final String TAG = "SongViewActivity";

	/**
	 * Play service reference
	 */
	protected final AtomicReference<PlayService> service = new AtomicReference<PlayService>();

	/**
	 * List view
	 */
	protected ListView listView;

	private View loadingView;

	private SearchListAdapter adapter;

	private ActionMode actionMode;

	private Callback selectionModeCallback = new Callback() {

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			int count = adapter.getSelectedCount();
			if (count > 0) {
				mode.setTitle(MessageFormat.format(
						getString(string.multiple_selected), count));
				return false;
			} else if (count == 1) {
				mode.setTitle(string.single_selected);
				return false;
			} else {
				mode.finish();
				return true;
			}
		}

		public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;
			unselectAllSongs();
		}

		public boolean onCreateActionMode(ActionMode mode, Menu actionMenu) {
			mode.getMenuInflater().inflate(menu.add, actionMenu);
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case id.m_add:
				queueSelectedSongs();
				mode.finish();
				return true;
			default:
				return false;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.search);

		final ViewFinder finder = new ViewFinder(this);
		loadingView = finder.find(id.ll_loading);
		loadingView.post(new Runnable() {

			public void run() {
				((AnimationDrawable) finder.find(id.v_loading).getBackground())
						.start();
			}
		});

		listView = finder.find(android.R.id.list);
		listView.setOnItemClickListener(this);
		adapter = new SearchListAdapter(this, service);
		listView.setAdapter(adapter);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PlayPreferences settings = new PlayPreferences(this);
		service.set(new PlayService(settings.getUrl(), settings.getToken()));

		refreshSongs();
	}

	/**
	 * Show/hide loading view
	 *
	 * @param loading
	 */
	protected void showLoading(final boolean loading) {
		ViewUtils.setGone(loadingView, !loading);
		ViewUtils.setGone(listView, loading);
	}

	/**
	 * Refresh songs being displayed
	 */
	protected abstract void refreshSongs();

	/**
	 * Display loaded songs
	 *
	 * @param result
	 */
	protected void displaySongs(final SongResult result) {
		if (result.exception == null)
			adapter.setSongs(result);
		else {
			Log.d(TAG, "Searching songs failed", result.exception);
			Toaster.showLong(SongViewActivity.this, string.search_failed);
		}

		showLoading(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, PlayActivity.class);
			intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		case id.m_refresh:
			refreshSongs();
			return true;
		case id.m_select_all:
			selectAllSongs();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Select all songs
	 */
	protected void selectAllSongs() {
		for (int i = 0; i < adapter.getCount(); i++)
			adapter.setSelected(i, true);

		adapter.notifyDataSetChanged();
		startSelectionMode();
	}

	/**
	 * Select all songs
	 */
	protected void unselectAllSongs() {
		for (int i = 0; i < adapter.getCount(); i++)
			adapter.setSelected(i, false);

		adapter.notifyDataSetChanged();
	}

	/**
	 * Add selected songs to the queue and finish this activity when complete
	 */
	protected void queueSelectedSongs() {
		if (adapter.getSelectedCount() < 1)
			return;

		final Song[] albums = adapter.getSelectedAlbums();
		final Song[] songs = adapter.getSelectedSongs();

		Toaster.showShort(SongViewActivity.this, string.adding_to_queue);

		new QueueSongsTask(service) {

			@Override
			protected IOException doInBackground(Song... params) {
				if (albums.length > 0) {
					Set<Song> albumSongs = new LinkedHashSet<Song>();
					for (Song album : albums)
						try {
							for (Song song : service.get().getSongs(
									album.artist, album.album))
								albumSongs.add(song);
						} catch (IOException e) {
							return e;
						}
					if (!albumSongs.isEmpty()) {
						for (Song song : params)
							albumSongs.add(song);
						params = albumSongs
								.toArray(new Song[albumSongs.size()]);
					}
				}

				return super.doInBackground(params);
			}

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null)
					Toaster.showLong(SongViewActivity.this,
							string.queueing_failed);
				else {
					sendBroadcast(new Intent(ACTION_QUEUE));
					setResult(RESULT_OK);
					finish();
				}
			}
		}.execute(songs);
	}

	private void startSelectionMode() {
		if (actionMode != null)
			actionMode.invalidate();
		else
			actionMode = startActionMode(selectionModeCallback);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long itemId) {
		if (adapter.toggleSelection(position)) {
			adapter.update(position, view, parent.getItemAtPosition(position));
			startSelectionMode();
		}
	}
}
