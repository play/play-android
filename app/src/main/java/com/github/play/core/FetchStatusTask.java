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
package com.github.play.core;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Obtain the currently playing and queued songs
 */
public class FetchStatusTask extends AsyncTask<Void, Void, Object[]> {

	private final AtomicReference<PlayService> service;

	private final SongCallback callback;

	/**
	 * Create task with callback to call from {@link #onPostExecute(Object[])}
	 *
	 * @param service
	 * @param callback
	 */
	public FetchStatusTask(final AtomicReference<PlayService> service,
			final SongCallback callback) {
		this.service = service;
		this.callback = callback;
	}

	@Override
	protected Object[] doInBackground(Void... params) {
		PlayService service = this.service.get();
		try {
			Song playing = service.getNowPlaying();
			Song[] queue = service.getQueue();
			return new Object[] { playing, queue };
		} catch (IOException e) {
			return new Object[] { e };
		}
	}

	@Override
	protected void onPostExecute(final Object[] result) {
		if (result.length == 2)
			callback.onUpdate((Song) result[0], (Song[]) result[1]);
		else
			callback.onError((IOException) result[0]);
	}
}
