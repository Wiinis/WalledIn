package walledin.game.entity;

import java.util.List;

import walledin.game.Background;
import walledin.game.Item;
import walledin.game.Player;
import walledin.game.map.GameMap;
import walledin.game.map.Tile;

public interface EntityFactory {
	Background createBackground(final String name);

	GameMap createGameMap(final String name, final int width, final int height,
			final List<Tile> tiles, final List<Item> items);

	Item createItem(final String familyName, final String itemName);

	Player createPlayer(final String name);
}