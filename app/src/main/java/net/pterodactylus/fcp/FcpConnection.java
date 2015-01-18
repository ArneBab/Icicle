/*
 * jSite2 - FpcConnection.java -
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

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An FCP connection to a Freenet node.
 * 
 * @author David ‘Bombe’ Roden &lt;bombe@freenetproject.org&gt;
 */
public class FcpConnection implements Closeable {

	/** Logger. */
	private static final Logger logger = Logger.getLogger(FcpConnection.class.getName());

	/** The default port for FCP v2. */
	public static final int DEFAULT_PORT = 9481;

	/** The list of FCP listeners. */
	private final List<FcpListener> fcpListeners = new ArrayList<FcpListener>();

	/** The address of the node. */
	private final InetAddress address;

	/** The port number of the node’s FCP port. */
	private final int port;

	/** The remote socket. */
	private Socket remoteSocket;

	/** The input stream from the node. */
	private InputStream remoteInputStream;

	/** The output stream to the node. */
	private OutputStream remoteOutputStream;

	/** The connection handler. */
	private FcpConnectionHandler connectionHandler;

	/** Incoming message statistics. */
	private Map<String, Integer> incomingMessageStatistics = Collections.synchronizedMap(new HashMap<String, Integer>());

	/**
	 * Creates a new FCP connection to the freenet node running on localhost,
	 * using the default port.
	 * 
	 * @throws UnknownHostException
	 *             if the hostname can not be resolved
	 */
	public FcpConnection() throws UnknownHostException {
		this(InetAddress.getLocalHost());
	}

	/**
	 * Creates a new FCP connection to the Freenet node running on the given
	 * host, listening on the default port.
	 * 
	 * @param host
	 *            The hostname of the Freenet node
	 * @throws UnknownHostException
	 *             if <code>host</code> can not be resolved
	 */
	public FcpConnection(String host) throws UnknownHostException {
		this(host, DEFAULT_PORT);
	}

	/**
	 * Creates a new FCP connection to the Freenet node running on the given
	 * host, listening on the given port.
	 * 
	 * @param host
	 *            The hostname of the Freenet node
	 * @param port
	 *            The port number of the node’s FCP port
	 * @throws UnknownHostException
	 *             if <code>host</code> can not be resolved
	 */
	public FcpConnection(String host, int port) throws UnknownHostException {
		this(InetAddress.getByName(host), port);
	}

	/**
	 * Creates a new FCP connection to the Freenet node running at the given
	 * address, listening on the default port.
	 * 
	 * @param address
	 *            The address of the Freenet node
	 */
	public FcpConnection(InetAddress address) {
		this(address, DEFAULT_PORT);
	}

