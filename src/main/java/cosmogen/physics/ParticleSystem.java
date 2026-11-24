
package cosmogen.physics;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSystem {

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private static final int PARTICLE_COUNT = 20000;

    private static final float INNER_RADIUS = 8.0f;
    private static final float OUTER_RADIUS = 25.0f;

    private static final float STAR_MU = 120.0f;

    public ParticleSystem() {
        generateParticles();
    }

    private void generateParticles() {

        for (int i = 0; i < PARTICLE_COUNT; i++) {

            float angle = random.nextFloat()
                    * (float) (2.0 * Math.PI);

            float radius = (float) Math.sqrt(
                    INNER_RADIUS * INNER_RADIUS
                            + random.nextFloat() * (
                            OUTER_RADIUS * OUTER_RADIUS
                                    - INNER_RADIUS * INNER_RADIUS
                    )
            );

            float x = radius * (float) Math.cos(angle);
            float z = radius * (float) Math.sin(angle);

            // Small vertical variation creates a thin disk
            float y = (random.nextFloat() - 0.5f) * 0.4f;

            Vector3f position = new Vector3f(x, y, z);

            // Circular orbital velocity: v = sqrt(GM/r)
            float speed = (float) Math.sqrt(STAR_MU / radius);

            Vector3f velocity = new Vector3f(
                    -(float) Math.sin(angle) * speed,
                    0.0f,
                    (float) Math.cos(angle) * speed
            );

            particles.add(
                    new Particle(position, velocity, 1.0f)
            );
        }
    }

    public List<Particle> getParticles() {
        return particles;
    }


    public void update(
            float deltaTime,
            List<CelestialBody> bodies) {

        for (Particle particle : particles) {

            GravityEngine.applyGravity(
                    particle,
                    bodies,
                    deltaTime
            );

            particle.update(deltaTime);
        }
    }

}

