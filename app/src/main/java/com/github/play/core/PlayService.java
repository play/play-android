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

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Service class to make requests to the Play API
 */
public class PlayService {

	private static class SongWrapper {

		private Song[] songs;
	}

	private static class StreamUrlWrapper {

		private String streamUrl;
	}

	private final Gson gson = new GsonBuilder().setFieldNamingPolicy(
			LOWER_CASE_WITH_UNDERSCORES).create();

	private final String baseUrl;

	private final String login;

	/**
	 * Create play service using base URL
	 * 
	 * @param baseUrl
	 * @param login
	 */
	public PlayService(final String baseUrl, final String login) {
		if (baseUrl.endsWith("/"))
			this.baseUrl = baseUrl;
		else
			this.baseUrl = baseUrl + '/';
		this.login = login;
	}

	/**
	 * Create object of class type from content of request
	 * 
	 * @param request
	 * @param target
	 * @return object of target class type
	 * @throws IOException
	 */
	protected <V> V fromJson(final HttpRequest request, final Type target)
			throws IOException {
		final Reader reader = request.bufferedReader();
		try {
			return gson.fromJson(reader, target);
		} catch (JsonParseException e) {
			IOException ioException = new IOException("Parsing JSON failed");
			ioException.initCause(e);
			throw ioException;
		} finally {
			try {
				reader.close();
			} catch (IOException ignored) {
				// Ignored
			}
		}
	}

	/**
	 * Get currently playing song
	 * 
	 * @return song
	 * @throws IOException
	 */
	public Song getNowPlaying() throws IOException {
		try {
			HttpRequest request = HttpRequest.get(baseUrl
					+ "now_playing?login=" + login);
			if (!request.ok())
				throw new IOException("Unexpected response code of "
						+ request.code());

			return fromJson(request, Song.class);
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Get songs in the queue
	 * 
	 * @return non-null but possibly empty array of songs
	 * @throws IOException
	 */
	public Song[] getQueue() throws IOException {
		try {
			HttpRequest request = HttpRequest.get(baseUrl + "queue?login="
					+ login);
			if (!request.ok())
				throw new IOException("Unexpected response code of "
						+ request.code());

			SongWrapper wrapper = fromJson(request, SongWrapper.class);
			if (wrapper != null && wrapper.songs != null)
				return wrapper.songs;
			else
				return new Song[0];
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Get URL to stream music from
	 * 
	 * @return URL to music stream
	 * @throws IOException
	 */
	public String getStreamUrl() throws IOException {
		try {
			HttpRequest request = HttpRequest.get(baseUrl + "stream_url?login="
					+ login);
			if (!request.ok())
				throw new IOException("Unexpected response code of "
						+ request.code());

			StreamUrlWrapper wrapper = fromJson(request, StreamUrlWrapper.class);
			return wrapper != null ? wrapper.streamUrl : null;
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Get application key to use when registering a pusher connection
	 * 
	 * @return pusher application key
	 */
	public String getPusherApplicationKey() {
		// TODO Needs API support from Play
		return null;
	}

	/**
	 * Download art image for {@link Song} to given file
	 * 
	 * @param song
	 * @param file
	 * @return true if succeeded, false if failed
	 */
	public boolean getArt(Song song, File file) {
		try {
			HttpRequest request = HttpRequest.get(baseUrl + "images/art/"
					+ song.id + ".png?login=" + login);
			if (request.ok() && request.contentLength() > 0) {
				request.receive(file);
				return true;
			} else
				return false;
		} catch (HttpRequestException e) {
			return false;
		}
	}
}
