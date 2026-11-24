
package cosmogen.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ScreenRenderer {

    private final int vao;
    private final int vbo;
    private final Shader shader;

    private static final String VERTEX_SHADER = """
        #version 330 core

        layout(location = 0) in vec2 aPos;
        layout(location = 1) in vec2 aTexCoord;

        out vec2 TexCoord;

        void main() {
            TexCoord = aTexCoord;
            gl_Position = vec4(aPos, 0.0, 1.0);
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 330 core

        in vec2 TexCoord;

        out vec4 FragColor;

        uniform sampler2D sceneTexture;
        uniform sampler2D bloomTexture;

        void main() {

            vec3 scene = texture(
                sceneTexture, TexCoord
            ).rgb;

            vec3 bloom = texture(
                bloomTexture, TexCoord
            ).rgb;

            float bloomStrength = 0.65;

            vec3 hdrColor =
                scene + bloom * bloomStrength;

            // HDR tone mapping
            vec3 mapped =
                vec3(1.0) - exp(-hdrColor);

            // Gamma correction
            mapped = pow(
                mapped,
                vec3(1.0 / 2.2)
            );

            FragColor = vec4(mapped, 1.0);
        }
        """;

    public ScreenRenderer() {

        shader = new Shader(
                VERTEX_SHADER,
                FRAGMENT_SHADER
        );

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
                0,
                2,
                GL_FLOAT,
                false,
                4 * Float.BYTES,
                0L
        );

        glEnableVertexAttribArray(0);

        glVertexAttribPointer(
                1,
                2,
                GL_FLOAT,
                false,
                4 * Float.BYTES,
                2L * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Assign texture units
        shader.bind();

        int program = glGetInteger(GL_CURRENT_PROGRAM);

        glUniform1i(
                glGetUniformLocation(program, "sceneTexture"),
                0
        );

        glUniform1i(
                glGetUniformLocation(program, "bloomTexture"),
                1
        );

        shader.unbind();
    }

    public void render(int sceneTexture, int bloomTexture) {

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);

        shader.bind();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTexture);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, bloomTexture);

        glBindVertexArray(vao);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        glBindVertexArray(0);

        shader.unbind();

        glActiveTexture(GL_TEXTURE0);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    public void cleanup() {

        shader.cleanup();

        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
