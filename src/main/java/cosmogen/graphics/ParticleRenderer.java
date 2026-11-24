
package cosmogen.graphics;

import cosmogen.physics.Particle;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class ParticleRenderer {

    private final int vaoId;
    private final int vboId;

    private final Shader shader;

    private final float[] positions;
    private final int maxParticles;

    public ParticleRenderer(int maxParticles) {

        this.maxParticles = maxParticles;
        positions = new float[maxParticles * 3];

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        glBufferData(
                GL_ARRAY_BUFFER,
                (long) maxParticles * 3 * Float.BYTES,
                GL_DYNAMIC_DRAW
        );

        glVertexAttribPointer(
                0, 3, GL_FLOAT,
                false, 3 * Float.BYTES, 0L
        );

        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public void render(Camera camera, List<Particle> particles) {

        int count = Math.min(particles.size(), maxParticles);

        for (int i = 0; i < count; i++) {

            Vector3f position = particles.get(i).getPosition();

            positions[i * 3] = position.x;
            positions[i * 3 + 1] = position.y;
            positions[i * 3 + 2] = position.z;
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, positions);

        shader.bind();

        shader.setUniformMat4(
                "uProjView",
                camera.getCombinedMatrix()
        );

        glEnable(GL_PROGRAM_POINT_SIZE);

        glBindVertexArray(vaoId);

        glDrawArrays(GL_POINTS, 0, count);

        glBindVertexArray(0);

        shader.unbind();
    }

    public void cleanup() {

        shader.cleanup();

        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }

    private static final String VERTEX_SHADER = """
            #version 330 core

            layout(location = 0) in vec3 aPos;

            uniform mat4 uProjView;

            void main() {

                gl_Position = uProjView * vec4(aPos, 1.0);

                gl_PointSize = 3.0;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core

            out vec4 FragColor;

            void main() {

                float distanceFromCenter =
                    length(gl_PointCoord - vec2(0.5));

                if (distanceFromCenter > 0.5)
                    discard;

                float intensity =
                    1.0 - smoothstep(
                        0.0, 0.5, distanceFromCenter
                    );

                vec3 color = vec3(0.7, 0.85, 1.0);

                FragColor = vec4(color * intensity * 2.0, 1.0);
            }
            """;
}

