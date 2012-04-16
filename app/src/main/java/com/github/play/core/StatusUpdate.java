package com.github.play.core;

import java.io.Serializable;

/**
 * Wrapper class to encapsulate all data send via a Play push update
 */
public class StatusUpdate implements Serializable {

	private static final long serialVersionUID = -877849357884315386L;

	/**
	 * Currently playing song
	 */
	public final Song playing;

	/**
	 * Songs in the queue
	 */
	public final Song[] queued;

	/**
	 * Create status update
	 * 
	 * @param playing
	 * @param queued
	 */
	public StatusUpdate(final Song playing, final Song[] queued) {
		this.playing = playing;
		this.queued = queued;
	}
}
