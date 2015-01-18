/*
 * jSite2 - FcpAdapter.java -
 * Copyright © 2008 David Roden
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
 * Adapter for {@link FcpListener}.
 * 
 * @author David ‘Bombe’ Roden &lt;bombe@freenetproject.org&gt;
 */
public class FcpAdapter implements FcpListener {

	/**
	 * {@inheritDoc}
	 */
	public void receivedNodeHello(FcpConnection fcpConnection, NodeHello nodeHello) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedCloseConnectionDuplicateClientName(FcpConnection fcpConnection, CloseConnectionDuplicateClientName closeConnectionDuplicateClientName) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedSSKKeypair(FcpConnection fcpConnection, SSKKeypair sskKeypair) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPeer(FcpConnection fcpConnection, Peer peer) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedEndListPeers(FcpConnection fcpConnection, EndListPeers endListPeers) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPeerNote(FcpConnection fcpConnection, PeerNote peerNote) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedEndListPeerNotes(FcpConnection fcpConnection, EndListPeerNotes endListPeerNotes) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPeerRemoved(FcpConnection fcpConnection, PeerRemoved peerRemoved) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see FcpListener#receivedNodeData(FcpConnection, NodeData)
	 */
	public void receivedNodeData(FcpConnection fcpConnection, NodeData nodeData) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see FcpListener#receivedTestDDAReply(FcpConnection, TestDDAReply)
	 */
	public void receivedTestDDAReply(FcpConnection fcpConnection, TestDDAReply testDDAReply) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedTestDDAComplete(FcpConnection fcpConnection, TestDDAComplete testDDAComplete) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPersistentGet(FcpConnection fcpConnection, PersistentGet persistentGet) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPersistentPut(FcpConnection fcpConnection, PersistentPut persistentPut) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedEndListPersistentRequests(FcpConnection fcpConnection, EndListPersistentRequests endListPersistentRequests) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedURIGenerated(FcpConnection fcpConnection, URIGenerated uriGenerated) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedDataFound(FcpConnection fcpConnection, DataFound dataFound) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedAllData(FcpConnection fcpConnection, AllData allData) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedSimpleProgress(FcpConnection fcpConnection, SimpleProgress simpleProgress) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedStartedCompression(FcpConnection fcpConnection, StartedCompression startedCompression) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receviedFinishedCompression(FcpConnection fcpConnection, FinishedCompression finishedCompression) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedUnknownPeerNoteType(FcpConnection fcpConnection, UnknownPeerNoteType unknownPeerNoteType) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedUnknownNodeIdentifier(FcpConnection fcpConnection, UnknownNodeIdentifier unknownNodeIdentifier) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedConfigData(FcpConnection fcpConnection, ConfigData configData) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedGetFailed(FcpConnection fcpConnection, GetFailed getFailed) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPutFailed(FcpConnection fcpConnection, PutFailed putFailed) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedIdentifierCollision(FcpConnection fcpConnection, IdentifierCollision identifierCollision) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPersistentPutDir(FcpConnection fcpConnection, PersistentPutDir persistentPutDir) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPersistentRequestRemoved(FcpConnection fcpConnection, PersistentRequestRemoved persistentRequestRemoved) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedSubscribedUSKUpdate(FcpConnection fcpConnection, SubscribedUSKUpdate subscribedUSKUpdate) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPluginInfo(FcpConnection fcpConnection, PluginInfo pluginInfo) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedFCPPluginReply(FcpConnection fcpConnection, FCPPluginReply fcpPluginReply) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPersistentRequestModified(FcpConnection fcpConnection, PersistentRequestModified persistentRequestModified) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPutSuccessful(FcpConnection fcpConnection, PutSuccessful putSuccessful) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedPutFetchable(FcpConnection fcpConnection, PutFetchable putFetchable) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedProtocolError(FcpConnection fcpConnection, ProtocolError protocolError) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void receivedMessage(FcpConnection fcpConnection, FcpMessage fcpMessage) {
		/* empty. */
	}

	/**
	 * {@inheritDoc}
	 */
	public void connectionClosed(FcpConnection fcpConnection, Throwable throwable) {
		/* empty. */
	}

}
