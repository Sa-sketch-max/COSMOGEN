
#version 330 core

in vec3 vNormal;
in vec3 vWorldPos;

out vec4 FragColor;

uniform vec3 cameraPos;
uniform float time;

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(cameraPos - vWorldPos);

    float fresnel = pow(
        1.0 - abs(dot(N, V)),
        3.0
    );

    float pulse = 0.9 + 0.1 * sin(time * 2.0);

    vec3 innerColor = vec3(1.0, 0.65, 0.12);
    vec3 outerColor = vec3(1.0, 0.18, 0.02);

    vec3 color = mix(
        innerColor,
        outerColor,
        fresnel
    );

    float alpha = fresnel * 0.65 * pulse;

    FragColor = vec4(color * 1.8, alpha);
}
