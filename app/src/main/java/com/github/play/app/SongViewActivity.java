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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.ViewUtils;
import com.github.play.R.id;
import com.github.play.R.layout;
import com.github.play.R.string;
import com.github.play.core.PlayPreferences;
import com.github.play.core.PlayService;
import com.github.play.core.QueueSongsTask;
import com.github.play.core.SongResult;
import com.github.play.widget.SearchListAdapter;
import com.github.play.widget.SearchListAdapter.SearchSong;
import com.github.play.widget.Toaster;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base activity to display a list of songs and optionally queue them
 */
public abstract class SongViewActivity extends SherlockActivity implements
		OnItemClickListener {

	/**
	 * Play service reference
	 */
	protected final AtomicReference<PlayService> service = new AtomicReference<PlayService>();

	private MenuItem addItem;

	private final Set<String> songs = new HashSet<String>();

	/**
	 * List view
	 */
	protected ListView listView;

	private View loadingView;

	private SearchListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.search);

		loadingView = findViewById(id.ll_loading);

		listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(this);
		adapter = new SearchListAdapter(this, layout.search_song, service);
		listView.setAdapter(adapter);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		PlayPreferences settings = new PlayPreferences(this);
		service.set(new PlayService(settings.getUrl(), settings.getToken()));

		refreshSongs();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		addItem = optionsMenu.findItem(id.m_add);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		showAddItem(!songs.isEmpty());
		return true;
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
		if (result.exception == null) {
			SearchSong[] searchSongs = new SearchSong[result.songs.length];
			for (int i = 0; i < searchSongs.length; i++)
				searchSongs[i] = new SearchSong(result.songs[i]);
			adapter.setItems(searchSongs);
		} else
			Toaster.showLong(SongViewActivity.this, string.search_failed);

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
		case id.m_add:
			queueSelectedSongs();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Show/hide add menu item
	 *
	 * @param show
	 */
	protected void showAddItem(final boolean show) {
		if (addItem != null)
			addItem.setVisible(show);
	}

	/**
	 * Add selected songs to the queue and finish this activity when complete
	 */
	protected void queueSelectedSongs() {
		if (songs.isEmpty())
			return;

		showAddItem(false);

		String[] ids = songs.toArray(new String[songs.size()]);

		String message;
		if (ids.length > 1)
			message = MessageFormat.format(
					getString(string.adding_songs_to_queue), ids.length);
		else
			message = getString(string.adding_song_to_queue);
		Toaster.showShort(SongViewActivity.this, message);

		new QueueSongsTask(service) {

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null) {
					Toaster.showLong(SongViewActivity.this,
							string.queueing_failed);
					showAddItem(true);
				} else {
					sendBroadcast(new Intent(ACTION_QUEUE));
					setResult(RESULT_OK);
					finish();
				}
			}
		}.execute(ids);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long itemId) {
		SearchSong song = (SearchSong) parent.getItemAtPosition(position);
		song.selected = !song.selected;

		if (song.selected)
			songs.add(song.id);
		else
			songs.remove(song.id);

		String title;
		if (songs.size() > 1)
			title = MessageFormat.format(getString(string.multiple_selected),
					songs.size());
		else if (songs.size() == 1)
			title = getString(string.single_selected);
		else
			title = getString(string.search);
		getSupportActionBar().setTitle(title);

		showAddItem(!songs.isEmpty());

		adapter.update(position, view, song);
	}
}
