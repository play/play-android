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

import java.io.IOException;

/**
 * Callback for when the current song changes or an error occurs
 */
public interface SongCallback {

	/**
	 * Currently playing or queued songs have changed
	 *
	 * @param playing
	 * @param queued
	 */
	void onUpdate(Song playing, Song[] queued);

	/**
	 * Exception occurred contacting Play server
	 *
	 * @param e
	 */
	void onError(IOException e);
}
