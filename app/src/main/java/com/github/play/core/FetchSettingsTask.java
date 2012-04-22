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

import com.github.play.core.FetchSettingsTask.PlaySettings;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task to fetch the streaming URL and pusher application key of a configured
 * Play server
 */
public class FetchSettingsTask extends AsyncTask<Void, Void, PlaySettings> {

	/**
	 * Retrieved Play server settings
	 */
	public static class PlaySettings {

		/**
		 * Streaming info
		 */
		public final StreamingInfo streamingInfo;

		/**
		 * Exception that occurred retrieving streaming URL
		 */
		public final IOException exception;

		private PlaySettings(final StreamingInfo streamingInfo,
				final IOException exception) {
			this.streamingInfo = streamingInfo;
			this.exception = exception;
		}

		private PlaySettings(final StreamingInfo streamingInfo) {
			this(streamingInfo, null);
		}

		private PlaySettings(final IOException exception) {
			this(null, exception);
		}
	}

	private final AtomicReference<PlayService> service;

	/**
	 * Create task to fetch streaming URL
	 *
	 * @param service
	 */
	public FetchSettingsTask(final AtomicReference<PlayService> service) {
		this.service = service;
	}

	@Override
	protected PlaySettings doInBackground(Void... params) {
		try {
			StreamingInfo streamingInfo = service.get().getStreamingInfo();
			return new PlaySettings(streamingInfo);
		} catch (IOException e) {
			return new PlaySettings(e);
		}
	}
}
