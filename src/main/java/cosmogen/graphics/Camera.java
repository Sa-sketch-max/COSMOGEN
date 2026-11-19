package cosmogen.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    // Spherical orbital coordinates (in degrees and world units)
    private float distance = 50.0f;
    private float yaw = 0.0f;
    private float pitch = 25.0f;

    // 3D Vectors for camera state
    private final Vector3f position = new Vector3f();
    private final Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

    // Helper vectors for panning calculations (reused to avoid GC allocations)
    private final Vector3f forward = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();

    // JOML 4x4 Matrices sent to the Vertex Shader
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f combinedMatrix = new Matrix4f(); // Projection * View

    // Lens settings
    private static final float FOV = (float) Math.toRadians(60.0f);
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 10000.0f;

    public Camera(int width, int height) {
        updateProjection(width, height);
        updateViewMatrix();
    }

    /**
     * Recalculates the perspective lens whenever the window is resized.
     */
    public void updateProjection(int width, int height) {
        if (height == 0) height = 1; // Prevent divide-by-zero when minimized
        float aspectRatio = (float) width / (float) height;
        projectionMatrix.identity().perspective(FOV, aspectRatio, NEAR_PLANE, FAR_PLANE);
        updateViewMatrix();
    }

    /**
     * Converts spherical coordinates (distance, yaw, pitch) into 3D position
     * and builds the LookAt View Matrix.
     */
    public void updateViewMatrix() {
        // Clamp pitch between -89 and +89 degrees to prevent camera "gimbal lock" flipping
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);

        // Spherical to Cartesian conversion
        float horizontalDistance = (float) (distance * Math.cos(radPitch));
        float verticalDistance = (float) (distance * Math.sin(radPitch));

        position.x = target.x + (float) (horizontalDistance * Math.sin(radYaw));
        position.y = target.y + verticalDistance;
        position.z = target.z + (float) (horizontalDistance * Math.cos(radYaw));

        // Build the View Matrix looking from 'position' toward 'target'
        viewMatrix.identity().lookAt(position, target, up);

        // Pre-multiply Projection * View for fast shader usage
        projectionMatrix.mul(viewMatrix, combinedMatrix);
    }

    /**
     * Rotates the camera around the current target point (Mouse Left/Right Drag).
     */
    public void rotate(float deltaYaw, float deltaPitch) {
        this.yaw -= deltaYaw;
        this.pitch += deltaPitch;
        updateViewMatrix();
    }

    /**
     * Zooms the camera in or out smoothly (Mouse Scroll Wheel).
     */
    public void zoom(float scrollOffset) {
        // Scale zoom speed proportionally to current distance so zooming feels natural both close-up and far away
        distance -= scrollOffset * (distance * 0.1f);
        if (distance < 1.0f) distance = 1.0f;
        if (distance > 5000.0f) distance = 5000.0f;

        System.out.println("Camera Distance: " + distance);

        updateViewMatrix();
    }

    /**
     * Pans the camera target across space (WASD keys or Middle-Mouse Drag).
     */
    public void pan(float deltaRight, float deltaUp, float deltaForward) {
        // Calculate the camera's local forward, right, and up axes
        target.sub(position, forward).normalize();
        forward.cross(up, right).normalize();
        right.cross(forward, cameraUp).normalize();

        // Scale movement speed by distance so panning is fast when zoomed out and precise when zoomed in
        float panSpeed = distance * 0.015f;

        target.fma(deltaRight * panSpeed, right);
        target.fma(deltaUp * panSpeed, cameraUp);
        target.fma(deltaForward * panSpeed, forward);

        updateViewMatrix();
    }

    /**
     * Resets camera back to the origin (0, 0, 0).
     */
    public void reset() {
        target.set(0.0f, 0.0f, 0.0f);
        distance = 50.0f;
        yaw = 0.0f;
        pitch = 25.0f;
        updateViewMatrix();
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getCombinedMatrix() {
        return combinedMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getTarget() {
        return target;
    }

    public float getDistance() {
        return distance;
    }


}

