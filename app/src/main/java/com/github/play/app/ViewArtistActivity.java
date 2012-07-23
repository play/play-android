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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.github.play.R.menu;
import com.github.play.core.Song;
import com.github.play.core.SongResult;

import java.io.IOException;

/**
 * Activity to view all the songs by an artist and be able to add them to the
 * queue
 */
public class ViewArtistActivity extends SongViewActivity {

	private static final String EXTRA_SONG = "song";

	/**
	 * Create intent for song
	 *
	 * @param context
	 * @param song
	 * @return intent
	 */
	public static Intent createIntent(final Context context, final Song song) {
		final Intent intent = new Intent(context, ViewArtistActivity.class);
		intent.putExtra(EXTRA_SONG, song);
		return intent;
	}

	private Song song;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		song = (Song) getIntent().getSerializableExtra(EXTRA_SONG);
		getSupportActionBar().setSubtitle(song.artist);

		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		getSupportMenuInflater().inflate(menu.songs, optionsMenu);

		return super.onCreateOptionsMenu(optionsMenu);
	}

	@Override
	protected void refreshSongs() {
		showLoading(true);

		new AsyncTask<Song, Void, SongResult>() {

			@Override
			protected SongResult doInBackground(Song... params) {
				try {
					return new SongResult(service.get().getSongs(song.artist));
				} catch (IOException e) {
					return new SongResult(e);
				}
			}

			@Override
			protected void onPostExecute(SongResult result) {
				displaySongs(result);
			}
		}.execute(song);
	}
}
