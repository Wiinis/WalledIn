package walledin.game.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import walledin.engine.TextureManager;
import walledin.engine.TexturePartManager;
import walledin.game.Item;
import walledin.game.entity.behaviors.BackgroundRenderBehavior;
import walledin.game.entity.behaviors.ItemRenderBehavior;
import walledin.game.entity.behaviors.MapRenderBehavior;
import walledin.game.entity.behaviors.PlayerAnimationBehavior;
import walledin.game.entity.behaviors.PlayerRenderBehavior;
import walledin.game.map.Tile;
import walledin.math.Rectangle;
import walledin.math.Vector2f;
import walledin.util.XMLReader;

public class ClientEntityFactory implements EntityFactory {
	public ClientEntityFactory() {
		itemContructionFunctions = new HashMap<String, ItemConstructionFunction>();
	}

	private interface ItemConstructionFunction {
		Item create(String itemName);
	}

	Map<String, ItemConstructionFunction> itemContructionFunctions;

	public Player createPlayer(final String name, final Vector2f position,
			final Vector2f velocity) {
		// we dont use position and velocity in client.. they are set by the network code
		final Player player = new Player(name);
		
		player.addBehavior(new PlayerRenderBehavior(player));
		player.addBehavior(new PlayerAnimationBehavior(player));

		return player;
	}

	public Background createBackground(final String name) {
		final Background background = new Background(name);
		background.addBehavior(new BackgroundRenderBehavior(background));
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

		// do something with pos and vel?
		final Item item = itemContructionFunctions.get(familyName).create(
				itemName);
		return item;
	}

	private Item createItemInternal(final String name, final String familyName,
			final String texPart, final Rectangle destRect) {
		final Item item = new Item(name, familyName);
		item.addBehavior(new ItemRenderBehavior(item, texPart, destRect));
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

		map.addBehavior(new MapRenderBehavior(map, width, height, tiles));

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
	private void addFunction(final String familyName, final String texPart,
			final Rectangle destRect, final Element el) {
		itemContructionFunctions.put(familyName,
				new ItemConstructionFunction() {

					@Override
					public Item create(final String itemName) {
						return createItemInternal(itemName, familyName,
								texPart, destRect);
					}
				});
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

			final String texture = reader.getRootElement().getAttribute(
					"texture");
			final String texName = reader.getRootElement().getAttribute(
					"texname");
			TextureManager.getInstance().loadFromFile(texture, texName);

			for (final Element cur : elList) {
				final String familyName = XMLReader.getTextValue(cur, "name");
				final int destWidth = XMLReader.getIntValue(cur, "width");
				final int destHeight = XMLReader.getIntValue(cur, "height");

				final Element texurePart = XMLReader.getFirstElement(cur,
						"texpart");
				final String texPartName = XMLReader.getTextValue(texurePart,
						"name");
				final int x = XMLReader.getIntValue(texurePart, "x");
				final int y = XMLReader.getIntValue(texurePart, "y");
				final int width = XMLReader.getIntValue(texurePart, "width");
				final int height = XMLReader.getIntValue(texurePart, "height");

				TexturePartManager.getInstance().createTexturePart(texPartName,
						texName, new Rectangle(x, y, width, height));

				addFunction(familyName, texPartName, new Rectangle(0, 0,
						destWidth, destHeight), cur);
			}

		}
		return false;
	}
}
