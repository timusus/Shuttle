package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ui.views.UpNextView;

import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;
import test.com.multisheetview.ui.view.MultiSheetView;

public class MainController extends BaseNavigationController {

    public static MainController newInstance() {

        Bundle args = new Bundle();

        MainController fragment = new MainController();
        fragment.setArguments(args);
        return fragment;
    }

    public MainController() {

    }

    private MultiSheetView multiSheetView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        multiSheetView = (MultiSheetView) rootView.findViewById(R.id.multiSheetView);

        if (savedInstanceState == null) {

            getChildFragmentManager()
                    .beginTransaction()
                    .add(multiSheetView.getSheet1ContainerResId(), PlayerFragment.newInstance())
                    .add(multiSheetView.getSheet1PeekViewResId(), MiniPlayerFragment.newInstance())
                    .add(multiSheetView.getSheet2ContainerResId(), QueueFragment.newInstance())
                    .commit();

            ((ViewGroup) multiSheetView.findViewById(multiSheetView.getSheet2PeekViewResId())).addView(new UpNextView(getContext()));
        }

        return rootView;
    }

    @Override
    public FragmentInfo getRootViewControllerInfo() {
        return LibraryController.fragmentInfo();
    }

    @Override
    public boolean consumeBackPress() {

        if (multiSheetView.consumeBackPress()) {
            return true;
        }

        return super.consumeBackPress();
    }
}
