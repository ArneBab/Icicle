/*
 * jFCPlib - ReceivedBookmarkFeed.java - Copyright © 2009 David Roden
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.pterodactylus.fcp;

/**
 * Implementation of the “ReceivedBookmarkFeed” FCP message. This message
 * notifies an FCP client that an update for a bookmark has been found.
 *
 * @author David ‘Bombe’ Roden &lt;bombe@freenetproject.org&gt;
 */
public class ReceivedBookmarkFeed extends BaseMessage {

	/**
	 * Creates a new “ReceivedBookmarkFeed” message.
	 *
	 * @param fcpMessage
	 *            The FCP message to get the fields from
	 */
	public ReceivedBookmarkFeed(FcpMessage fcpMessage) {
		super(fcpMessage);
	}

	/**
	 * Returns the name of the bookmark.
	 *
	 * @return The bookmark’s name
	 */
	public String getBookmarkName() {
		return getField("Name");
	}

	/**
	 * Returns the URI of the updated bookmark.
	 *
	 * @return The bookmark’s URI
	 */
	public String getURI() {
		return getField("URI");
	}

	/**
	 * Returns whether the bookmark has an active link image.
	 *
	 * @return {@code true} if the bookmark has an active link image,
	 *         {@code false} otherwise
	 */
	public boolean hasActiveLink() {
		return Boolean.parseBoolean(getField("HasAnActiveLink"));
	}

	/**
	 * Returns the description of the bookmark. Note that the description may
	 * be {@code null} and if it is not, it is base64-encoded!
	 *
	 * @return The bookmark’s description, or {@code null} if the bookmark has
	 *         no description
	 */
	public String getDescription() {
		return getField("Description");
	}

}
