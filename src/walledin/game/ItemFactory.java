package walledin.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import walledin.engine.TextureManager;
import walledin.engine.TexturePartManager;
import walledin.engine.math.Rectangle;
import walledin.util.XMLReader;

public class ItemFactory {
	private interface ItemConstructionFunction {
		Item create(String itemName);
	}

	Map<String, ItemConstructionFunction> itemContructionFunctions;

	public ItemFactory() {
		itemContructionFunctions = new HashMap<String, ItemConstructionFunction>();
	}

	/**
	 * creates a function that can create an item of this family and adds it.
	 * 
	 * @param familyName
	 *            Name of the family. i.e healthkit, armourkit.
	 * @param texPart
	 *            Texture part
	 * @param el
	 *            Element in XML file
	 * @return Returns the created item
	 */
	private void addFunction(String familyName, final String texPart, final Element el) {
		if (familyName.equals("healthkit")) {
			itemContructionFunctions.put("healthkit",
					new ItemConstructionFunction() {

						@Override
						public Item create(String itemName) {
							return new HealthKitItem(itemName, texPart);
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
	public boolean loadFromXML(String filename) {
		XMLReader reader = new XMLReader();

		if (reader.open(filename)) {
			List<Element> elList = XMLReader.getElements(reader
					.getRootElement(), "item");

			String texture = reader.getRootElement().getAttribute("texture");
			String texName = reader.getRootElement().getAttribute("texname");
			TextureManager.getInstance().loadFromFile(texture, texName); // load
			// texture

			for (Element cur : elList) {
				String familyName = XMLReader.getTextValue(cur, "name");

				Element texurePart = XMLReader.getFirstElement(cur, "texpart");
				String texPartName = XMLReader.getTextValue(texurePart, "name");
				final int x = XMLReader.getIntValue(texurePart, "x");
				final int y = XMLReader.getIntValue(texurePart, "y");
				final int width = XMLReader.getIntValue(texurePart, "width");
				final int height = XMLReader.getIntValue(texurePart, "height");

				TexturePartManager.getInstance().createTexturePart(texPartName,
						texName, new Rectangle(x, y, width, height));

				addFunction(familyName, texPartName, cur);
			}

		}

		return false;
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
	public Item create(String familyName, String itemName) {
		if (!itemContructionFunctions.containsKey(familyName))
			throw new IllegalArgumentException(
					"Item "
							+ familyName
							+ " is not found in the database. Are the items loaded correctly?");

		return itemContructionFunctions.get(familyName).create(itemName);
	}
}
