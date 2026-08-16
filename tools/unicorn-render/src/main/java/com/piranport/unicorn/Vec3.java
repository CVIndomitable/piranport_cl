package com.piranport.unicorn;

/** 3D 向量 */
public class Vec3 {
    public float x, y, z;
    public Vec3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public Vec3(float[] t) { this.x = t[0]; this.y = t[1]; this.z = t[2]; }
}
