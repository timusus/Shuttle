package com.simplecity.amp_library.interfaces;

import androidx.annotation.IntDef;

@IntDef({ FileType.PARENT, FileType.FOLDER, FileType.FILE })
public @interface FileType {
    int PARENT = 0;
    int FOLDER = 1;
    int FILE = 2;
}
