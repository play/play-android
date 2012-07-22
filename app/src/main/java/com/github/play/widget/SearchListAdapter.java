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

import android.content.Context;
import android.view.View;

import com.github.kevinsawicki.wishlist.ViewUtils;
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

	/**
	 * @param context
	 * @param viewId
	 * @param service
	 */
	public SearchListAdapter(final Context context, int viewId,
			AtomicReference<PlayService> service) {
		super(context, viewId, service);
	}

	@Override
	protected int[] getChildViewIds() {
		return join(super.getChildViewIds(), id.tv_check);
	}

	@Override
	public void update(int position, View view, Song song) {
		super.update(position, view, song);

		ViewUtils.setInvisible(view(view, id.tv_check),
				!((SearchSong) song).selected);
	}
}
