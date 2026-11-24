
package cosmogen.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

public class HDRFramebuffer {

    private int fbo;
    private int colorTexture;
    private int depthBuffer;

    private int width;
    private int height;

    public HDRFramebuffer(int width, int height) {
        resize(width, height);
    }

    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        cleanup();

        this.width = width;
        this.height = height;

        // Create framebuffer
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // Create floating-point color texture
        colorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTexture);

        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA16F,
                width,
                height,
                0,
                GL_RGBA,
                GL_FLOAT,
                0L
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER,
                GL_LINEAR
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MAG_FILTER,
                GL_LINEAR
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_S,
                GL_CLAMP_TO_EDGE
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_T,
                GL_CLAMP_TO_EDGE
        );

        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                colorTexture,
                0
        );

        // Create depth buffer
        depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);

        glRenderbufferStorage(
                GL_RENDERBUFFER,
                GL_DEPTH_COMPONENT24,
                width,
                height
        );

        glFramebufferRenderbuffer(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_RENDERBUFFER,
                depthBuffer
        );

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER)
                != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException(
                    "HDR Framebuffer is incomplete!"
            );
        }

        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getColorTexture() {
        return colorTexture;
    }

    public void cleanup() {
        if (depthBuffer != 0) {
            glDeleteRenderbuffers(depthBuffer);
            depthBuffer = 0;
        }

        if (colorTexture != 0) {
            glDeleteTextures(colorTexture);
            colorTexture = 0;
        }

        if (fbo != 0) {
            glDeleteFramebuffers(fbo);
            fbo = 0;
        }
    }
}

