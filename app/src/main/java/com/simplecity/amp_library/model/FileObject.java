package com.simplecity.amp_library.model;

import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.utils.FileHelper;
import com.simplecity.amp_library.utils.StringUtils;

public class FileObject extends BaseFileObject {

    public String extension;

    public TagInfo tagInfo;

    private long duration = 0;

    public FileObject() {
        this.fileType = FileType.FILE;
    }

    public String getTimeString() {
        if (duration == 0) {
            duration = FileHelper.getDuration(ShuttleApplication.getInstance(), this);
        }
        return StringUtils.makeTimeString(ShuttleApplication.getInstance(), duration / 1000);
    }

    @Override
    public String toString() {
        return "FileObject{" +
                "extension='" + extension + '\'' +
                ", size='" + size + '\'' +
                "} " + super.toString();
    }
}
