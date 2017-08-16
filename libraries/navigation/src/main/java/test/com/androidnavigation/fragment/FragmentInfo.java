package test.com.androidnavigation.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class FragmentInfo implements Parcelable {

    public Class rootViewController;

    @Nullable public transient Bundle args;

    public String rootViewControllerTag;

    public FragmentInfo(Class rootViewController, @Nullable Bundle args, String rootViewControllerTag) {
        this.rootViewController = rootViewController;
        this.rootViewControllerTag = rootViewControllerTag;
        this.args = args;
    }

    public Fragment instantiateFragment(Context context) {
        return Fragment.instantiate(context, rootViewController.getName(), args);
    }

    @Override
    public String toString() {
        return "FragmentInfo{" +
                "rootViewController=" + rootViewController +
                ", rootViewControllerTag='" + rootViewControllerTag + '\'' +
                ", args=" + args +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FragmentInfo that = (FragmentInfo) o;

        if (rootViewController != null ? !rootViewController.equals(that.rootViewController) : that.rootViewController != null) return false;
        if (rootViewControllerTag != null ? !rootViewControllerTag.equals(that.rootViewControllerTag) : that.rootViewControllerTag != null) return false;
        return args != null ? args.equals(that.args) : that.args == null;

    }

    @Override
    public int hashCode() {
        int result = rootViewController != null ? rootViewController.hashCode() : 0;
        result = 31 * result + (rootViewControllerTag != null ? rootViewControllerTag.hashCode() : 0);
        result = 31 * result + (args != null ? args.hashCode() : 0);
        return result;
    }

    protected FragmentInfo(Parcel in) {
        rootViewController = (Class) in.readSerializable();
        args = in.readBundle(Bundle.class.getClassLoader());
        rootViewControllerTag = in.readString();
    }

    public static final Creator<FragmentInfo> CREATOR = new Creator<FragmentInfo>() {
        @Override
        public FragmentInfo createFromParcel(Parcel in) {
            return new FragmentInfo(in);
        }

        @Override
        public FragmentInfo[] newArray(int size) {
            return new FragmentInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(rootViewController);
        parcel.writeBundle(args);
        parcel.writeString(rootViewControllerTag);
    }
}