package walledin.engine.math;

public class Circle {
	private Vector2f pos;
	private float radius;

	public Circle(final Vector2f pos, final float radius) {
		super();
		this.pos = pos;
		this.radius = radius;
	}

	public Circle() {
		pos = new Vector2f();
	}

	public Vector2f getPos() {
		return pos;
	}

	public float getRadius() {
		return radius;
	}

	public void setPos(final Vector2f pos) {
		this.pos = pos;
	}

	public Circle addPos(final Vector2f pos) {
		return new Circle(this.pos.add(pos), radius);
	}

	public void setRadius(final float radius) {
		this.radius = radius;
	}

	public boolean pointInSphere(final Vector2f pos) {
		return pos.sub(this.pos).lengthSquared() < radius * radius;
	}

	public boolean intersects(final Circle circ) {
		return (getRadius() + circ.getRadius())
				* (getRadius() + circ.getRadius()) > getPos()
				.sub(circ.getPos()).lengthSquared();
	}

	public static Circle fromRect(final Rectangle rect) {
		final Vector2f center = rect.getLeftTop().add(rect.getRightBottom())
				.scale(0.5f).add(rect.getLeftTop());
		final float radius = (float) (0.5 * Math.sqrt(rect.getWidth()
				+ rect.getHeight()));
		return new Circle(center, radius);
	}

}
