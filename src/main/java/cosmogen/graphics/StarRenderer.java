package cosmogen.graphics;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class StarRenderer {

    private final Shader shader;
    private final int vaoId;
    private final int vboId;
    private final int eboId;
    private final int indexCount;

    // 1. VERTEX SHADER: Transforms 3D mesh vertices and normals into camera space
    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            
            uniform mat4 uProjView;
            
            out vec3 FragPos;
            out vec3 Normal;
            
            void main() {
                // Scale our unit sphere to radius 5.0 units in the universe
                vec3 scaledPos = aPos * 5.0;
                
                FragPos = scaledPos;
                Normal = aNormal; // Surface normal for 3D lighting
                
                gl_Position = uProjView * vec4(scaledPos, 1.0);
            }
            """;

    // 2. FRAGMENT SHADER: Computes 3D surface lighting and a glowing corona
    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec3 FragPos;
            in vec3 Normal;
            
            out vec4 FragColor;
            
            void main() {
                // Simple pseudo-lighting: brighter on top/front, glowing core
                vec3 norm = normalize(Normal);
                float brightness = max(dot(norm, vec3(0.0, 1.0, 0.5)), 0.2);
                
                // Hot core color mixing with a radiant corona
                vec3 starColor = mix(vec3(0.1, 0.4, 1.0), vec3(0.95, 0.98, 1.0), brightness);
                
                FragColor = vec4(starColor * 2.5, 1.0);
            }
            """;

    public StarRenderer() {
        this.shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);

        // Procedurally generate a 3D UV Sphere mesh (Latitude / Longitude rings)
        int sectorCount = 30; // Horizontal resolution
        int stackCount = 30;  // Vertical resolution
        float radius = 1.0f;

        var verticesList = new java.util.ArrayList<Float>();
        var indicesList = new java.util.ArrayList<Integer>();

        for (int i = 0; i <= stackCount; ++i) {
            float stackAngle = (float) (Math.PI / 2 - i * Math.PI / stackCount); // from pi/2 to -pi/2
            float xy = (float) (radius * Math.cos(stackAngle));
            float z = (float) (radius * Math.sin(stackAngle));

            for (int j = 0; j <= sectorCount; ++j) {
                float sectorAngle = (float) (j * 2 * Math.PI / sectorCount); // from 0 to 2pi

                // Vertex position (X, Y, Z)
                float x = (float) (xy * Math.sin(sectorAngle));
                float y = z;
                float zCoord = (float) (xy * Math.cos(sectorAngle));
                verticesList.add(x);
                verticesList.add(y);
                verticesList.add(zCoord);

                // Normal vector (same as normalized position for a sphere)
                float length = (float) Math.sqrt(x * x + y * y + zCoord * zCoord);
                verticesList.add(x / length);
                verticesList.add(y / length);
                verticesList.add(zCoord / length);
            }
        }

        // Generate sphere indices for triangles
        for (int i = 0; i < stackCount; ++i) {
            int k1 = i * (sectorCount + 1);
            int k2 = k1 + sectorCount + 1;

            for (int j = 0; j < sectorCount; ++j, ++k1, ++k2) {
                if (i != 0) {
                    indicesList.add(k1);
                    indicesList.add(k2);
                    indicesList.add(k1 + 1);
                }
                if (i != (stackCount - 1)) {
                    indicesList.add(k1 + 1);
                    indicesList.add(k2);
                    indicesList.add(k2 + 1);
                }
            }
        }

        indexCount = indicesList.size();

        // Convert lists to primitive arrays
        float[] vertexArray = new float[verticesList.size()];
        for (int i = 0; i < verticesList.size(); i++) vertexArray[i] = verticesList.get(i);

        int[] indexArray = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) indexArray[i] = indicesList.get(i);

        // Create VAO, VBO, and EBO (Element Buffer Object for triangles)
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertexArray.length);
        vertexBuffer.put(vertexArray).flip();

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);

        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indexArray.length);
        indexBuffer.put(indexArray).flip();

        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);

        int stride = 6 * Float.BYTES; // 3 floats for Position + 3 floats for Normal
        // Attribute 0: Position
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // Attribute 1: Normal
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render(Camera camera) {
        shader.bind();
        shader.setUniformMat4("uProjView", camera.getCombinedMatrix());

        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);

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
