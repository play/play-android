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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

/**
 * Service class to make requests to the Play API
 */
public class PlayService {

	private static class SongWrapper {

		private Song[] songs;
	}

	private final Gson gson = new GsonBuilder().setFieldNamingPolicy(
			LOWER_CASE_WITH_UNDERSCORES).create();

	private final String baseUrl;

	private final String token;

	/**
	 * Create play service using base URL
	 * 
	 * @param baseUrl
	 * @param token
	 */
	public PlayService(final String baseUrl, final String token) {
		if (baseUrl.endsWith("/"))
			this.baseUrl = baseUrl;
		else
			this.baseUrl = baseUrl + '/';
		this.token = token;
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
	 * Encode value using {@link URLEncoder}
	 * 
	 * @param value
	 * @return encoded value
	 * @throws IOException
	 */
	protected String encode(String value) throws IOException {
		try {
			return URLEncoder.encode(value, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			IOException ioException = new IOException("Encoding URL failed");
			ioException.initCause(e);
			throw ioException;
		}
	}

	/**
	 * Create a GET request for the given URL
	 * 
	 * @param url
	 * @return request
	 */
	protected HttpRequest get(String url) {
		return HttpRequest.get(baseUrl + url).authorization(token);
	}

	/**
	 * Create a POST request for the given URL
	 * 
	 * @param url
	 * @return request
	 */
	protected HttpRequest post(String url) {
		return HttpRequest.post(baseUrl + url).authorization(token);
	}

	/**
	 * Create a DELETE request for the given URL
	 * 
	 * @param url
	 * @return request
	 */
	protected HttpRequest delete(String url) {
		return HttpRequest.delete(baseUrl + url).authorization(token);
	}

	/**
	 * Get currently playing song
	 * 
	 * @return song
	 * @throws IOException
	 */
	public Song getNowPlaying() throws IOException {
		try {
			HttpRequest request = get("now_playing");
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
			HttpRequest request = get("queue");
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
	 * Get {@link StreamingInfo} for Play service
	 * 
	 * @return URL to music stream
	 * @throws IOException
	 */
	public StreamingInfo getStreamingInfo() throws IOException {
		try {
			HttpRequest request = get("streaming_info");
			if (!request.ok())
				throw new IOException("Unexpected response code of "
						+ request.code());

			return fromJson(request, StreamingInfo.class);
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
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
			HttpRequest request = get("images/art/" + song.id + ".png");
			if (request.ok() && request.contentLength() > 0) {
				request.receive(file);
				return true;
			} else
				return false;
		} catch (HttpRequestException e) {
			return false;
		}
	}

	/**
	 * Star song
	 * 
	 * @param song
	 * @throws IOException
	 */
	public void star(Song song) throws IOException {
		try {
			HttpRequest request = post("star?id=" + song.id);
			if (!request.ok())
				throw new IOException("Unexpected response code of "
						+ request.code());
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Unstar song
	 * 
	 * @param song
	 * @throws IOException
	 */
	public void unstar(Song song) throws IOException {
		try {
			HttpRequest request = delete("star?id=" + song.id);
			if (!request.ok())
				throw new IOException("Unexpected response code of "
						+ request.code());
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Remove the given song from the queue
	 * 
	 * @param song
	 * @throws IOException
	 */
	public void dequeue(Song song) throws IOException {
		try {
			HttpRequest request = delete("queue?id=" + song.id);
			if (!request.ok())
				throw new IOException("Unexpected response code of "
						+ request.code());
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Requests some songs that match the freeform subject to be played
	 * 
	 * @param subject
	 * @return non-null but possibly empty array of queued songs
	 * @throws IOException
	 */
	public Song[] queueSubject(String subject) throws IOException {
		try {
			HttpRequest request = post("freeform?subject=" + encode(subject));
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
}
