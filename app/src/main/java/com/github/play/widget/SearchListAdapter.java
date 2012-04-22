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
package com.github.play.widget;

import android.view.LayoutInflater;
import static android.view.View.*;
import android.view.View;

import com.github.play.R.id;
import com.github.play.core.PlayService;
import com.github.play.core.Song;

import java.util.concurrent.atomic.AtomicReference;

/**
 * List adapter for searched songs
 */
public class SearchListAdapter extends PlayListAdapter {

	/**
	 * Song class that also encapsulates selected state
	 */
	public static class SearchSong extends Song {

		private static final long serialVersionUID = 7750930308930707720L;

		/**
		 * True if song selected to be added to the queue, false otherwise
		 */
		public boolean selected;

		/**
		 * Create from given song
		 *
		 * @param song
		 */
		public SearchSong(Song song) {
			super(song.id, song.name, song.artist, song.album, song.starred);
		}
	}

	private static class SearchSongViewWrapper extends SongViewWrapper {

		private final View selectedText;

		/**
		 * @param view
		 * @param service
		 */
		public SearchSongViewWrapper(View view,
				AtomicReference<PlayService> service) {
			super(view, service, null);

			selectedText = view.findViewById(id.tv_check);
		}

		@Override
		public void update(Song song) {
			super.update(song);

			int visibility = ((SearchSong) song).selected ? VISIBLE : INVISIBLE;
			selectedText.setVisibility(visibility);
		}
	}

	/**
	 * @param viewId
	 * @param inflater
	 * @param service
	 */
	public SearchListAdapter(int viewId, LayoutInflater inflater,
			AtomicReference<PlayService> service) {
		super(viewId, inflater, service, null);
	}

	@Override
	protected ViewWrapper<Song> createItemView(View view) {
		return new SearchSongViewWrapper(view, service);
	}
}
