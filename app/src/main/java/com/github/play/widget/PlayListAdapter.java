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

import android.app.Activity;
import android.view.View;

import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.github.play.R.id;
import com.github.play.core.PlayService;
import com.github.play.core.Song;

import java.util.concurrent.atomic.AtomicReference;

/**
 * List adapter for songs
 */
public class PlayListAdapter extends SingleTypeAdapter<Song> {

	/**
	 * Play service reference
	 */
	protected final AtomicReference<PlayService> service;

	private final SongArtWrapper albumArt;

	/**
	 * @param activity
	 * @param viewId
	 * @param service
	 */
	public PlayListAdapter(Activity activity, int viewId,
			AtomicReference<PlayService> service) {
		super(activity, viewId);

		this.service = service;
		albumArt = new SongArtWrapper(activity, service);
	}

	@Override
	protected int[] getChildViewIds() {
		return new int[] { id.tv_artist, id.tv_song, id.tv_album, id.iv_art };
	}

	@Override
	public View initialize(View view) {
		return super.initialize(view);
	}

	@Override
	public void update(int position, View view, Song song) {
		setText(view, id.tv_artist, song.artist);
		setText(view, id.tv_song, song.name);
		setText(view, id.tv_album, song.album);

		albumArt.update(imageView(view, id.iv_art), song);
	}
}
