
package cosmogen.core;

import cosmogen.graphics.*;
import cosmogen.input.InputManager;

import cosmogen.physics.BodySystem;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import cosmogen.physics.ParticleSystem;
import cosmogen.graphics.ParticleRenderer;
import cosmogen.physics.BodySystem;

public class Application {

    private final String title;
    private int width;
    private int height;
    private long windowHandle;
    private ParticleSystem particleSystem;
    private ParticleRenderer particleRenderer;
    private BodySystem bodySystem;

    private StarRenderer starRenderer;
    private Camera camera;
    private InputManager inputManager;

    private HDRFramebuffer hdrFramebuffer;
    private ScreenRenderer screenRenderer;
    private BloomRenderer bloomRenderer;

    public Application(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void run() {
        System.out.println("Starting " + title);
        System.out.println("LWJGL Version: " + Version.getVersion());

        init();

        try {
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException(
                    "Unable to initialize GLFW"
            );
        }

        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        glfwWindowHint(
                GLFW_OPENGL_PROFILE,
                GLFW_OPENGL_CORE_PROFILE
        );

        glfwWindowHint(
                GLFW_OPENGL_FORWARD_COMPAT,
                GLFW_TRUE
        );

        windowHandle = glfwCreateWindow(
                width,
                height,
                title,
                NULL,
                NULL
        );

        if (windowHandle == NULL) {
            throw new IllegalStateException(
                    "Failed to create GLFW window"
            );
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1);

        GL.createCapabilities();

        System.out.println(
                "OpenGL Vendor: " + glGetString(GL_VENDOR)
        );

        System.out.println(
                "OpenGL Renderer: " + glGetString(GL_RENDERER)
        );

        System.out.println(
                "OpenGL Version: " + glGetString(GL_VERSION)
        );

        // 3D rendering configuration
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        // Camera and input
        camera = new Camera(width, height);
        inputManager = new InputManager(windowHandle, camera);
        bodySystem = new BodySystem();

        // Main 3D renderer
        starRenderer = new StarRenderer();
        particleSystem = new ParticleSystem();
        particleRenderer = new ParticleRenderer(20000);

        // HDR and bloom renderers
        hdrFramebuffer = new HDRFramebuffer(width, height);
        screenRenderer = new ScreenRenderer();
        bloomRenderer = new BloomRenderer(width, height);

        // Handle framebuffer resizing
        glfwSetFramebufferSizeCallback(
                windowHandle,
                (window, newWidth, newHeight) -> {

                    this.width = newWidth;
                    this.height = newHeight;

                    if (newWidth <= 0 || newHeight <= 0) {
                        return;
                    }

                    glViewport(
                            0,
                            0,
                            newWidth,
                            newHeight
                    );

                    if (camera != null) {
                        camera.updateProjection(
                                newWidth,
                                newHeight
                        );
                    }

                    if (hdrFramebuffer != null) {
                        hdrFramebuffer.resize(
                                newWidth,
                                newHeight
                        );
                    }

                    if (bloomRenderer != null) {
                        bloomRenderer.resize(
                                newWidth,
                                newHeight
                        );
                    }
                }
        );
    }


    private void loop() {

        double lastTime = glfwGetTime();

        while (!glfwWindowShouldClose(windowHandle)) {

            // Calculate delta time
            double currentTime = glfwGetTime();

            float deltaTime = (float) (currentTime - lastTime);

            lastTime = currentTime;

            deltaTime = Math.min(deltaTime, 0.05f);

            // Handle input
            glfwPollEvents();
            inputManager.update();

            if (glfwGetKey(windowHandle, GLFW_KEY_F)
                    == GLFW_PRESS) {

                bodySystem.launchIncomingStar();
            }

            if (width <= 0 || height <= 0) {
                glfwWaitEvents();
                continue;
            }

            // Update physics

            particleSystem.update(
                    deltaTime,
                    bodySystem.getBodies()
            );

            bodySystem.update(deltaTime);

            // Render HDR scene
            hdrFramebuffer.bind();

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);

            glClearColor(0.005f, 0.005f, 0.015f, 1.0f);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            starRenderer.render(camera, bodySystem.getBodies());

            particleRenderer.render(
                    camera,
                    particleSystem.getParticles()
            );

            hdrFramebuffer.unbind();

            // Calculate bloom
            int bloomTexture = bloomRenderer.render(
                    hdrFramebuffer.getColorTexture()
            );

            // Render final image
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            glViewport(0, 0, width, height);

            glDisable(GL_BLEND);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            screenRenderer.render(
                    hdrFramebuffer.getColorTexture(),
                    bloomTexture
            );

            glfwSwapBuffers(windowHandle);
        }
    }


    private void cleanup() {

        if (bloomRenderer != null) {
            bloomRenderer.cleanup();
        }

        if (screenRenderer != null) {
            screenRenderer.cleanup();
        }

        if (hdrFramebuffer != null) {
            hdrFramebuffer.cleanup();
        }

        if (starRenderer != null) {
            starRenderer.cleanup();
        }

        particleRenderer.cleanup();

        if (windowHandle != NULL) {
            Callbacks.glfwFreeCallbacks(windowHandle);
            glfwDestroyWindow(windowHandle);
        }

        glfwTerminate();

        GLFWErrorCallback errorCallback =
                glfwSetErrorCallback(null);

        if (errorCallback != null) {
            errorCallback.free();
        }

        System.out.println(
                "CosmoGen shut down cleanly."
        );
    }
}
