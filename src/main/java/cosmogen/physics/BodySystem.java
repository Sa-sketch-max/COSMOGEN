
package cosmogen.physics;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class BodySystem {

    public static final float G = 1.0f;

    private final List<CelestialBody> bodies =
            new ArrayList<>();

    private boolean incomingStarLaunched = false;

    public BodySystem() {
        createSingleStarSystem();
    }

    public void createSingleStarSystem() {

        bodies.clear();

        incomingStarLaunched = false;

        CelestialBody star = new CelestialBody(
                BodyType.STAR,
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 0),
                new Vector3f(0.3f, 0.6f, 1.0f),
                120.0f,
                4.0f
        );

        bodies.add(star);
    }

    public void launchIncomingStar() {

        if (incomingStarLaunched) {
            return;
        }

        CelestialBody incomingStar = new CelestialBody(
                BodyType.STAR,
                new Vector3f(-100, 0, 15),
                new Vector3f(2.0f, 0, 0),
                new Vector3f(1.0f, 0.5f, 0.15f),
                600.0f,
                7.0f
        );

        bodies.add(incomingStar);

        incomingStarLaunched = true;

        System.out.println("Incoming star launched!");
    }

    public void createBinarySystem() {

        bodies.clear();

        incomingStarLaunched = false;

        float mass = 120.0f;
        float separation = 20.0f;

        float speed = (float) Math.sqrt(
                G * mass / (2.0f * separation)
        );

        CelestialBody star1 = new CelestialBody(
                BodyType.STAR,
                new Vector3f(-10, 0, 0),
                new Vector3f(0, 0, -speed),
                new Vector3f(0.3f, 0.6f, 1.0f),
                mass,
                4.0f
        );

        CelestialBody star2 = new CelestialBody(
                BodyType.STAR,
                new Vector3f(10, 0, 0),
                new Vector3f(0, 0, speed),
                new Vector3f(1.0f, 0.5f, 0.15f),
                mass,
                4.0f
        );

        bodies.add(star1);
        bodies.add(star2);
    }

    public void update(float deltaTime) {

        GravityEngine.updateBodies(
                bodies,
                deltaTime
        );
    }

    public List<CelestialBody> getBodies() {
        return bodies;
    }

    public void addBody(CelestialBody body) {
        bodies.add(body);
    }

    public void removeBody(CelestialBody body) {
        bodies.remove(body);
    }

    public void clear() {
        bodies.clear();
        incomingStarLaunched = false;
    }
}


