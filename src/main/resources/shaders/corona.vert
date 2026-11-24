
#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;

uniform mat4 uProjView;
uniform float uRadius;

out vec3 vNormal;
out vec3 vWorldPos;

void main() {
    vec3 worldPos = aPos * uRadius;

    vWorldPos = worldPos;
    vNormal = normalize(aNormal);

    gl_Position = uProjView * vec4(worldPos, 1.0);
}
