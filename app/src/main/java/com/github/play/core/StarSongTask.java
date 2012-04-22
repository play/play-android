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
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task to star a song
 */
public class StarSongTask extends AsyncTask<Song, Void, IOException> {

	private static final String TAG = "StarSongTask";

	private final AtomicReference<PlayService> service;

	/**
	 * Create task to star a song
	 *
	 * @param service
	 */
	public StarSongTask(final AtomicReference<PlayService> service) {
		this.service = service;
	}

	@Override
	protected IOException doInBackground(Song... params) {
		try {
			service.get().star(params[0]);
			return null;
		} catch (IOException e) {
			return e;
		}
	}

	@Override
	protected void onPostExecute(IOException result) {
		super.onPostExecute(result);

		if (result != null)
			Log.d(TAG, "Unstarring failed", result);
	}
}
