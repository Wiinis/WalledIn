package walledin.game;

import java.util.Collection;
import java.util.List;

import walledin.engine.math.Circle;
import walledin.engine.math.Rectangle;
import walledin.engine.math.Vector2f;
import walledin.game.entity.Attribute;
import walledin.game.entity.Entity;
import walledin.game.entity.MessageType;
import walledin.game.map.GameMap;
import walledin.game.map.Tile;

public class CollisionManager {

	static private Tile tileFromPixel(GameMap map, Vector2f pos) {
		float tileSize = map.getAttribute(Attribute.RENDER_TILE_SIZE);
		int width = map.getAttribute(Attribute.WIDTH);

		List<Tile> tiles = map.getAttribute(Attribute.TILES);

		return tiles.get((int) (pos.x / tileSize) + width
				* (int) (pos.y / tileSize));
	}

	static public void calculateEntityCollisions(Collection<Entity> entities,
			double delta) {
		Entity[] entArray = new Entity[0];
		entArray = entities.toArray(entArray);

		for (int i = 0; i < entArray.length - 1; i++)
			for (int j = i + 1; j < entArray.length; j++) {
				if (!entArray[i].hasAttribute(Attribute.BOUNDING_RECT)
						|| !entArray[j].hasAttribute(Attribute.BOUNDING_RECT))
					continue;

				Circle circA = entArray[i]
						.getAttribute(Attribute.BOUNDING_CIRCLE);
				Circle circB = entArray[j]
						.getAttribute(Attribute.BOUNDING_CIRCLE);
				
				circA = circA.addPos((Vector2f)entArray[i].getAttribute(Attribute.POSITION));
				circB = circB.addPos((Vector2f)entArray[j].getAttribute(Attribute.POSITION));

				if (!circA.intersects(circB))
					continue;

				Rectangle rectA = entArray[i]
						.getAttribute(Attribute.BOUNDING_RECT);
				Rectangle rectB = entArray[j]
						.getAttribute(Attribute.BOUNDING_RECT);
				
				rectA = rectA.translate((Vector2f)entArray[i].getAttribute(Attribute.POSITION));
				rectB = rectB.translate((Vector2f)entArray[j].getAttribute(Attribute.POSITION));

				if (!rectA.intersects(rectB))
					continue;

				entArray[i].sendMessage(MessageType.COLLIDED, entArray[j]);
				entArray[j].sendMessage(MessageType.COLLIDED, entArray[i]);
			}
	}

	static public void calculateMapCollisions(GameMap map,
			Collection<Entity> entities, double delta) {
		float tileSize = map.getAttribute(Attribute.RENDER_TILE_SIZE);

		for (Entity ent : entities)
			if (ent.hasAttribute(Attribute.BOUNDING_RECT) && !ent.equals(map)) {

				Vector2f vel = ent.getAttribute(Attribute.VELOCITY);

				if (vel.x == 0 && vel.y == 0)
					continue;

				vel = vel.scale((float) delta); // velocity per frame
				Rectangle rect = ent.getAttribute(Attribute.BOUNDING_RECT);
				Vector2f curPos = ent.getAttribute(Attribute.POSITION);
				Vector2f oldPos = curPos.sub(vel);

				float x = curPos.x; // new x position after collision
				float y = curPos.y; // new y position after collision
				float eps = 0.001f; // small value to prevent floating errors

				// VERTICAL CHECK - move vertically only
				rect = rect.setPos(new Vector2f(oldPos.x, curPos.y));

				// check the four edges
				Tile lt = tileFromPixel(map, rect.getLeftTop());
				Tile lb = tileFromPixel(map, rect.getLeftBottom());
				Tile rt = tileFromPixel(map, rect.getRightTop());
				Tile rb = tileFromPixel(map, rect.getRightBottom());

				// bottom check
				if (vel.y > 0
						&& (lb.getType().isSolid() || rb.getType().isSolid())) {
					int rest = (int) (rect.getBottom() / tileSize);
					y = rest * tileSize - rect.getHeight() - eps;
				} else
				// top check
				if (vel.y < 0
						&& (lt.getType().isSolid() || rt.getType().isSolid())) {
					int rest = (int) (rect.getTop() / tileSize);
					y = (rest + 1) * tileSize + eps;
				}

				// HORIZONTAL CHECK - move horizontally only
				rect = rect.setPos(new Vector2f(curPos.x, y));

				lt = tileFromPixel(map, rect.getLeftTop());
				lb = tileFromPixel(map, rect.getLeftBottom());
				rt = tileFromPixel(map, rect.getRightTop());
				rb = tileFromPixel(map, rect.getRightBottom());

				// right check
				if (vel.x > 0
						&& (rt.getType().isSolid() || rb.getType().isSolid())) {
					int rest = (int) (rect.getRight() / tileSize);
					x = rest * tileSize - rect.getWidth() - eps;
				} else
				// left check
				if (vel.x < 0
						&& (lt.getType().isSolid() || lb.getType().isSolid())) {
					int rest = (int) (rect.getLeft() / tileSize);
					x = (rest + 1) * tileSize + eps;
				}

				ent.setAttribute(Attribute.POSITION, new Vector2f(x, y));
				ent.setAttribute(Attribute.VELOCITY, new Vector2f(x - oldPos.x,
						y - oldPos.y));
			}

	}

}