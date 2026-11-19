package cosmogen.core;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import cosmogen.graphics.StarRenderer;
import cosmogen.graphics.Camera;
import cosmogen.input.InputManager;

public class Application {

    private final String title;
    private int width;
    private int height;
    private long windowHandle;
    private StarRenderer starRenderer;
    private Camera camera;
    private InputManager inputManager;

    public Application(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void run() {
        System.out.println("Starting " + title);
        System.out.println("LWJGL Version: " + Version.getVersion());

        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);


        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new IllegalStateException("Failed to create the GLFW window");
        }

        System.out.println("Window created! Memoty address (handle): " + windowHandle);

        glfwSetFramebufferSizeCallback(windowHandle, (window, newWidth, newHeight) -> {
            this.width = newWidth;
            this.height = newHeight;
            glViewport(0, 0, newWidth, newHeight);
            if (camera != null) {
                camera.updateProjection(newWidth, newHeight);
            }
        });

        glfwMakeContextCurrent(windowHandle);

        glfwSwapInterval(1);

        GL.createCapabilities();

        System.out.println("OpenGL Vendor:   " + glGetString(GL_VENDOR));
        System.out.println("OpenGL Renderer: " + glGetString(GL_RENDERER));
        System.out.println("OpenGL Version:  " + glGetString(GL_VERSION));

        glEnable(GL_DEPTH_TEST);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        starRenderer = new StarRenderer();

        camera = new Camera(width, height);
        inputManager = new InputManager(windowHandle, camera);
        starRenderer = new StarRenderer();

    }

    private void loop() {

        glClearColor(0.02f, 0.02f, 0.06f, 1.0f);

        while (!glfwWindowShouldClose(windowHandle)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // 1. Poll keyboard movement
            inputManager.update();

            // 2. Render our 3D scene through the Camera's lens
            starRenderer.render(camera);

            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }
    }

    private void cleanup() {

        if (starRenderer != null) {
            starRenderer.cleanup();
        }

        // Free any input listeners attached to the window and destroy the native window
        Callbacks.glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);

        // Shut down the GLFW library and free the error printer
        glfwTerminate();
        var errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
        System.out.println("CosmoGen shut down cleanly.");
    }
}