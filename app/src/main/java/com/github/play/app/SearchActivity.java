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
import static com.github.play.app.PlayActivity.ACTION_QUEUE;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.play.R.id;
import com.github.play.R.menu;
import com.github.play.R.string;
import com.github.play.core.QueueSongsTask;
import com.github.play.core.SearchTask;
import com.github.play.core.Song;
import com.github.play.core.SongResult;
import com.github.play.widget.Toaster;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Activity to search for songs and add them to the queue
 */
public class SearchActivity extends SongViewActivity implements
		OnItemLongClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void refreshSongs() {
		search(getIntent());

		listView.setOnItemClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		getSupportMenuInflater().inflate(menu.search, optionsMenu);

		return super.onCreateOptionsMenu(optionsMenu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case id.m_search:
			onSearchRequested();
			return true;
		case id.m_clear:
			SearchSuggestionsProvider.clear(this);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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

		showLoading(true);

		new SearchTask(service) {

			@Override
			protected void onPostExecute(SongResult result) {
				super.onPostExecute(result);

				displaySongs(result);
				showLoading(false);
			}
		}.execute(query);
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		final Song song = (Song) parent.getItemAtPosition(position);
		final Builder builder = new Builder(this);
		builder.setCancelable(true);
		builder.setTitle(string.title_play_album);
		builder.setMessage(MessageFormat.format(
				getString(string.message_play_album), song.album));
		builder.setNegativeButton(android.R.string.no, null);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();

						showAddItem(false);

						new QueueSongsTask(service) {

							@Override
							protected IOException doInBackground(
									String... params) {
								String[] ids;
								try {
									Song[] songs = service.get().getSongs(
											song.artist, song.album);
									ids = new String[songs.length];
									for (int i = 0; i < songs.length; i++)
										ids[i] = songs[i].id;
								} catch (IOException e) {
									return e;
								}
								return super.doInBackground(ids);
							}

							@Override
							protected void onPostExecute(IOException result) {
								super.onPostExecute(result);

								showAddItem(true);

								if (result != null)
									Toaster.showLong(SearchActivity.this,
											string.queueing_failed);
								else {
									Toaster.showLong(SearchActivity.this,
											string.album_added_to_queue,
											song.album);
									sendBroadcast(new Intent(ACTION_QUEUE));
								}
							}

						}.execute();
					}
				});
		builder.show();
		return true;
	}
}
