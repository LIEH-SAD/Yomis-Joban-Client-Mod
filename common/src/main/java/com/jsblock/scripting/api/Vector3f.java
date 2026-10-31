package com.jsblock.scripting.api;

/**
 * An immutable 3-component vector handed to scripts, e.g. by {@code pids.blockPos()}.
 * Mirrors JCM 2.3's {@code ScriptVector3f} so that scripts can read {@code vec.x()}.
 * @author LX86 (JCM)
 */
public class Vector3f {
    public static final Vector3f ZERO = new Vector3f(0, 0, 0);

    private final double x;
    private final double y;
    private final double z;

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

    public Vector3f add(double x, double y, double z) {
        return new Vector3f(this.x + x, this.y + y, this.z + z);
    }

    public Vector3f add(Vector3f other) {
        return other == null ? copy() : add(other.x, other.y, other.z);
    }

    public Vector3f sub(Vector3f other) {
        return other == null ? copy() : new Vector3f(x - other.x, y - other.y, z - other.z);
    }

    public Vector3f mul(double n) {
        return new Vector3f(x * n, y * n, z * n);
    }

    public Vector3f mul(double x, double y, double z) {
        return new Vector3f(this.x * x, this.y * y, this.z * z);
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
