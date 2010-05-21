package walledin.game.entity;

import java.util.List;

import walledin.game.Item;
import walledin.game.map.Tile;
import walledin.math.Vector2f;

public interface EntityFactory {
	Background createBackground(final String name);

	GameMap createGameMap(final String name, final int width, final int height,
			final List<Tile> tiles, final List<Item> items);

	Item createItem(final String familyName, final String itemName,
			final Vector2f position, final Vector2f velocity);

	Player createPlayer(final String name, final Vector2f position,
			final Vector2f velocity);
}
