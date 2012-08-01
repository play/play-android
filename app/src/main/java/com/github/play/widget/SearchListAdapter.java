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
import com.github.kevinsawicki.wishlist.ViewUtils;
import com.github.play.R.drawable;
import com.github.play.R.id;
import com.github.play.R.layout;
import com.github.play.core.PlayService;
import com.github.play.core.Song;
import com.github.play.core.SongResult;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * List adapter for searched songs
 */
public class SearchListAdapter extends MultiTypeAdapter {

	private static final NumberFormat FORMAT = NumberFormat
			.getIntegerInstance();

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
	 * @return this adapter
	 */
	public SearchListAdapter toggleSelection(final int position) {
		return setSelected(position, !selected.get(position, false));
	}

	/**
	 * Set item as selected or deselected
	 *
	 * @param position
	 * @param selected
	 * @return this adapter
	 */
	public SearchListAdapter setSelected(final int position,
			final boolean selected) {
		int type = getItemViewType(position);
		if (type == TYPE_SONG || type == TYPE_ALBUM)
			if (selected)
				this.selected.put(position, true);
			else
				this.selected.delete(position);
		return this;
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
					id.tv_check };
		case TYPE_SONG:
			return new int[] { id.tv_artist, id.tv_song, id.tv_album,
					id.iv_art, id.tv_check };
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
		this.view = view;
		update(position, item, getItemViewType(position));
	}

	@Override
	protected void update(final int position, final Object item, final int type) {
		switch (type) {
		case TYPE_ALBUM_HEADER:
			setText(id.tv_label, "Albums");
			setText(id.tv_count,
					'(' + FORMAT.format(result.albums.length) + ')');
			return;
		case TYPE_ALBUM:
			Song album = (Song) item;
			ViewUtils.setInvisible(view(view, id.tv_check),
					!selected.get(position));
			setText(id.tv_artist, album.artist);
			setText(id.tv_album, album.album);

			albumArt.update(imageView(id.iv_art), drawable.queued_cd, album,
					album.getAlbumId());
			return;
		case TYPE_SONG_HEADER:
			setText(id.tv_label, "Songs");
			setText(id.tv_count, '(' + FORMAT.format(result.songs.length) + ')');
			return;
		case TYPE_SONG:
			Song song = (Song) item;
			ViewUtils.setInvisible(view(view, id.tv_check),
					!selected.get(position));
			setText(id.tv_artist, song.artist);
			setText(id.tv_song, song.name);
			setText(id.tv_album, song.album);

			albumArt.update(imageView(id.iv_art), drawable.queued_cd, song);
			return;
		}
	}
}
