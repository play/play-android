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
import android.util.SparseBooleanArray;
import android.view.View;

import com.github.kevinsawicki.wishlist.MultiTypeAdapter;
import com.github.play.R.drawable;
import com.github.play.R.id;
import com.github.play.R.layout;
import com.github.play.core.PlayService;
import com.github.play.core.Song;
import com.github.play.core.SongResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * List adapter for searched songs
 */
public class SearchListAdapter extends MultiTypeAdapter {

	private static final int TYPE_ALBUM_HEADER = 0;

	private static final int TYPE_ALBUM = 1;

	private static final int TYPE_SONG_HEADER = 2;

	private static final int TYPE_SONG = 3;

	private final SparseBooleanArray selected;

	private final SongArtWrapper albumArt;

	private SongResult result;

	/**
	 * Create search list adapter
	 *
	 * @param activity
	 * @param service
	 */
	public SearchListAdapter(final Activity activity,
			final AtomicReference<PlayService> service) {
		super(activity);

		albumArt = new SongArtWrapper(activity, service);
		selected = new SparseBooleanArray();
	}

	/**
	 * Set songs to display
	 *
	 * @param result
	 * @return this adapter
	 */
	public SearchListAdapter setSongs(final SongResult result) {
		this.result = result;
		clear();
		addItem(TYPE_ALBUM_HEADER, TYPE_ALBUM_HEADER);
		addItems(TYPE_ALBUM, result.albums);
		addItem(TYPE_SONG_HEADER, TYPE_SONG_HEADER);
		addItems(TYPE_SONG, result.songs);
		return this;
	}

	/**
	 * Toggle selected of item at given position
	 *
	 * @param position
	 * @return true if song or album was selected, false otherwise
	 */
	public boolean toggleSelection(final int position) {
		return setSelected(position, !selected.get(position, false));
	}

	/**
	 * Set item as selected or deselected
	 *
	 * @param position
	 * @param selected
	 * @return true if song or album was selected, false otherwise
	 */
	public boolean setSelected(final int position, final boolean selected) {
		int type = getItemViewType(position);
		boolean validType = type == TYPE_SONG || type == TYPE_ALBUM;
		if (validType)
			if (selected)
				this.selected.put(position, true);
			else
				this.selected.delete(position);
		return validType;
	}

	@Override
	public int getViewTypeCount() {
		return 4;
	}

	@Override
	protected int getChildLayoutId(final int type) {
		switch (type) {
		case TYPE_ALBUM_HEADER:
		case TYPE_SONG_HEADER:
			return layout.search_separator;
		case TYPE_ALBUM:
			return layout.search_album;
		case TYPE_SONG:
			return layout.search_song;
		default:
			return -1;
		}
	}

	/**
	 * Get number of items selected
	 *
	 * @return selected count
	 */
	public int getSelectedCount() {
		return selected.size();
	}

	/**
	 * Get selected songs
	 *
	 * @return non-null but possibly empty array of songs
	 */
	public Song[] getSelectedSongs() {
		List<Song> songs = new ArrayList<Song>();
		for (int i = 0; i < selected.size(); i++) {
			int position = selected.keyAt(i);
			if (TYPE_SONG == getItemViewType(position))
				songs.add((Song) getItem(position));
		}
		return songs.toArray(new Song[songs.size()]);
	}

	/**
	 * Get selected songs
	 *
	 * @return non-null but possibly empty array of songs
	 */
	public Song[] getSelectedAlbums() {
		List<Song> songs = new ArrayList<Song>();
		for (int i = 0; i < selected.size(); i++) {
			int position = selected.keyAt(i);
			if (TYPE_ALBUM == getItemViewType(position))
				songs.add((Song) getItem(position));
		}
		return songs.toArray(new Song[songs.size()]);
	}

	@Override
	protected int[] getChildViewIds(final int type) {
		switch (type) {
		case TYPE_ALBUM_HEADER:
		case TYPE_SONG_HEADER:
			return new int[] { id.tv_label, id.tv_count };
		case TYPE_ALBUM:
			return new int[] { id.tv_artist, id.tv_album, id.iv_art,
					id.iv_check };
		case TYPE_SONG:
			return new int[] { id.tv_artist, id.tv_song, id.tv_album,
					id.iv_art, id.iv_check };
		default:
			return null;
		}
	}

	/**
	 * Update item at given position
	 *
	 * @param position
	 * @param view
	 * @param item
	 */
	public void update(final int position, final View view, final Object item) {
		setCurrentView(view);
		update(position, item, getItemViewType(position));
	}

	@Override
	protected void update(final int position, final Object item, final int type) {
		switch (type) {
		case TYPE_ALBUM_HEADER:
			setText(0, "Albums");
			setText(1, '(' + FORMAT_INT.format(result.albums.length) + ')');
			return;
		case TYPE_ALBUM:
			Song album = (Song) item;
			if (selected.get(position))
				imageView(3).setImageResource(drawable.selection_checked);
			else
				imageView(3).setImageResource(drawable.selection_unchecked);
			setText(0, album.artist);
			setText(1, album.album);

			albumArt.update(imageView(2), drawable.queued_cd, album,
					album.getAlbumId());
			return;
		case TYPE_SONG_HEADER:
			setText(0, "Songs");
			setText(1, '(' + FORMAT_INT.format(result.songs.length) + ')');
			return;
		case TYPE_SONG:
			Song song = (Song) item;
			if (selected.get(position))
				imageView(4).setImageResource(drawable.selection_checked);
			else
				imageView(4).setImageResource(drawable.selection_unchecked);
			setText(0, song.artist);
			setText(1, song.name);
			setText(2, song.album);

			albumArt.update(imageView(3), drawable.queued_cd, song);
			return;
		}
	}

	@Override
	public boolean isEnabled(final int position) {
		final int type = getItemViewType(position);
		return TYPE_ALBUM_HEADER != type && TYPE_SONG_HEADER != type;
	}
}
