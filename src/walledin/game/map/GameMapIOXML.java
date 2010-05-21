package walledin.game.map;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import walledin.game.EntityFactory;
import walledin.game.EntityManager;
import walledin.game.Item;
import walledin.game.entity.Attribute;
import walledin.game.entity.Entity;
import walledin.game.entity.behaviors.MapRenderBehavior;
import walledin.math.Vector2f;
import walledin.util.XMLReader;

/**
 * Loads a map from an XML file
 * 
 * @author ben
 */
public class GameMapIOXML implements GameMapIO {
	// FIXME dont use instance vars for this...
	private int width;
	private int height;
	private final EntityFactory entityFactory;
	
	public GameMapIOXML(EntityFactory entityFactory) {
		this.entityFactory = entityFactory;
	}

	/**
	 * Reads tile information
	 * 
	 * @param reader
	 *            XML reader
	 * @param element
	 *            Current element
	 * @return An list of tiles
	 */
	private List<Tile> parseTiles(final Element element) {
		final List<Tile> result = new ArrayList<Tile>();
		final String tiles = XMLReader.getTextValue(element, "tiles");
		final String[] rows = tiles.split("\n");
		int x = 0;
		int y = 0;
		for (final String row : rows) {
			if (row.trim().isEmpty()) {
				continue;
			}
			y = 0;
			for (final char mapChar : row.trim().toCharArray()) {
				final TileType type = TileType.getTile(mapChar);
				final Tile tile = new Tile(type, x, y);
				result.add(tile);
				y++;
			}
			x++;
		}
		height = x;
		width = y;
		return result;
	}

	private List<Item> parseItems(final Element element) {
		final List<Item> itList = new ArrayList<Item>();
		final Element itemsNode = XMLReader.getFirstElement(element, "items");
		final List<Element> items = XMLReader.getElements(itemsNode, "item");

		for (final Element el : items) {
			final String name = el.getAttribute("name");
			final String type = el.getAttribute("type");
			final int x = Integer.parseInt(el.getAttribute("x"));
			final int y = Integer.parseInt(el.getAttribute("y"));

			final Item item = entityFactory.createItem(type, name, 
					new Vector2f(x, y), new Vector2f(0, 0));
			itList.add(item);
		}

		return itList;
	}

	/**
	 * Reads map data from an XML file
	 * 
	 * @param filename
	 *            Filename of XML data
	 * @return Returns true on success and false on failure
	 */
	@Override
	public Entity readFromFile(final String filename) {
		final XMLReader reader = new XMLReader();

		if (reader.open(filename)) {
			final Element map = reader.getRootElement();

			final String name = XMLReader.getTextValue(map, "name");
			final List<Item> items = parseItems(map);
			final List<Tile> tiles = parseTiles(map);

			final Entity m = new Entity("Map", name);
			m.setAttribute(Attribute.WIDTH, width);
			m.setAttribute(Attribute.HEIGHT, height);
			m.setAttribute(Attribute.TILES, tiles);
			m.setAttribute(Attribute.ITEM_LIST, items);
			
			m.addBehavior(new MapRenderBehavior(m, width, height, tiles));
			
			EntityManager.getInstance().add(m);
			EntityManager.getInstance().add((List<Entity>)(ArrayList)items);

			return m;
		} else {
			return null;
		}

	}

	@Override
	public boolean writeToFile(final Entity map, final String filename) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
