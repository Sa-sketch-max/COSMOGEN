
package cosmogen.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BloomRenderer {

    private final int[] fbos = new int[2];
    private final int[] textures = new int[2];

    private final int vao;
    private final int vbo;

    private final Shader extractShader;
    private final Shader blurShader;

    private int width;
    private int height;

    private static final String VERTEX = """
        #version 330 core

        layout(location = 0) in vec2 aPos;
        layout(location = 1) in vec2 aTexCoords;

        out vec2 TexCoords;

        void main() {
            TexCoords = aTexCoords;
            gl_Position = vec4(aPos, 0.0, 1.0);
        }
        """;

    private static final String EXTRACT = """
        #version 330 core

        in vec2 TexCoords;
        out vec4 FragColor;

        uniform sampler2D sceneTexture;

        void main() {
            vec3 color = texture(sceneTexture, TexCoords).rgb;

            float brightness = dot(
                color,
                vec3(0.2126, 0.7152, 0.0722)
            );

            // Smooth threshold to avoid a hard glow boundary
            float mask = smoothstep(0.8, 1.4, brightness);

            FragColor = vec4(color * mask, 1.0);
        }
        """;

    private static final String BLUR = """
        #version 330 core

        in vec2 TexCoords;
        out vec4 FragColor;

        uniform sampler2D image;
        uniform int horizontal;

        void main() {
            vec2 texelSize = 1.0 / textureSize(image, 0);

            float weight[5] = float[](
                0.227027,
                0.1945946,
                0.1216216,
                0.054054,
                0.016216
            );

            vec3 result = texture(image, TexCoords).rgb
                          * weight[0];

            for (int i = 1; i < 5; i++) {
                vec2 offset = horizontal == 1
                    ? vec2(texelSize.x * i, 0.0)
                    : vec2(0.0, texelSize.y * i);

                result += texture(
                    image, TexCoords + offset
                ).rgb * weight[i];

                result += texture(
                    image, TexCoords - offset
                ).rgb * weight[i];
            }

            FragColor = vec4(result, 1.0);
        }
        """;

    public BloomRenderer(int width, int height) {

        extractShader = new Shader(VERTEX, EXTRACT);
        blurShader = new Shader(VERTEX, BLUR);

        float[] vertices = {
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                1f,  1f, 1f, 1f,

                -1f, -1f, 0f, 0f,
                1f,  1f, 1f, 1f,
                -1f,  1f, 0f, 1f
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        glBufferData(
                GL_ARRAY_BUFFER,
                vertices,
                GL_STATIC_DRAW
        );

        glVertexAttribPointer(
                0, 2, GL_FLOAT, false,
                4 * Float.BYTES, 0L
        );
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(
                1, 2, GL_FLOAT, false,
                4 * Float.BYTES,
                2L * Float.BYTES
        );
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        resize(width, height);
    }

    public void resize(int width, int height) {

        if (width <= 0 || height <= 0) return;

        this.width = width;
        this.height = height;

        for (int i = 0; i < 2; i++) {

            if (textures[i] != 0) {
                glDeleteTextures(textures[i]);
            }

            if (fbos[i] != 0) {
                glDeleteFramebuffers(fbos[i]);
            }

            fbos[i] = glGenFramebuffers();
            textures[i] = glGenTextures();

            glBindFramebuffer(GL_FRAMEBUFFER, fbos[i]);
            glBindTexture(GL_TEXTURE_2D, textures[i]);

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
                    textures[i],
                    0
            );

            if (glCheckFramebufferStatus(GL_FRAMEBUFFER)
                    != GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException(
                        "Bloom framebuffer incomplete: " + i
                );
            }
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int render(int sceneTexture) {

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);

        glViewport(0, 0, width, height);

        glActiveTexture(GL_TEXTURE0);
        glBindVertexArray(vao);

        // Pass 1: Extract bright pixels
        glBindFramebuffer(GL_FRAMEBUFFER, fbos[0]);

        extractShader.bind();

        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        glDrawArrays(GL_TRIANGLES, 0, 6);

        extractShader.unbind();

        // Pass 2: Alternating horizontal/vertical blur
        int sourceIndex = 0;

        for (int i = 0; i < 20; i++) {

            int targetIndex = 1 - sourceIndex;

            glBindFramebuffer(
                    GL_FRAMEBUFFER,
                    fbos[targetIndex]
            );

            blurShader.bind();

            // Horizontal on even passes, vertical on odd
            int horizontal = (i % 2 == 0) ? 1 : 0;

            int location = glGetUniformLocation(
                    glGetInteger(GL_CURRENT_PROGRAM),
                    "horizontal"
            );

            glUniform1i(location, horizontal);

            glBindTexture(
                    GL_TEXTURE_2D,
                    textures[sourceIndex]
            );

            glDrawArrays(GL_TRIANGLES, 0, 6);

            blurShader.unbind();

            sourceIndex = targetIndex;
        }

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glEnable(GL_DEPTH_TEST);

        return textures[sourceIndex];
    }

    public void cleanup() {

        extractShader.cleanup();
        blurShader.cleanup();

        for (int i = 0; i < 2; i++) {
            if (textures[i] != 0) {
                glDeleteTextures(textures[i]);
            }
            if (fbos[i] != 0) {
                glDeleteFramebuffers(fbos[i]);
            }
        }

        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}

