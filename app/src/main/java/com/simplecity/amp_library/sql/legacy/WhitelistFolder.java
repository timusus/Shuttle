package com.simplecity.amp_library.sql.legacy;

import android.database.Cursor;

public class WhitelistFolder {

    public long id;
    public String folder;

    public WhitelistFolder(Cursor cursor) {
        id = cursor.getLong(0);
        folder = cursor.getString(1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WhitelistFolder that = (WhitelistFolder) o;

        if (id != that.id) return false;
        return folder != null ? folder.equals(that.folder) : that.folder == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (folder != null ? folder.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return folder;
    }
}
