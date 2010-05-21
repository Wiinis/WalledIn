package walledin.game.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import walledin.game.Item;
import walledin.game.entity.behaviors.BulletBehavior;
import walledin.game.entity.behaviors.HealthBehavior;
import walledin.game.entity.behaviors.HealthKitBehavior;
import walledin.game.entity.behaviors.SpatialBehavior;
import walledin.game.map.Tile;
import walledin.math.Rectangle;
import walledin.math.Vector2f;
import walledin.util.XMLReader;

public class ServerEntityFactory implements EntityFactory {
	public ServerEntityFactory() {
		itemContructionFunctions = new HashMap<String, ItemConstructionFunction>();
	}

	private interface ItemConstructionFunction {
		Item create(String itemName, Vector2f position, Vector2f velocity);
	}

	Map<String, ItemConstructionFunction> itemContructionFunctions;

	@Override
	public Player createPlayer(final String name, final Vector2f position,
			final Vector2f velocity) {
		final Player player = new Player(name);

		player.setAttribute(Attribute.ORIENTATION, 1); // start looking to
		// the right

		player.addBehavior(new HealthBehavior(player, 100, 100));
		player.addBehavior(new SpatialBehavior(player, position, velocity));

		// FIXME correct the drawing instead of the hack the bounding box
		player.setAttribute(Attribute.BOUNDING_RECT,
				new Rectangle(0, 0, 44, 43));

		return player;
	}

	@Override
	public Background createBackground(final String name) {
		final Background background = new Background(name);
		return background;
	}

	/**
	 * Creates a new item of a given family.
	 * 
	 * @param familyName
	 *            Family name
	 * @param itemName
	 *            The name of the item to be created
	 * @return Returns an item or null on failure
	 */
	@Override
	public Item createItem(final String familyName, final String itemName,
			final Vector2f position, final Vector2f velocity) {
		if (!itemContructionFunctions.containsKey(familyName)) {
			throw new IllegalArgumentException(
					"Item "
							+ familyName
							+ " is not found in the database. Are the items loaded correctly?");
		}

		final Item item = itemContructionFunctions.get(familyName).create(
				itemName, position, velocity);
		return item;
	}

	private Item createItemInternal(final String name, final String familyName,
			final Rectangle destRect, final Vector2f position,
			final Vector2f velocity) {
		final Item item = new Item(name, familyName);
		item.addBehavior(new SpatialBehavior(item, position, velocity));

		item.setAttribute(Attribute.BOUNDING_RECT, destRect);
		return item;
	}

	/**
	 * Creates a new game map
	 * 
	 * @param name
	 *            Map name
	 * @param width
	 *            Width of map
	 * @param height
	 *            Height of map
	 * @param tiles
	 *            Tile information
	 * @param items
	 */
	public GameMap createGameMap(final String name, final int width,
			final int height, final List<Tile> tiles, final List<Item> items) {
		final GameMap map = new GameMap(name);

		map.setAttribute(Attribute.WIDTH, width);
		map.setAttribute(Attribute.HEIGHT, height);
		map.setAttribute(Attribute.TILES, tiles);
		map.setAttribute(Attribute.ITEM_LIST, items);

		// TODO needed for col detection ... but maybe should be handled
		// differently
		map.setAttribute(Attribute.RENDER_TILE_SIZE, 32f);

		return map;
	}

	/**
	 * Creates a function that can create items of a particular family. It takes
	 * care of reading extra information, specific for the item, from the XML.
	 * 
	 * The creation of the item involves adding the required behaviors and
	 * setting its variables.
	 * 
	 * @param familyName
	 *            Name of the family. i.e healthkit, armourkit.
	 * @param texPart
	 *            Texture part
	 * @param scale
	 *            The scale with which to draw
	 * @param el
	 *            Element in XML file which contains item specific information,
	 *            like health kit strength or armor penetration value
	 */
	private void addFunction(final String familyName, final Rectangle destRect,
			final Element el) {
		if (familyName.equals("healthkit")) {
			itemContructionFunctions.put(familyName,
					new ItemConstructionFunction() {

						@Override
						public Item create(final String itemName,
								final Vector2f position, final Vector2f velocity) {
							final Item hk = new Item(familyName, itemName, texPart,
									destRect, position, velocity);

							// read extra data
							final int hkStrength = XMLReader.getIntValue(el,
									"strength");
							hk
									.addBehavior(new HealthKitBehavior(hk,
											hkStrength));
							return hk;
						}
					});
		}

		if (familyName.equals("armourkit")) {
			itemContructionFunctions.put(familyName,
					new ItemConstructionFunction() {

						@Override
						public Item create(final String itemName,
								final Vector2f position, final Vector2f velocity) {
							return new Item(familyName, itemName, texPart, destRect,
									position, velocity);
							// TODO:
							// read
							// custom
							// information
						}
					});
		}

		if (familyName.equals("bullet")) {
			itemContructionFunctions.put("bullet",
					new ItemConstructionFunction() {

						@Override
						public Item create(final String itemName,
								final Vector2f position, final Vector2f velocity) {
							final Item bl = new Item(familyName, itemName, texPart,
									destRect, position, velocity);
								bl
								.addBehavior(new BulletBehavior(bl));
							return bl; // TODO:

						}
					});
		}
	}

	/**
	 * Loads all information for the prototypes from an XML file.
	 * 
	 * @param filename
	 *            XML file
	 * @return True on success, false on failure
	 */
	public boolean loadItemsFromXML(final String filename) {
		final XMLReader reader = new XMLReader();

		if (reader.open(filename)) {
			final List<Element> elList = XMLReader.getElements(reader
					.getRootElement(), "item");

			for (final Element cur : elList) {
				final String familyName = XMLReader.getTextValue(cur, "name");
				final int destWidth = XMLReader.getIntValue(cur, "width");
				final int destHeight = XMLReader.getIntValue(cur, "height");

				addFunction(familyName, new Rectangle(0, 0, destWidth,
						destHeight), cur);
			}

		}
		return false;
	}
}
