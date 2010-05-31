/*  Copyright 2010 Ben Ruijl, Wouter Smeenk

This file is part of Walled In.

Walled In is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3, or (at your option)
any later version.

Walled In is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with Walled In; see the file LICENSE.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

 */
package walledin.game.network.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

import walledin.engine.math.Vector2f;
import walledin.game.EntityManager;
import walledin.game.entity.Attribute;
import walledin.game.entity.Entity;
import walledin.game.map.GameMapIO;
import walledin.game.map.GameMapIOXML;
import walledin.game.network.NetworkConstants;
import walledin.game.network.NetworkDataReader;
import walledin.game.network.NetworkDataWriter;
import walledin.game.network.NetworkEventListener;
import walledin.util.Utils;

/**
 * This class provides the server for the game. All gamestate updates happen
 * here. Clients can register to this class to be added to the game.
 * 
 */
public class Server implements NetworkEventListener {
	private static final Logger LOG = Logger.getLogger(Server.class);
	private static final int PORT = 1234;
	private static final int UPDATES_PER_SECOND = 30;
	private static final int STORED_CHANGESETS = UPDATES_PER_SECOND*2;
	private final Map<SocketAddress, PlayerConnection> players;
	private boolean running;
	private final NetworkDataWriter networkWriter;
	private final NetworkDataReader networkReader;
	private Entity map;
	private long currentTime;
	private final EntityManager entityManager;
	private final Queue<ChangeSet> changeSets;
	private final Map<Integer, ChangeSet> changeSetLookup;

	/**
	 * Creates a new server. Initializes variables to their default values.
	 */
	public Server() {
		players = new HashMap<SocketAddress, PlayerConnection>();
		running = false;
		networkWriter = new NetworkDataWriter();
		networkReader = new NetworkDataReader(this);
		entityManager = new EntityManager(new ServerEntityFactory());
		changeSetLookup = new HashMap<Integer, ChangeSet>();
		changeSets = new LinkedList<ChangeSet>();
		// Store the first version so we can give it new players
		ChangeSet firstChangeSet = entityManager.getChangeSet();
		changeSetLookup.put(firstChangeSet.getVersion(), firstChangeSet);
	}

	/**
	 * Start of application. It runs the server.
	 * 
	 * @param args
	 *            Command line arguments
	 * @throws IOException
	 */
	public static void main(final String[] args) {
		try {
			new Server().run();
		} catch (final IOException e) {
			LOG.fatal("IOException during network loop", e);
		}
	}

	/**
	 * Runs the server. It starts a server channel and enters the main loop.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		LOG.info("initializing");
		init();
		final DatagramChannel channel = DatagramChannel.open();
		channel.socket().bind(new InetSocketAddress(PORT));
		channel.configureBlocking(false);

		running = true;
		LOG.info("starting main loop");
		while (running) {
			final long time = System.nanoTime();
			doLoop(channel);
			double delta = System.nanoTime() - time;
			// convert to sec
			delta /= 1000000000;
			// Calculate the how many milliseconds are left
			final long left = (long) ((1d / UPDATES_PER_SECOND - delta) * 1000);
			try {
				if (left > 0) {
					Thread.sleep(left);
				}
			} catch (final InterruptedException e) {
				LOG.fatal("Interrupted in network loop!", e);
				return;
			}
		}
	}

	/**
	 * Main loop of the server. Takes care of reading messages, updating
	 * gamestate, and sending messages.
	 * 
	 * @param channel
	 *            The channel to read from / send to
	 * @throws IOException
	 */
	private void doLoop(final DatagramChannel channel) throws IOException {
		// Read input messages and login messages
		boolean hasMore = networkReader.recieveMessage(channel, entityManager);
		while (hasMore) {
			hasMore = networkReader.recieveMessage(channel, entityManager);
		}

		double delta = System.nanoTime() - currentTime;
		currentTime = System.nanoTime();
		// convert to sec
		delta /= 1000000000;

		// Update game state
		update(delta);
		// Process the changes
		processChanges();
		// Write to all the clients
		sendGamestate(channel);
	}

