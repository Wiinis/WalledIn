package walledin.game;

import walledin.game.entity.Attribute;
import walledin.game.entity.Entity;
import walledin.game.entity.behaviors.ItemRenderBehavior;
import walledin.game.entity.behaviors.SpatialBehavior;
import walledin.math.Rectangle;
import walledin.math.Vector2f;

public class Item extends Entity implements Cloneable {

	public Item(final String familyName, final String name, final String texPart,
			final Rectangle destRect, final Vector2f position,
			final Vector2f velocity) {
		super(familyName, name);

		addBehavior(new SpatialBehavior(this, position, velocity));
		addBehavior(new ItemRenderBehavior(this, texPart, destRect));

		setAttribute(Attribute.BOUNDING_RECT, destRect);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

}
