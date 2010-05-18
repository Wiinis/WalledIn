package walledin.game;

import walledin.game.entity.Entity;

public class Item extends Entity implements Cloneable {

	public Item(final String name, final String familyName) {
		super(name, familyName);
	}
}
