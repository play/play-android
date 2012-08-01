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
import android.content.Intent;
import android.text.TextUtils;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.play.R.id;
import com.github.play.R.menu;
import com.github.play.core.SearchTask;
import com.github.play.core.SongResult;

/**
 * Activity to search for songs and add them to the queue
 */
public class SearchActivity extends SongViewActivity {

	@Override
	protected void refreshSongs() {
		search(getIntent());
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
}
