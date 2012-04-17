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

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.github.play.R.color;
import com.github.play.R.id;
import com.github.play.core.PlayService;
import com.github.play.core.Song;
import com.github.play.widget.ItemListAdapter.ViewWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * View wrapper for the currently playing {@link Song} layout
 */
public class NowPlayingViewWrapper extends ViewWrapper<Song> {

	private final TextView songText;

	private final TextView artistText;

	private final TextView albumText;

	private final TextView starText;

	private SongArtWrapper artWrapper;

	/**
	 * Create item view
	 * 
	 * @param view
	 * @param service
	 * @param starListener
	 */
	public NowPlayingViewWrapper(final View view,
			final AtomicReference<PlayService> service,
			final OnClickListener starListener) {
		songText = (TextView) view.findViewById(id.tv_song);
		albumText = (TextView) view.findViewById(id.tv_album);
		artistText = (TextView) view.findViewById(id.tv_artist);
		starText = (TextView) view.findViewById(id.tv_star);
		starText.setOnClickListener(starListener);
		artWrapper = new SongArtWrapper(view.findViewById(id.iv_art), service);
	}

	@Override
	public void update(final Song song) {
		songText.setText(song.name);
		artistText.setText(song.artist);
		albumText.setText(song.album);

		starText.setTag(song);
		if (song.starred)
			starText.setTextColor(starText.getContext().getResources()
					.getColor(color.starred));
		else
			starText.setTextColor(starText.getContext().getResources()
					.getColor(color.unstarred));

		artWrapper.update(song);
	}
}
