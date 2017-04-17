package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;

import java.util.List;

import test.com.androidnavigation.fragment.BaseController;
import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;

public class MainController extends BaseNavigationController implements LibraryController.OtherClickListener {

    public static MainController newInstance() {

        Bundle args = new Bundle();

        MainController fragment = new MainController();
        fragment.setArguments(args);
        return fragment;
    }

    public MainController() {

    }

    @Override
    public FragmentInfo getRootViewControllerInfo() {
        return LibraryController.fragmentInfo();
    }

    @Override
    public void onItemClick(String transitionName, List<Pair<View, String>> sharedElements) {

        BaseController controller = DetailController.newInstance(transitionName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Transition moveTransition = TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move);
            controller.setSharedElementEnterTransition(moveTransition);
            controller.setSharedElementReturnTransition(moveTransition);
        }

        pushViewController(controller, "DetailController", sharedElements);
    }
}
