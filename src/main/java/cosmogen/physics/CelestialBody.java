
package cosmogen.physics;

import org.joml.Vector3f;

public class CelestialBody {

    private final BodyType type;

    private final Vector3f position;
    private final Vector3f velocity;
    private final Vector3f acceleration;
    private final Vector3f color;

    private float mass;
    private float radius;

    public CelestialBody(
            BodyType type,
            Vector3f position,
            Vector3f velocity,
            Vector3f color,
            float mass,
            float radius) {

        this.type = type;

        this.position = new Vector3f(position);
        this.velocity = new Vector3f(velocity);
        this.color = new Vector3f(color);

        this.acceleration = new Vector3f();

        this.mass = mass;
        this.radius = radius;
    }

    public BodyType getType() {
        return type;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public Vector3f getAcceleration() {
        return acceleration;
    }

    public Vector3f getColor() {
        return color;
    }

    public float getMass() {
        return mass;
    }

    public float getRadius() {
        return radius;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void resetAcceleration() {
        acceleration.zero();
    }

    public void addAcceleration(Vector3f value) {
        acceleration.add(value);
    }

    public void update(float deltaTime) {

        velocity.fma(deltaTime, acceleration);

        position.fma(deltaTime, velocity);
    }
}