	private void processChanges() {
		// Get the oldest changeset (we dont support this version anymore from
		// now)
		ChangeSet oldChangeSet = changeSets.remove();
		changeSetLookup.remove(oldChangeSet.getVersion());
		// Get current change set from entity manager and merge it with all the
		// save versions
		ChangeSet currentChangeSet = entityManager.getChangeSet();
		for (ChangeSet changeSet : changeSetLookup.values()) {
			changeSet.merge(currentChangeSet);
		}
		// Add the current change set
		changeSets.add(currentChangeSet);
		changeSetLookup.put(currentChangeSet.getVersion(), currentChangeSet);
		Set<SocketAddress> removedPlayers = new HashSet<SocketAddress>();
		for (PlayerConnection connection: players.values()) {
			if (connection.getReceivedVersion() <= oldChangeSet.getVersion()) {
				removedPlayers.add(connection.getAddress());
				LOG.info("Connection lost to client " + connection.getAddress());
			}
		}
		for (SocketAddress address: removedPlayers) {
			players.remove(address);
		}
	}

	/**
	 * Writes updated game information to both new and current players. The new
	 * players receive extra data.
	 * 
	 * @param channel
	 *            The channel to send to
	 * @throws IOException
	 */
	private void sendGamestate(final DatagramChannel channel)
			throws IOException {
		int currentVersion = entityManager.getCurrentVersion();
		for (PlayerConnection connection : players.values()) {
			int sendVersion = connection.getReceivedVersion();
			if (connection.isNew()) {
				// Set to first version
				sendVersion = 0;
				connection.setNew();
			}
			ChangeSet changeSet = changeSetLookup.get(sendVersion);
			networkWriter.sendGamestateMessage(channel,
					connection.getAddress(), entityManager, changeSet,
					currentVersion);
		}
	}

	private void removePlayer(SocketAddress address) {
		players.remove(address);
	}

	@Override
	public void receivedGamestateMessage(final SocketAddress address, int version) {
		// ignore .. should not happen
	}

	/**
	 * Creates a connection to a new client.
	 * 
	 * @param name
	 *            Player name
	 * @param address
	 *            Player socket address
	 */
	@Override
	public void receivedLoginMessage(final SocketAddress address,
			final String name) {
		final String entityName = NetworkConstants
				.getAddressRepresentation(address);
		final Entity player = entityManager.create("Player", entityName);
		player.setAttribute(Attribute.POSITION, new Vector2f(400, 300));
		player.setAttribute(Attribute.PLAYER_NAME, name);

		/* Let the player start with a handgun */
		/*
		 * final Entity weapon = entityManager.create("handgun",
		 * entityManager.generateUniqueName("handgun"));
		 * player.setAttribute(Attribute.WEAPON, weapon);
		 */

		final PlayerConnection con = new PlayerConnection(address, player, entityManager.getCurrentVersion());
		players.put(address, con);

		LOG.info("new player " + name + " @ " + address);
	}

	/**
	 * Log the player out
	 */
	@Override
	public void receivedLogoutMessage(final SocketAddress address) {
		LOG.info("Player " + address.toString() + " left the game.");
		removePlayer(address);
	}

	/**
	 * Set the new input state
	 */
	@Override
	public void receivedInputMessage(final SocketAddress address, int version,
			final Set<Integer> keys) {
		final PlayerConnection connection = players.get(address);
		if (connection != null) {
			connection.getPlayer().setAttribute(Attribute.KEYS_DOWN, keys);
			connection.setReceivedVersion(version);
		}
	}

	/**
	 * Update the gamestate, removes disconnected players and does collision
	 * detection.
	 * 
	 * @param delta
	 *            Time elapsed since last update
	 */
	public void update(final double delta) {
		/* Update all entities */
		entityManager.update(delta);

		/* Do collision detection */
		entityManager.doCollisionDetection(map, delta);
	}

	/**
	 * Initializes the game. It reads the default map and initializes the entity
	 * manager.
	 */
	public void init() {
		// Fill the change set queue
		for (int i = 0; i < STORED_CHANGESETS; i++) {
			ChangeSet changeSet = entityManager.getChangeSet();
			changeSets.add(changeSet);
			changeSetLookup.put(changeSet.getVersion(), changeSet);
		}
		
		// initialize entity manager
		entityManager.init();

		final GameMapIO mapIO = new GameMapIOXML(entityManager); // choose XML
		// as format
		map = mapIO.readFromURL(Utils.getClasspathURL("map.xml"));
	}
}
