package walledin.game;

import java.util.List;

import org.w3c.dom.Element;

import walledin.engine.TextureManager;
import walledin.engine.TexturePartManager;
import walledin.game.entity.Attribute;
import walledin.game.entity.Entity;
import walledin.game.entity.behaviors.BackgroundRenderBehavior;
import walledin.game.entity.behaviors.ItemRenderBehavior;
import walledin.game.entity.behaviors.MapRenderBehavior;
import walledin.game.entity.behaviors.PlayerAnimationBehavior;
import walledin.game.entity.behaviors.PlayerRenderBehavior;
import walledin.math.Rectangle;
import walledin.util.XMLReader;

public class ClientEntityFactory extends AbstractEntityFactory {
	public ClientEntityFactory() {
		super();
		addCreationFunctions();
	}

	private Entity createPlayer(final Entity player) {
		// TODO spatial is missing?
		player.setAttribute(Attribute.ORIENTATION, 1); // start looking to
		// the right

		player.addBehavior(new PlayerAnimationBehavior(player));
		player.addBehavior(new PlayerRenderBehavior(player));

		// FIXME correct the drawing instead of the hack the bounding box
		player.setAttribute(Attribute.BOUNDING_RECT,
				new Rectangle(0, 0, 44, 43));

		return player;
	}

	private Entity createBackground(final Entity ent) {
		ent.addBehavior(new BackgroundRenderBehavior(ent));
		return ent;
	}

	private Entity createGameMap(final Entity map) {
		map.addBehavior(new MapRenderBehavior(map));
		return map;
	}

	private Entity createBullet(final String texPart, final Rectangle destRect,
			final Element el, final Entity bl) {
		bl.addBehavior(new ItemRenderBehavior(bl, texPart, destRect));
		return bl;
	}

	private Entity createArmorKit(final String texPart,
			final Rectangle destRect, final Element el, final Entity ak) {
		ak.addBehavior(new ItemRenderBehavior(ak, texPart, destRect));
		return ak;
	}

	private Entity createHealthKit(final String texPart,
			final Rectangle destRect, final Element el, final Entity hk) {
		hk.addBehavior(new ItemRenderBehavior(hk, texPart, destRect));
		return hk;
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
	 * @param destRect
	 *            Where to draw the texture part
	 * @param el
	 *            Element in XML file which contains item specific information,
	 *            like health kit strength or armor penetration value
	 */
	private void addFunction(final String familyName, final String texPart,
			final Rectangle destRect, final Element el) {

		if (familyName.equals("healthkit")) {
			entityContructionFunctions.put(familyName,
					new EntityConstructionFunction() {

						@Override
						public Entity create(final Entity hk) {
							return createHealthKit(texPart, destRect, el, hk);
						}
					});
		}

		if (familyName.equals("armourkit")) {
			entityContructionFunctions.put(familyName,
					new EntityConstructionFunction() {

						@Override
						public Entity create(final Entity ak) {
							return createArmorKit(texPart, destRect, el, ak);
						}
					});
		}

		if (familyName.equals("bullet")) {
			entityContructionFunctions.put(familyName,
					new EntityConstructionFunction() {

						@Override
						public Entity create(final Entity bl) {
							return createBullet(texPart, destRect, el, bl);

						}
					});
		}
	}

	private void addCreationFunctions() {
		entityContructionFunctions.put("Player",
				new EntityConstructionFunction() {

					@Override
					public Entity create(final Entity ent) {
						return createPlayer(ent);
					}
				});

		entityContructionFunctions.put("Background",
				new EntityConstructionFunction() {

					@Override
					public Entity create(final Entity ent) {
						return createBackground(ent);
					}
				});

		entityContructionFunctions.put("Map", new EntityConstructionFunction() {

			@Override
			public Entity create(final Entity ent) {
				return createGameMap(ent);
			}
		});
	}

	/**
	 * @see walledin.game.EntityFactory#loadItemsFromXML(java.lang.String)
	 */
	@Override
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
