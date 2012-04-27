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
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * List adapter for items of a specific type
 *
 * @param <V>
 */
public abstract class ItemListAdapter<V> extends BaseAdapter {

	/**
	 * View wrapper
	 *
	 * @param <I>
	 */
	public static abstract class ViewWrapper<I> {

		/**
		 * Update view for item
		 *
		 * @param item
		 */
		public abstract void update(I item);
	}

	private final LayoutInflater inflater;

	private final int viewId;

	private Object[] elements;

	/**
	 * Create empty adapter
	 *
	 * @param viewId
	 * @param inflater
	 */
	@SuppressWarnings("unchecked")
	public ItemListAdapter(final int viewId, final LayoutInflater inflater) {
		this(viewId, inflater, (V[]) new Object[0]);
	}

	/**
	 * Create adapter
	 *
	 * @param viewId
	 * @param inflater
	 * @param elements
	 */
	public ItemListAdapter(final int viewId, final LayoutInflater inflater,
			final V[] elements) {
		this.viewId = viewId;
		this.inflater = inflater;
		this.elements = elements;
	}

	/**
	 * @return items
	 */
	@SuppressWarnings("unchecked")
	protected V[] getItems() {
		return (V[]) elements;
	}

	public int getCount() {
		return elements.length;
	}

	@SuppressWarnings("unchecked")
	public V getItem(int position) {
		return (V) elements[position];
	}

	public long getItemId(int position) {
		return elements[position].hashCode();
	}

	/**
	 * Set items
	 *
	 * @param items
	 * @return items
	 */
	public ItemListAdapter<V> setItems(final Object[] items) {
		if (items != null)
			elements = items;
		else
			elements = new Object[0];
		notifyDataSetChanged();
		return this;
	}

	/**
	 * Create empty item view
	 *
	 * @param view
	 * @return item
	 */
	protected abstract ViewWrapper<V> createItemView(View view);

	@SuppressWarnings("unchecked")
	public View getView(final int position, View convertView,
			final ViewGroup parent) {
		ViewWrapper<V> itemView = null;
		if (convertView != null)
			itemView = (ViewWrapper<V>) convertView.getTag();
		if (itemView == null) {
			convertView = inflater.inflate(viewId, null);
			itemView = createItemView(convertView);
			convertView.setTag(itemView);
		}
		itemView.update(getItem(position));
		return convertView;
	}
}
