package com.voxel.registry;

import java.util.Objects;

public final class Identifier implements Comparable<Identifier> {
    public static final String DEFAULT_NAMESPACE = "voxel";

    private final String namespace;
    private final String path;

    public Identifier(String namespace, String path) {
        if (namespace == null || namespace.isBlank()) {
            this.namespace = DEFAULT_NAMESPACE;
        } else {
            this.namespace = namespace.toLowerCase().trim();
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Identifier path cannot be null or empty");
        }
        this.path = path.toLowerCase().trim();
    }

    public Identifier(String path) {
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            this.namespace = parts[0].toLowerCase().trim();
            this.path = parts[1].toLowerCase().trim();
        } else {
            this.namespace = DEFAULT_NAMESPACE;
            this.path = path.toLowerCase().trim();
        }
    }

    public static Identifier of(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    public static Identifier of(String combined) {
        return new Identifier(combined);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifier that = (Identifier) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public int compareTo(Identifier o) {
        int cmp = this.namespace.compareTo(o.namespace);
        return cmp != 0 ? cmp : this.path.compareTo(o.path);
    }
}
