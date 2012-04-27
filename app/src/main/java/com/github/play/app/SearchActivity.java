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

import static android.app.SearchManager.QUERY;
import static android.content.Intent.ACTION_SEARCH;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static com.github.play.app.PlayActivity.ACTION_QUEUE;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.play.R.id;
import com.github.play.R.layout;
import com.github.play.R.menu;
import com.github.play.R.string;
import com.github.play.core.PlayPreferences;
import com.github.play.core.PlayService;
import com.github.play.core.QueueSongsTask;
import com.github.play.core.SearchTask;
import com.github.play.widget.SearchListAdapter;
import com.github.play.widget.SearchListAdapter.SearchSong;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Activity to search for songs and add them to the queue
 */
public class SearchActivity extends SherlockActivity implements
		OnItemClickListener {

	private final AtomicReference<PlayService> service = new AtomicReference<PlayService>();

	private MenuItem addItem;

	private final Set<String> songs = new HashSet<String>();

	private ListView listView;

	private View loadingView;

	private SearchListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.search);

		loadingView = findViewById(id.ll_loading);

		listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(this);
		adapter = new SearchListAdapter(layout.search_song,
				getLayoutInflater(), service);
		listView.setAdapter(adapter);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(string.search);
		actionBar.setDisplayHomeAsUpEnabled(true);

		PlayPreferences settings = new PlayPreferences(this);
		service.set(new PlayService(settings.getUrl(), settings.getToken()));

		search(getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		getSupportMenuInflater().inflate(menu.search, optionsMenu);
		addItem = optionsMenu.findItem(id.m_add);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		addItem.setEnabled(!songs.isEmpty());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, PlayActivity.class);
			intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		case id.m_search:
			onSearchRequested();
			return true;
		case id.m_add:
			queueSelected();
			return true;
		case id.m_clear:
			SearchSuggestionsProvider.clear(this);
			return true;
		case id.m_refresh:
			search(getIntent());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void queueSelected() {
		if (songs.isEmpty())
			return;

		if (addItem != null)
			addItem.setEnabled(false);

		String[] ids = songs.toArray(new String[songs.size()]);

		String message;
		if (ids.length > 1)
			message = MessageFormat.format(
					getString(string.adding_songs_to_queue), ids.length);
		else
			message = getString(string.adding_song_to_queue);
		Toast.makeText(getApplicationContext(), message, LENGTH_SHORT).show();

		new QueueSongsTask(service) {

			@Override
			protected void onPostExecute(IOException result) {
				super.onPostExecute(result);

				if (result != null) {
					Toast.makeText(getApplicationContext(),
							string.queueing_failed, LENGTH_LONG).show();
					if (addItem != null)
						addItem.setEnabled(true);
				} else {
					sendBroadcast(new Intent(ACTION_QUEUE));
					setResult(RESULT_OK);
					finish();
				}
			}
		}.execute(ids);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		search(intent);
	}

	private void search(final Intent intent) {
		if (intent == null)
			return;
		if (!ACTION_SEARCH.equals(intent.getAction()))
			return;

		String query = intent.getStringExtra(QUERY);
		if (TextUtils.isEmpty(query))
			return;

		getSupportActionBar().setSubtitle(query);

		SearchSuggestionsProvider.add(this, query);

		loadingView.setVisibility(VISIBLE);
		listView.setVisibility(GONE);

		new SearchTask(service) {

			@Override
			protected void onPostExecute(SearchResult result) {
				super.onPostExecute(result);

				if (result.exception == null) {
					SearchSong[] searchSongs = new SearchSong[result.songs.length];
					for (int i = 0; i < searchSongs.length; i++)
						searchSongs[i] = new SearchSong(result.songs[i]);
					adapter.setItems(searchSongs);
				} else {
					Toast.makeText(getApplicationContext(),
							string.search_failed, LENGTH_LONG).show();
				}

				loadingView.setVisibility(GONE);
				listView.setVisibility(VISIBLE);
			}
		}.execute(query);
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

		if (addItem != null)
			addItem.setEnabled(!songs.isEmpty());

		adapter.notifyDataSetChanged();
	}
}
