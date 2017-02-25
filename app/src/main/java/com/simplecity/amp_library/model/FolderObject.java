package com.simplecity.amp_library.model;

import com.simplecity.amp_library.interfaces.FileType;

public class FolderObject extends BaseFileObject {

    public int fileCount;
    public int folderCount;

    public FolderObject() {
        this.fileType = FileType.FOLDER;
    }

    @Override
    public String toString() {
        return "FolderObject{" +
                "fileCount=" + fileCount +
                ", folderCount=" + folderCount +
                "} " + super.toString();
    }
}
