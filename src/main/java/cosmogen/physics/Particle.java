package cosmogen.physics;

import org.joml.Vector3f;

public class Particle {

    private final Vector3f position;
    private final Vector3f velocity;
    private final float mass;

    public Particle(Vector3f position,
                    Vector3f velocity,
                    float mass) {

        this.position = new Vector3f(position);
        this.velocity = new Vector3f(velocity);
        this.mass = mass;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public float getMass() {
        return mass;
    }

    public void update(float deltaTime) {
        position.fma(deltaTime, velocity);
    }
}