	/**
	 * Creates a new FCP connection to the Freenet node running at the given
	 * address, listening on the given port.
	 * 
	 * @param address
	 *            The address of the Freenet node
	 * @param port
	 *            The port number of the node’s FCP port
	 */
	public FcpConnection(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	//
	// LISTENER MANAGEMENT
	//

	/**
	 * Adds the given listener to the list of listeners.
	 * 
	 * @param fcpListener
	 *            The listener to add
	 */
	public void addFcpListener(FcpListener fcpListener) {
		fcpListeners.add(fcpListener);
	}

	/**
	 * Removes the given listener from the list of listeners.
	 * 
	 * @param fcpListener
	 *            The listener to remove
	 */
	public void removeFcpListener(FcpListener fcpListener) {
		fcpListeners.remove(fcpListener);
	}

	/**
	 * Notifies listeners that a “NodeHello” message was received.
	 * 
	 * @see FcpListener#receivedNodeHello(FcpConnection, NodeHello)
	 * @param nodeHello
	 *            The “NodeHello” message
	 */
	private void fireReceivedNodeHello(NodeHello nodeHello) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedNodeHello(this, nodeHello);
		}
	}

	/**
	 * Notifies listeners that a “CloseConnectionDuplicateClientName” message
	 * was received.
	 * 
	 * @see FcpListener#receivedCloseConnectionDuplicateClientName(FcpConnection,
	 *      CloseConnectionDuplicateClientName)
	 * @param closeConnectionDuplicateClientName
	 *            The “CloseConnectionDuplicateClientName” message
	 */
	private void fireReceivedCloseConnectionDuplicateClientName(CloseConnectionDuplicateClientName closeConnectionDuplicateClientName) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedCloseConnectionDuplicateClientName(this, closeConnectionDuplicateClientName);
		}
	}

	/**
	 * Notifies listeners that a “SSKKeypair” message was received.
	 * 
	 * @see FcpListener#receivedSSKKeypair(FcpConnection, SSKKeypair)
	 * @param sskKeypair
	 *            The “SSKKeypair” message
	 */
	private void fireReceivedSSKKeypair(SSKKeypair sskKeypair) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedSSKKeypair(this, sskKeypair);
		}
	}

	/**
	 * Notifies listeners that a “Peer” message was received.
	 * 
	 * @see FcpListener#receivedPeer(FcpConnection, Peer)
	 * @param peer
	 *            The “Peer” message
	 */
	private void fireReceivedPeer(Peer peer) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPeer(this, peer);
		}
	}

	/**
	 * Notifies all listeners that an “EndListPeers” message was received.
	 * 
	 * @see FcpListener#receivedEndListPeers(FcpConnection, EndListPeers)
	 * @param endListPeers
	 *            The “EndListPeers” message
	 */
	private void fireReceivedEndListPeers(EndListPeers endListPeers) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedEndListPeers(this, endListPeers);
		}
	}

	/**
	 * Notifies all listeners that a “PeerNote” message was received.
	 * 
	 * @see FcpListener#receivedPeerNote(FcpConnection, PeerNote)
	 * @param peerNote
	 */
	private void fireReceivedPeerNote(PeerNote peerNote) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPeerNote(this, peerNote);
		}
	}

	/**
	 * Notifies all listeners that an “EndListPeerNotes” message was received.
	 * 
	 * @see FcpListener#receivedEndListPeerNotes(FcpConnection,
	 *      EndListPeerNotes)
	 * @param endListPeerNotes
	 *            The “EndListPeerNotes” message
	 */
	private void fireReceivedEndListPeerNotes(EndListPeerNotes endListPeerNotes) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedEndListPeerNotes(this, endListPeerNotes);
		}
	}

	/**
	 * Notifies all listeners that a “PeerRemoved” message was received.
	 * 
	 * @see FcpListener#receivedPeerRemoved(FcpConnection, PeerRemoved)
	 * @param peerRemoved
	 *            The “PeerRemoved” message
	 */
	private void fireReceivedPeerRemoved(PeerRemoved peerRemoved) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPeerRemoved(this, peerRemoved);
		}
	}

	/**
	 * Notifies all listeners that a “NodeData” message was received.
	 * 
	 * @see FcpListener#receivedNodeData(FcpConnection, NodeData)
	 * @param nodeData
	 *            The “NodeData” message
	 */
	private void fireReceivedNodeData(NodeData nodeData) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedNodeData(this, nodeData);
		}
	}

	/**
	 * Notifies all listeners that a “TestDDAReply” message was received.
	 * 
	 * @see FcpListener#receivedTestDDAReply(FcpConnection, TestDDAReply)
	 * @param testDDAReply
	 *            The “TestDDAReply” message
	 */
	private void fireReceivedTestDDAReply(TestDDAReply testDDAReply) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedTestDDAReply(this, testDDAReply);
		}
	}

	/**
	 * Notifies all listeners that a “TestDDAComplete” message was received.
	 * 
	 * @see FcpListener#receivedTestDDAComplete(FcpConnection, TestDDAComplete)
	 * @param testDDAComplete
	 *            The “TestDDAComplete” message
	 */
	private void fireReceivedTestDDAComplete(TestDDAComplete testDDAComplete) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedTestDDAComplete(this, testDDAComplete);
		}
	}

	/**
	 * Notifies all listeners that a “PersistentGet” message was received.
	 * 
	 * @see FcpListener#receivedPersistentGet(FcpConnection, PersistentGet)
	 * @param persistentGet
	 *            The “PersistentGet” message
	 */
	private void fireReceivedPersistentGet(PersistentGet persistentGet) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPersistentGet(this, persistentGet);
		}
	}

	/**
	 * Notifies all listeners that a “PersistentPut” message was received.
	 * 
	 * @see FcpListener#receivedPersistentPut(FcpConnection, PersistentPut)
	 * @param persistentPut
	 *            The “PersistentPut” message
	 */
	private void fireReceivedPersistentPut(PersistentPut persistentPut) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPersistentPut(this, persistentPut);
		}
	}

	/**
	 * Notifies all listeners that a “EndListPersistentRequests” message was
	 * received.
	 * 
	 * @see FcpListener#receivedEndListPersistentRequests(FcpConnection,
	 *      EndListPersistentRequests)
	 * @param endListPersistentRequests
	 *            The “EndListPersistentRequests” message
	 */
	private void fireReceivedEndListPersistentRequests(EndListPersistentRequests endListPersistentRequests) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedEndListPersistentRequests(this, endListPersistentRequests);
		}
	}

	/**
	 * Notifies all listeners that a “URIGenerated” message was received.
	 * 
	 * @see FcpListener#receivedURIGenerated(FcpConnection, URIGenerated)
	 * @param uriGenerated
	 *            The “URIGenerated” message
	 */
	private void fireReceivedURIGenerated(URIGenerated uriGenerated) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedURIGenerated(this, uriGenerated);
		}
	}

	/**
	 * Notifies all listeners that a “DataFound” message was received.
	 * 
	 * @see FcpListener#receivedDataFound(FcpConnection, DataFound)
	 * @param dataFound
	 *            The “DataFound” message
	 */
	private void fireReceivedDataFound(DataFound dataFound) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedDataFound(this, dataFound);
		}
	}

	/**
	 * Notifies all listeners that an “AllData” message was received.
	 * 
	 * @see FcpListener#receivedAllData(FcpConnection, AllData)
	 * @param allData
	 *            The “AllData” message
	 */
	private void fireReceivedAllData(AllData allData) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedAllData(this, allData);
		}
	}

	/**
	 * Notifies all listeners that a “SimpleProgress” message was received.
	 * 
	 * @see FcpListener#receivedSimpleProgress(FcpConnection, SimpleProgress)
	 * @param simpleProgress
	 *            The “SimpleProgress” message
	 */
	private void fireReceivedSimpleProgress(SimpleProgress simpleProgress) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedSimpleProgress(this, simpleProgress);
		}
	}

	/**
	 * Notifies all listeners that a “StartedCompression” message was received.
	 * 
	 * @see FcpListener#receivedStartedCompression(FcpConnection,
	 *      StartedCompression)
	 * @param startedCompression
	 *            The “StartedCompression” message
	 */
	private void fireReceivedStartedCompression(StartedCompression startedCompression) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedStartedCompression(this, startedCompression);
		}
	}

	/**
	 * Notifies all listeners that a “FinishedCompression” message was received.
	 * 
	 * @see FcpListener#receviedFinishedCompression(FcpConnection,
	 *      FinishedCompression)
	 * @param finishedCompression
	 *            The “FinishedCompression” message
	 */
	private void fireReceivedFinishedCompression(FinishedCompression finishedCompression) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receviedFinishedCompression(this, finishedCompression);
		}
	}

	/**
	 * Notifies all listeners that an “UnknownPeerNoteType” message was
	 * received.
	 * 
	 * @see FcpListener#receivedUnknownPeerNoteType(FcpConnection,
	 *      UnknownPeerNoteType)
	 * @param unknownPeerNoteType
	 *            The “UnknownPeerNoteType” message
	 */
	private void fireReceivedUnknownPeerNoteType(UnknownPeerNoteType unknownPeerNoteType) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedUnknownPeerNoteType(this, unknownPeerNoteType);
		}
	}

	/**
	 * Notifies all listeners that an “UnknownNodeIdentifier” message was
	 * received.
	 * 
	 * @see FcpListener#receivedUnknownNodeIdentifier(FcpConnection,
	 *      UnknownNodeIdentifier)
	 * @param unknownNodeIdentifier
	 *            The “UnknownNodeIdentifier” message
	 */
	private void fireReceivedUnknownNodeIdentifier(UnknownNodeIdentifier unknownNodeIdentifier) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedUnknownNodeIdentifier(this, unknownNodeIdentifier);
		}
	}

	/**
	 * Notifies all listeners that a “ConfigData” message was received.
	 * 
	 * @see FcpListener#receivedConfigData(FcpConnection, ConfigData)
	 * @param configData
	 *            The “ConfigData” message
	 */
	private void fireReceivedConfigData(ConfigData configData) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedConfigData(this, configData);
		}
	}

	/**
	 * Notifies all listeners that a “GetFailed” message was received.
	 * 
	 * @see FcpListener#receivedGetFailed(FcpConnection, GetFailed)
	 * @param getFailed
	 *            The “GetFailed” message
	 */
	private void fireReceivedGetFailed(GetFailed getFailed) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedGetFailed(this, getFailed);
		}
	}

	/**
	 * Notifies all listeners that a “PutFailed” message was received.
	 * 
	 * @see FcpListener#receivedPutFailed(FcpConnection, PutFailed)
	 * @param putFailed
	 *            The “PutFailed” message
	 */
	private void fireReceivedPutFailed(PutFailed putFailed) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPutFailed(this, putFailed);
		}
	}

	/**
	 * Notifies all listeners that an “IdentifierCollision” message was
	 * received.
	 * 
	 * @see FcpListener#receivedIdentifierCollision(FcpConnection,
	 *      IdentifierCollision)
	 * @param identifierCollision
	 *            The “IdentifierCollision” message
	 */
	private void fireReceivedIdentifierCollision(IdentifierCollision identifierCollision) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedIdentifierCollision(this, identifierCollision);
		}
	}

	/**
	 * Notifies all listeners that an “PersistentPutDir” message was received.
	 * 
	 * @see FcpListener#receivedPersistentPutDir(FcpConnection,
	 *      PersistentPutDir)
	 * @param persistentPutDir
	 *            The “PersistentPutDir” message
	 */
	private void fireReceivedPersistentPutDir(PersistentPutDir persistentPutDir) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPersistentPutDir(this, persistentPutDir);
		}
	}

	/**
	 * Notifies all listeners that a “PersistentRequestRemoved” message was
	 * received.
	 * 
	 * @see FcpListener#receivedPersistentRequestRemoved(FcpConnection,
	 *      PersistentRequestRemoved)
	 * @param persistentRequestRemoved
	 *            The “PersistentRequestRemoved” message
	 */
	private void fireReceivedPersistentRequestRemoved(PersistentRequestRemoved persistentRequestRemoved) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPersistentRequestRemoved(this, persistentRequestRemoved);
		}
	}

	/**
	 * Notifies all listeners that a “SubscribedUSKUpdate” message was received.
	 * 
	 * @see FcpListener#receivedSubscribedUSKUpdate(FcpConnection,
	 *      SubscribedUSKUpdate)
	 * @param subscribedUSKUpdate
	 *            The “SubscribedUSKUpdate” message
	 */
	private void fireReceivedSubscribedUSKUpdate(SubscribedUSKUpdate subscribedUSKUpdate) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedSubscribedUSKUpdate(this, subscribedUSKUpdate);
		}
	}

	/**
	 * Notifies all listeners that a “PluginInfo” message was received.
	 * 
	 * @see FcpListener#receivedPluginInfo(FcpConnection, PluginInfo)
	 * @param pluginInfo
	 *            The “PluginInfo” message
	 */
	private void fireReceivedPluginInfo(PluginInfo pluginInfo) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPluginInfo(this, pluginInfo);
		}
	}

	/**
	 * Notifies all listeners that an “FCPPluginReply” message was received.
	 * 
	 * @see FcpListener#receivedFCPPluginReply(FcpConnection, FCPPluginReply)
	 * @param fcpPluginReply
	 *            The “FCPPluginReply” message
	 */
	private void fireReceivedFCPPluginReply(FCPPluginReply fcpPluginReply) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedFCPPluginReply(this, fcpPluginReply);
		}
	}

	/**
	 * Notifies all listeners that a “PersistentRequestModified” message was
	 * received.
	 * 
	 * @see FcpListener#receivedPersistentRequestModified(FcpConnection,
	 *      PersistentRequestModified)
	 * @param persistentRequestModified
	 *            The “PersistentRequestModified” message
	 */
	private void fireReceivedPersistentRequestModified(PersistentRequestModified persistentRequestModified) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPersistentRequestModified(this, persistentRequestModified);
		}
	}

	/**
	 * Notifies all listeners that a “PutSuccessful” message was received.
	 * 
	 * @see FcpListener#receivedPutSuccessful(FcpConnection, PutSuccessful)
	 * @param putSuccessful
	 *            The “PutSuccessful” message
	 */
	private void fireReceivedPutSuccessful(PutSuccessful putSuccessful) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPutSuccessful(this, putSuccessful);
		}
	}

	/**
	 * Notifies all listeners that a “PutFetchable” message was received.
	 * 
	 * @see FcpListener#receivedPutFetchable(FcpConnection, PutFetchable)
	 * @param putFetchable
	 *            The “PutFetchable” message
	 */
	private void fireReceivedPutFetchable(PutFetchable putFetchable) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedPutFetchable(this, putFetchable);
		}
	}

	/**
	 * Notifies all listeners that a “ProtocolError” message was received.
	 * 
	 * @see FcpListener#receivedProtocolError(FcpConnection, ProtocolError)
	 * @param protocolError
	 *            The “ProtocolError” message
	 */
	private void fireReceivedProtocolError(ProtocolError protocolError) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedProtocolError(this, protocolError);
		}
	}

	/**
	 * Notifies all registered listeners that a message has been received.
	 * 
	 * @see FcpListener#receivedMessage(FcpConnection, FcpMessage)
	 * @param fcpMessage
	 *            The message that was received
	 */
	private void fireMessageReceived(FcpMessage fcpMessage) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.receivedMessage(this, fcpMessage);
		}
	}

	/**
	 * Notifies all listeners that the connection to the node was closed.
	 * 
	 * @param throwable
	 *            The exception that caused the disconnect, or <code>null</code>
	 *            if there was no exception
	 * @see FcpListener#connectionClosed(FcpConnection, Throwable)
	 */
	private void fireConnectionClosed(Throwable throwable) {
		for (FcpListener fcpListener : fcpListeners) {
			fcpListener.connectionClosed(this, throwable);
		}
	}

	//
	// ACTIONS
	//

	/**
	 * Connects to the node.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalStateException
	 *             if there is already a connection to the node
	 */
	public synchronized void connect() throws IOException, IllegalStateException {
		if (connectionHandler != null) {
			throw new IllegalStateException("already connected, disconnect first");
		}
		logger.info("connecting to " + address + ":" + port + "…");
		remoteSocket = new Socket(address, port);
		remoteInputStream = remoteSocket.getInputStream();
		remoteOutputStream = remoteSocket.getOutputStream();
		new Thread(connectionHandler = new FcpConnectionHandler(this, remoteInputStream)).start();
	}

	/**
	 * Disconnects from the node. If there is no connection to the node, this
	 * method does nothing.
	 * 
	 * @deprecated Use {@link #close()} instead
	 */
	@Deprecated
	public synchronized void disconnect() {
		close();
	}

	/**
	 * Closes the connection. If there is no connection to the node, this method
	 * does nothing.
	 */
	public void close() {
		handleDisconnect(null);
	}

	/**
	 * Sends the given FCP message.
	 * 
	 * @param fcpMessage
	 *            The FCP message to send
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public synchronized void sendMessage(FcpMessage fcpMessage) throws IOException {
		logger.fine("sending message: " + fcpMessage.getName());
		fcpMessage.write(remoteOutputStream);
	}

	//
	// PACKAGE-PRIVATE METHODS
	//

	/**
	 * Handles the given message, notifying listeners. This message should only
	 * be called by {@link FcpConnectionHandler}.
	 * 
	 * @param fcpMessage
	 *            The received message
	 */
	void handleMessage(FcpMessage fcpMessage) {
		logger.fine("received message: " + fcpMessage.getName());
		String messageName = fcpMessage.getName();
		countMessage(messageName);
		if ("SimpleProgress".equals(messageName)) {
			fireReceivedSimpleProgress(new SimpleProgress(fcpMessage));
		} else if ("ProtocolError".equals(messageName)) {
			fireReceivedProtocolError(new ProtocolError(fcpMessage));
		} else if ("PersistentGet".equals(messageName)) {
			fireReceivedPersistentGet(new PersistentGet(fcpMessage));
		} else if ("PersistentPut".equals(messageName)) {
			fireReceivedPersistentPut(new PersistentPut(fcpMessage));
		} else if ("PersistentPutDir".equals(messageName)) {
			fireReceivedPersistentPutDir(new PersistentPutDir(fcpMessage));
		} else if ("URIGenerated".equals(messageName)) {
			fireReceivedURIGenerated(new URIGenerated(fcpMessage));
		} else if ("EndListPersistentRequests".equals(messageName)) {
			fireReceivedEndListPersistentRequests(new EndListPersistentRequests(fcpMessage));
		} else if ("Peer".equals(messageName)) {
			fireReceivedPeer(new Peer(fcpMessage));
		} else if ("PeerNote".equals(messageName)) {
			fireReceivedPeerNote(new PeerNote(fcpMessage));
		} else if ("StartedCompression".equals(messageName)) {
			fireReceivedStartedCompression(new StartedCompression(fcpMessage));
		} else if ("FinishedCompression".equals(messageName)) {
			fireReceivedFinishedCompression(new FinishedCompression(fcpMessage));
		} else if ("GetFailed".equals(messageName)) {
			fireReceivedGetFailed(new GetFailed(fcpMessage));
		} else if ("PutFetchable".equals(messageName)) {
			fireReceivedPutFetchable(new PutFetchable(fcpMessage));
		} else if ("PutSuccessful".equals(messageName)) {
			fireReceivedPutSuccessful(new PutSuccessful(fcpMessage));
		} else if ("PutFailed".equals(messageName)) {
			fireReceivedPutFailed(new PutFailed(fcpMessage));
		} else if ("DataFound".equals(messageName)) {
			fireReceivedDataFound(new DataFound(fcpMessage));
		} else if ("SubscribedUSKUpdate".equals(messageName)) {
			fireReceivedSubscribedUSKUpdate(new SubscribedUSKUpdate(fcpMessage));
		} else if ("IdentifierCollision".equals(messageName)) {
			fireReceivedIdentifierCollision(new IdentifierCollision(fcpMessage));
		} else if ("AllData".equals(messageName)) {
			LimitedInputStream payloadInputStream = getInputStream(FcpUtils.safeParseLong(fcpMessage.getField("DataLength")));
			fireReceivedAllData(new AllData(fcpMessage, payloadInputStream));
			try {
				payloadInputStream.consume();
			} catch (IOException ioe1) {
				/* well, ignore. when the connection handler fails, all fails. */
			}
		} else if ("EndListPeerNotes".equals(messageName)) {
			fireReceivedEndListPeerNotes(new EndListPeerNotes(fcpMessage));
		} else if ("EndListPeers".equals(messageName)) {
			fireReceivedEndListPeers(new EndListPeers(fcpMessage));
		} else if ("SSKKeypair".equals(messageName)) {
			fireReceivedSSKKeypair(new SSKKeypair(fcpMessage));
		} else if ("PeerRemoved".equals(messageName)) {
			fireReceivedPeerRemoved(new PeerRemoved(fcpMessage));
		} else if ("PersistentRequestModified".equals(messageName)) {
			fireReceivedPersistentRequestModified(new PersistentRequestModified(fcpMessage));
		} else if ("PersistentRequestRemoved".equals(messageName)) {
			fireReceivedPersistentRequestRemoved(new PersistentRequestRemoved(fcpMessage));
		} else if ("UnknownPeerNoteType".equals(messageName)) {
			fireReceivedUnknownPeerNoteType(new UnknownPeerNoteType(fcpMessage));
		} else if ("UnknownNodeIdentifier".equals(messageName)) {
			fireReceivedUnknownNodeIdentifier(new UnknownNodeIdentifier(fcpMessage));
		} else if ("FCPPluginReply".equals(messageName)) {
			LimitedInputStream payloadInputStream = getInputStream(FcpUtils.safeParseLong(fcpMessage.getField("DataLength")));
			fireReceivedFCPPluginReply(new FCPPluginReply(fcpMessage, payloadInputStream));
			try {
				payloadInputStream.consume();
			} catch (IOException ioe1) {
				/* ignore. */
			}
		} else if ("PluginInfo".equals(messageName)) {
			fireReceivedPluginInfo(new PluginInfo(fcpMessage));
		} else if ("NodeData".equals(messageName)) {
			fireReceivedNodeData(new NodeData(fcpMessage));
		} else if ("TestDDAReply".equals(messageName)) {
			fireReceivedTestDDAReply(new TestDDAReply(fcpMessage));
		} else if ("TestDDAComplete".equals(messageName)) {
			fireReceivedTestDDAComplete(new TestDDAComplete(fcpMessage));
		} else if ("ConfigData".equals(messageName)) {
			fireReceivedConfigData(new ConfigData(fcpMessage));
		} else if ("NodeHello".equals(messageName)) {
			fireReceivedNodeHello(new NodeHello(fcpMessage));
		} else if ("CloseConnectionDuplicateClientName".equals(messageName)) {
			fireReceivedCloseConnectionDuplicateClientName(new CloseConnectionDuplicateClientName(fcpMessage));
		} else {
			fireMessageReceived(fcpMessage);
		}
	}

	/**
	 * Handles a disconnect from the node.
	 * 
	 * @param throwable
	 *            The exception that caused the disconnect, or <code>null</code>
	 *            if there was no exception
	 */
	synchronized void handleDisconnect(Throwable throwable) {
		FcpUtils.close(remoteInputStream);
		FcpUtils.close(remoteOutputStream);
		FcpUtils.close(remoteSocket);
		if (connectionHandler != null) {
			connectionHandler.stop();
			connectionHandler = null;
			fireConnectionClosed(throwable);
		}
	}

	//
	// PRIVATE METHODS
	//

	/**
	 * Incremets the counter in {@link #incomingMessageStatistics} by <cod>1</code>
	 * for the given message name.
	 * 
	 * @param name
	 *            The name of the message to count
	 */
	private void countMessage(String name) {
		int oldValue = 0;
		if (incomingMessageStatistics.containsKey(name)) {
			oldValue = incomingMessageStatistics.get(name);
		}
		incomingMessageStatistics.put(name, oldValue + 1);
		logger.finest("count for " + name + ": " + (oldValue + 1));
	}

	/**
	 * Returns a limited input stream from the node’s input stream.
	 * 
	 * @param dataLength
	 *            The length of the stream
	 * @return The limited input stream
	 */
	private LimitedInputStream getInputStream(long dataLength) {
		if (dataLength <= 0) {
			return new LimitedInputStream(null, 0);
		}
		return new LimitedInputStream(remoteInputStream, dataLength);
	}

	/**
	 * A wrapper around an {@link InputStream} that only supplies a limit number
	 * of bytes from the underlying input stream.
	 * 
	 * @author David ‘Bombe’ Roden &lt;bombe@freenetproject.org&gt;
	 */
	private static class LimitedInputStream extends FilterInputStream {

		/** The remaining number of bytes that can be read. */
		private long remaining;

		/**
		 * Creates a new LimitedInputStream that supplies at most
		 * <code>length</code> bytes from the given input stream.
		 * 
		 * @param inputStream
		 *            The input stream
		 * @param length
		 *            The number of bytes to read
		 */
		public LimitedInputStream(InputStream inputStream, long length) {
			super(inputStream);
			remaining = length;
		}

		/**
		 * @see java.io.FilterInputStream#available()
		 */
		@Override
		public synchronized int available() throws IOException {
			if (remaining == 0) {
				return 0;
			}
			return (int) Math.min(super.available(), Math.min(Integer.MAX_VALUE, remaining));
		}

		/**
		 * @see java.io.FilterInputStream#read()
		 */
		@Override
		public synchronized int read() throws IOException {
			int read = -1;
			if (remaining > 0) {
				read = super.read();
				remaining--;
			}
			return read;
		}

		/**
		 * @see java.io.FilterInputStream#read(byte[], int, int)
		 */
		@Override
		public synchronized int read(byte[] b, int off, int len) throws IOException {
			if (remaining == 0) {
				return -1;
			}
			int toCopy = (int) Math.min(len, Math.min(remaining, Integer.MAX_VALUE));
			int read = super.read(b, off, toCopy);
			remaining -= read;
			return read;
		}

		/**
		 * @see java.io.FilterInputStream#skip(long)
		 */
		@Override
		public synchronized long skip(long n) throws IOException {
			if ((n < 0) || (remaining == 0)) {
				return 0;
			}
			long skipped = super.skip(Math.min(n, remaining));
			remaining -= skipped;
			return skipped;
		}

		/**
		 * {@inheritDoc} This method does nothing, as {@link #mark(int)} and
		 * {@link #reset()} are not supported.
		 * 
		 * @see java.io.FilterInputStream#mark(int)
		 */
		@Override
		public void mark(int readlimit) {
			/* do nothing. */
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see java.io.FilterInputStream#markSupported()
		 * @return <code>false</code>
		 */
		@Override
		public boolean markSupported() {
			return false;
		}

		/**
		 * {@inheritDoc} This method does nothing, as {@link #mark(int)} and
		 * {@link #reset()} are not supported.
		 * 
		 * @see java.io.FilterInputStream#reset()
		 */
		@Override
		public void reset() throws IOException {
			/* do nothing. */
		}

		/**
		 * Consumes the input stream, i.e. read all bytes until the limit is
		 * reached.
		 * 
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public void consume() throws IOException {
			while (remaining > 0) {
				skip(remaining);
			}
		}

	}

}
