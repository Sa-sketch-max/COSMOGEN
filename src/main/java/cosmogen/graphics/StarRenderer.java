
package cosmogen.graphics;

import cosmogen.physics.CelestialBody;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import cosmogen.physics.BodyType;
import org.joml.Matrix4f;

public class StarRenderer {

    private final Shader shader;

    private final int vaoId;
    private final int vboId;
    private final int eboId;
    private final int indexCount;

    private static final float STAR_RADIUS = 5.0f;

    // 3D sphere vertex shader

    private static final String VERTEX_SHADER = """
    #version 330 core

    layout(location = 0) in vec3 aPos;
    layout(location = 1) in vec3 aNormal;

    uniform mat4 uProjView;
    uniform mat4 uModel;

    out vec3 Normal;

    void main() {

        Normal = normalize(aNormal);

        gl_Position =
            uProjView * uModel * vec4(aPos, 1.0);
    }
    """;


    // HDR emissive star shader

    private static final String FRAGMENT_SHADER = """
    #version 330 core

    in vec3 Normal;

    out vec4 FragColor;

    uniform vec3 uColor;
    uniform float uBrightness;

    void main() {

        vec3 N = normalize(Normal);

        float variation = 0.5 + 0.5 * N.y;

        vec3 darkColor = uColor * 0.45;
        vec3 brightColor = uColor;

        vec3 surfaceColor = mix(
            darkColor,
            brightColor,
            variation
        );

        vec3 hdrColor =
            surfaceColor * uBrightness;

        FragColor = vec4(hdrColor, 1.0);
    }
    """;


    public StarRenderer() {

        shader = new Shader(
                VERTEX_SHADER,
                FRAGMENT_SHADER
        );

        int sectorCount = 60;
        int stackCount = 60;

        float radius = 1.0f;

        ArrayList<Float> verticesList =
                new ArrayList<>();

        ArrayList<Integer> indicesList =
                new ArrayList<>();

        // Generate sphere positions and normals
        for (int i = 0; i <= stackCount; i++) {

            float stackAngle = (float) (
                    Math.PI / 2.0 -
                            i * Math.PI / stackCount
            );

            float xy = (float) (
                    radius * Math.cos(stackAngle)
            );

            float z = (float) (
                    radius * Math.sin(stackAngle)
            );

            for (int j = 0; j <= sectorCount; j++) {

                float sectorAngle = (float) (
                        j * 2.0 * Math.PI / sectorCount
                );

                float x = (float) (
                        xy * Math.sin(sectorAngle)
                );

                float y = z;

                float zCoord = (float) (
                        xy * Math.cos(sectorAngle)
                );

                // Position
                verticesList.add(x);
                verticesList.add(y);
                verticesList.add(zCoord);

                // Normal
                float length = (float) Math.sqrt(
                        x * x +
                                y * y +
                                zCoord * zCoord
                );

                verticesList.add(x / length);
                verticesList.add(y / length);
                verticesList.add(zCoord / length);
            }
        }

        // Correct outward-facing triangle winding
        for (int i = 0; i < stackCount; i++) {

            int k1 = i * (sectorCount + 1);
            int k2 = k1 + sectorCount + 1;

            for (int j = 0;
                 j < sectorCount;
                 j++, k1++, k2++) {

                if (i != 0) {

                    indicesList.add(k1);
                    indicesList.add(k1 + 1);
                    indicesList.add(k2);
                }

                if (i != stackCount - 1) {

                    indicesList.add(k1 + 1);
                    indicesList.add(k2 + 1);
                    indicesList.add(k2);
                }
            }
        }

        indexCount = indicesList.size();

        float[] vertices =
                new float[verticesList.size()];

        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = verticesList.get(i);
        }

        int[] indices =
                new int[indicesList.size()];

        for (int i = 0; i < indices.length; i++) {
            indices[i] = indicesList.get(i);
        }

        // Create VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Upload vertex data
        FloatBuffer vertexBuffer =
                MemoryUtil.memAllocFloat(vertices.length);

        vertexBuffer.put(vertices).flip();

        vboId = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        glBufferData(
                GL_ARRAY_BUFFER,
                vertexBuffer,
                GL_STATIC_DRAW
        );

        MemoryUtil.memFree(vertexBuffer);

        // Upload triangle indices
        IntBuffer indexBuffer =
                MemoryUtil.memAllocInt(indices.length);

        indexBuffer.put(indices).flip();

        eboId = glGenBuffers();

        glBindBuffer(
                GL_ELEMENT_ARRAY_BUFFER,
                eboId
        );

        glBufferData(
                GL_ELEMENT_ARRAY_BUFFER,
                indexBuffer,
                GL_STATIC_DRAW
        );

        MemoryUtil.memFree(indexBuffer);

        // Each vertex: position(3) + normal(3)
        int stride = 6 * Float.BYTES;

        // Vertex position
        glVertexAttribPointer(
                0,
                3,
                GL_FLOAT,
                false,
                stride,
                0L
        );

        glEnableVertexAttribArray(0);

        // Vertex normal
        glVertexAttribPointer(
                1,
                3,
                GL_FLOAT,
                false,
                stride,
                3L * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    // Draw the emissive 3D star

    public void render(
            Camera camera,
            List<CelestialBody> bodies) {

        shader.bind();

        shader.setUniformMat4(
                "uProjView",
                camera.getCombinedMatrix()
        );

        int program = glGetInteger(GL_CURRENT_PROGRAM);

        int colorLocation =
                glGetUniformLocation(program, "uColor");

        int brightnessLocation =
                glGetUniformLocation(program, "uBrightness");

        glBindVertexArray(vaoId);

        Matrix4f model = new Matrix4f();

        for (CelestialBody body : bodies) {

            model.identity()
                    .translate(body.getPosition())
                    .scale(body.getRadius());

            shader.setUniformMat4("uModel", model);

            glUniform3f(
                    colorLocation,
                    body.getColor().x,
                    body.getColor().y,
                    body.getColor().z
            );

            float brightness =
                    body.getType() == BodyType.STAR
                            ? 3.0f : 0.7f;

            glUniform1f(
                    brightnessLocation,
                    brightness
            );

            glDrawElements(
                    GL_TRIANGLES,
                    indexCount,
                    GL_UNSIGNED_INT,
                    0L
            );
        }

        glBindVertexArray(0);

        shader.unbind();
    }


    public void cleanup() {

        shader.cleanup();

        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
        glDeleteVertexArrays(vaoId);
    }
}


