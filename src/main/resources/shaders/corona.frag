
#version 330 core

in vec3 vNormal;
in vec3 vWorldPos;

out vec4 FragColor;

uniform vec3 cameraPos;
uniform float time;

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(cameraPos - vWorldPos);

    float rim = 1.0 - abs(dot(N, V));
    rim = clamp(rim, 0.0, 1.0);

    // Concentrate light near the silhouette
    float glow = pow(rim, 4.0);

    // Gentle animation
    float pulse = 0.9 + 0.1 * sin(time * 1.5);

    vec3 color = mix(
            vec3(0.08, 0.3, 1.0),
            vec3(0.5, 0.85, 1.0),
            rim
    );

    float alpha = glow * 0.35 * pulse;

    FragColor = vec4(color, alpha);
}



