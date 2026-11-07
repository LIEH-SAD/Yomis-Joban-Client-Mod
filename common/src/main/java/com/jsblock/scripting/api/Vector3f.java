package com.jsblock.scripting.api;

import net.minecraft.core.BlockPos;

/**
 * A mutable, chainable 3-component vector exposed to scripts as the {@code Vector3f} global.<br>
 * JCM 2.x documents {@code vec.add(0, 1, 0).mul(2)} as mutating the receiver, so the operations
 * here modify {@code this} and return it; {@link #copy()} is the way to get a fresh value.
 * @author LX86 (JCM)
 */
public class Vector3f {
    public static final Vector3f ZERO = new Vector3f(0, 0, 0);
    public static final Vector3f XP = new Vector3f(1, 0, 0);
    public static final Vector3f YP = new Vector3f(0, 1, 0);
    public static final Vector3f ZP = new Vector3f(0, 0, 1);

    private double x;
    private double y;
    private double z;

    public Vector3f() {
        this(0, 0, 0);
    }

    public Vector3f(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public Vector3f copy() {
        return new Vector3f(x, y, z);
    }

    public Vector3f add(double dx, double dy, double dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    public Vector3f add(Vector3f other) {
        return other == null ? this : add(other.x, other.y, other.z);
    }

    public Vector3f sub(Vector3f other) {
        return other == null ? this : add(-other.x, -other.y, -other.z);
    }

    public Vector3f mul(double n) {
        return mul(n, n, n);
    }

    public Vector3f mul(double mx, double my, double mz) {
        x *= mx;
        y *= my;
        z *= mz;
        return this;
    }

    /** Scales the vector to unit length; a zero-length vector is left untouched. */
    public Vector3f normalize() {
        final double length = Math.sqrt(x * x + y * y + z * z);
        return length == 0 ? this : mul(1 / length);
    }

    public Vector3f rotX(double radians) {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);
        final double ny = y * cos - z * sin;
        final double nz = y * sin + z * cos;
        y = ny;
        z = nz;
        return this;
    }

    public Vector3f rotY(double radians) {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);
        final double nx = x * cos + z * sin;
        final double nz = -x * sin + z * cos;
        x = nx;
        z = nz;
        return this;
    }

    public Vector3f rotZ(double radians) {
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);
        final double nx = x * cos - y * sin;
        final double ny = x * sin + y * cos;
        x = nx;
        y = ny;
        return this;
    }

    /** Replaces this vector with the cross product of itself and {@code other}. */
    public Vector3f cross(Vector3f other) {
        if (other == null) return this;
        final double nx = y * other.z - z * other.y;
        final double ny = z * other.x - x * other.z;
        final double nz = x * other.y - y * other.x;
        x = nx;
        y = ny;
        z = nz;
        return this;
    }

    public double distance(Vector3f other) {
        return Math.sqrt(distanceSq(other));
    }

    public double distanceSq(Vector3f other) {
        if (other == null) return 0;
        final double dx = x - other.x;
        final double dy = y - other.y;
        final double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** @return the block this vector points at, floored, so scripts can hand it to MTR APIs */
    public BlockPos rawBlockPos() {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector3f)) return false;
        final Vector3f other = (Vector3f) obj;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public String toString() {
        return "Vector3f[x=" + x + ", y=" + y + ", z=" + z + "]";
    }
}
