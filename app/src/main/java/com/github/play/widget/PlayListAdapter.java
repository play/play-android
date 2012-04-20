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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.github.play.R.color;
import com.github.play.R.id;
import com.github.play.core.PlayService;
import com.github.play.core.Song;

import java.util.concurrent.atomic.AtomicReference;

/**
 * List adapter for songs
 */
public class PlayListAdapter extends ItemListAdapter<Song> {

	private static class SongViewWrapper extends ViewWrapper<Song> {

		private final TextView artistText;

		private final TextView songText;

		private final TextView albumText;

		private final TextView starText;

		private final SongArtWrapper albumArt;

		/**
		 * @param view
		 * @param service
		 * @param starListener
		 */
		public SongViewWrapper(View view, AtomicReference<PlayService> service,
				OnClickListener starListener) {
			artistText = (TextView) view.findViewById(id.tv_artist);
			songText = (TextView) view.findViewById(id.tv_song);
			albumText = (TextView) view.findViewById(id.tv_album);
			starText = (TextView) view.findViewById(id.tv_star);
			starText.setOnClickListener(starListener);
			albumArt = new SongArtWrapper(view.findViewById(id.iv_art), service);
		}

		public void update(Song song) {
			artistText.setText(song.artist);
			songText.setText(song.name);
			albumText.setText(song.album);

			starText.setTag(song);
			if (song.starred)
				starText.setTextColor(starText.getContext().getResources()
						.getColor(color.starred));
			else
				starText.setTextColor(starText.getContext().getResources()
						.getColor(color.unstarred));

			albumArt.update(song);
		}
	}

	private final AtomicReference<PlayService> service;

	private final OnClickListener starListener;

	/**
	 * @param viewId
	 * @param inflater
	 * @param service
	 * @param starListener
	 */
	public PlayListAdapter(int viewId, LayoutInflater inflater,
			AtomicReference<PlayService> service, OnClickListener starListener) {
		super(viewId, inflater);

		this.service = service;
		this.starListener = starListener;
	}

	@Override
	protected ViewWrapper<Song> createItemView(View view) {
		return new SongViewWrapper(view, service, starListener);
	}
}
