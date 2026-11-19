package cosmogen.input;

import cosmogen.graphics.Camera;

import static java.awt.SystemColor.window;
import static org.lwjgl.glfw.GLFW.*;

public class InputManager {

    private final long windowHandle;
    private final Camera camera;

    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;
    private boolean firstMouse = true;

    private boolean isDraggingRotate = false;

    private static final float MOUSE_SENSITIVITY = 0.25f;
    private static final float KEY_PAN_SPEED = 0.8f;

    public InputManager(long windowHandle, Camera camera) {
        this.windowHandle = windowHandle;
        this.camera = camera;
        initCallbacks();
    }

    private void initCallbacks() {
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT || button == GLFW_MOUSE_BUTTON_RIGHT) {
                if (action == GLFW_PRESS) {
                    isDraggingRotate = true;
                    firstMouse = true;
                } else if (action == GLFW_RELEASE) {
                    isDraggingRotate = false;
                }
            }
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }

            float dx = (float) (xpos - lastMouseX);
            float dy = (float) (ypos - lastMouseY);
            lastMouseX = xpos;
            lastMouseY = ypos;

            if (isDraggingRotate) {
                camera.rotate(dx * MOUSE_SENSITIVITY, dy * MOUSE_SENSITIVITY);
            }
        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            camera.zoom((float) yoffset);
        });
    }

    public void update(){
        float deltaRight = 0.0f;
        float deltaUp = 0.0f;
        float deltaForward = 0.0f;

        if(glfwGetKey(windowHandle, GLFW_KEY_W) == GLFW_PRESS) deltaForward +=KEY_PAN_SPEED;
        if(glfwGetKey(windowHandle, GLFW_KEY_S) == GLFW_PRESS) deltaForward -=KEY_PAN_SPEED;
        if(glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS) deltaRight +=KEY_PAN_SPEED;
        if(glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS) deltaRight -=KEY_PAN_SPEED;

        if(glfwGetKey(windowHandle, GLFW_KEY_E) == GLFW_PRESS) deltaUp +=KEY_PAN_SPEED;
        if(glfwGetKey(windowHandle, GLFW_KEY_Q) == GLFW_PRESS) deltaUp -=KEY_PAN_SPEED;

        if(deltaRight != 0.0f || deltaUp != 0.0f || deltaForward != 0.0f) {
            camera.pan(deltaRight, deltaUp, deltaForward);
        }

        if(glfwGetKey(windowHandle, GLFW_KEY_R) == GLFW_PRESS) {
            camera.reset();
        }
    }
}
