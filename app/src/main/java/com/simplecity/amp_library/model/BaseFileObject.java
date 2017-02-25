package com.simplecity.amp_library.model;

import com.simplecity.amp_library.interfaces.FileType;

import java.io.File;
import java.io.Serializable;

public class BaseFileObject implements Serializable {

    public String name;
    public String path;
    public long size;

    @FileType
    public int fileType;

    public File getParent() {
        File file = new File(path);
        return file.getParentFile();
    }

    @Override
    public String toString() {
        return "BaseFileObject{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", fileType=" + fileType +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseFileObject that = (BaseFileObject) o;

        if (size != that.size) return false;
        if (fileType != that.fileType) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return path != null ? path.equals(that.path) : that.path == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + fileType;
        return result;
    }
}