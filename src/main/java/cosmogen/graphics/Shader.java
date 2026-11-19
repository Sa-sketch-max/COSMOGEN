package cosmogen.graphics;

import static org.lwjgl.opengl.GL20.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class Shader {

    // Just like GLFW gave us a 'long' handle for the window,
    // OpenGL gives us an 'int' ID for every GPU Shader Program.
    private final int programId;

    public Shader(String vertexSource, String fragmentSource) {
        // 1. Compile the Vertex Shader (calculates 3D positions)
        int vertexShaderId = compileShader(GL_VERTEX_SHADER, vertexSource);

        // 2. Compile the Fragment Shader (calculates pixel colors)
        int fragmentShaderId = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        // 3. Create an empty GPU Program and attach both compiled shaders to it
        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);

        // 4. Link them together into a single executable GPU pipeline
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader Program Linking Failed:\n" + glGetProgramInfoLog(programId));
        }

        // 5. Once linked into the program, we can delete the individual shader objects
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    private int compileShader(int type, String source) {
        // Ask the GPU to allocate a new shader slot of 'type' (Vertex or Fragment)
        int shaderId = glCreateShader(type);

        // Upload our GLSL String to the GPU and compile it
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check if the GPU compiler found any syntax errors in our GLSL code
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String typeName = (type == GL_VERTEX_SHADER) ? "VERTEX" : "FRAGMENT";
            throw new RuntimeException(typeName + " Shader Compilation Failed:\n" + glGetShaderInfoLog(shaderId));
        }

        return shaderId;
    }

    // Tell OpenGL: "Use this shader pipeline for whatever I draw next"
    public void bind() {
        glUseProgram(programId);
    }

    // Tell OpenGL: "Stop using this shader"
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * Uploads a JOML 4x4 Matrix into a GLSL 'uniform mat4' variable on the GPU.
     */
    public void setUniformMat4(String uniformName, Matrix4f matrix) {
        int location = glGetUniformLocation(programId, uniformName);
        if (location == -1) {
            throw new RuntimeException("CRITICAL: Uniform '" + uniformName + "' not found in shader!");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    // Free the GPU program when closing the application
    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }

    // Sends a 4x4 JOML Matrix (16 floats) from Java into a 'uniform mat4' variable inside GLSL
    public void setUniform(String name, Matrix4f matrix) {
        // 1. Ask the GPU where the variable 'name' (e.g., "uProjection") lives in the shader
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            // 2. Use LWJGL's fast off-heap MemoryStack to hold the 16 floats temporarily
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniformMatrix4fv(location, false, matrix.get(stack.mallocFloat(16)));
            }
        }
    }
}
