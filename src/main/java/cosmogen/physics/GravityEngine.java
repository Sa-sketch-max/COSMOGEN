
package cosmogen.physics;

import org.joml.Vector3f;
import java.util.List;

public class GravityEngine {

    private static final float G = BodySystem.G;

    private static final float SOFTENING = 0.1f;

    private static final float STAR_MU = 120.0f;
    private static final float MIN_DISTANCE = 5.0f;

    // Gravitational interaction between celestial bodies
    public static void updateBodies(
            List<CelestialBody> bodies,
            float deltaTime) {

        // Reset acceleration
        for (CelestialBody body : bodies) {
            body.resetAcceleration();
        }

        Vector3f direction = new Vector3f();
        Vector3f acceleration = new Vector3f();

        // Calculate gravitational interactions
        for (int i = 0; i < bodies.size(); i++) {

            CelestialBody a = bodies.get(i);

            for (int j = i + 1; j < bodies.size(); j++) {

                CelestialBody b = bodies.get(j);

                direction.set(b.getPosition())
                        .sub(a.getPosition());

                float distanceSquared =
                        direction.lengthSquared()
                                + SOFTENING * SOFTENING;

                float distance =
                        (float) Math.sqrt(distanceSquared);

                float factor =
                        G / (distanceSquared * distance);

                // Acceleration of A towards B
                acceleration.set(direction)
                        .mul(factor * b.getMass());

                a.addAcceleration(acceleration);

                // Acceleration of B towards A
                acceleration.set(direction)
                        .mul(-factor * a.getMass());

                b.addAcceleration(acceleration);
            }
        }

        // Update all bodies after calculating gravity
        for (CelestialBody body : bodies) {
            body.update(deltaTime);
        }
    }

    // Existing particle gravity (temporary)

    public static void applyGravity(
            Particle particle,
            List<CelestialBody> bodies,
            float deltaTime) {

        Vector3f position = particle.getPosition();

        float ax = 0.0f;
        float ay = 0.0f;
        float az = 0.0f;

        for (CelestialBody body : bodies) {

            float dx = body.getPosition().x - position.x;
            float dy = body.getPosition().y - position.y;
            float dz = body.getPosition().z - position.z;

            float distanceSquared =
                    dx * dx + dy * dy + dz * dz
                            + SOFTENING * SOFTENING;

            float distance =
                    (float) Math.sqrt(distanceSquared);

            float factor = G * body.getMass()
                    / (distanceSquared * distance);

            ax += dx * factor;
            ay += dy * factor;
            az += dz * factor;
        }

        particle.getVelocity().add(
                ax * deltaTime,
                ay * deltaTime,
                az * deltaTime
        );
    }

}
