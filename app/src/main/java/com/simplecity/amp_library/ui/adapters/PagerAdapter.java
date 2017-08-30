package com.simplecity.amp_library.ui.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

public class PagerAdapter extends FragmentPagerAdapter {

    private static final String ARG_PAGE_TITLE = "page_title";

    /**
     * The data used in this {@link FragmentPagerAdapter}
     */
    private final List<Fragment> mFragments = new ArrayList<>();

    /**
     * Constructor for <code>PagerAdapter</code>
     *
     * @param fragmentManager The {@link FragmentManager} to use
     */
    public PagerAdapter(FragmentManager fragmentManager, boolean refreshPager) {
        super(fragmentManager);

        /*
		 * Removing all displayed fragments to avoid the problem when changing the displayed tabs
         */
        if (refreshPager) {
            List<Fragment> childFragments = fragmentManager.getFragments();
            if (childFragments != null && !childFragments.isEmpty()) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                for (Fragment childFragment : childFragments) {
                    if (childFragment != null) fragmentTransaction.remove(childFragment);
                }
                fragmentTransaction.commitAllowingStateLoss();
            }
        }
    }

    @Override
    public Fragment getItem(int position) {
        if (mFragments.size() == 0) {
            return null;
        }
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getItem(position).getArguments().getString(ARG_PAGE_TITLE);
    }

    /**
     * Builds the data for this {@link FragmentPagerAdapter}
     *
     * @param data The data to add to this {@link FragmentPagerAdapter}
     */
    public void addFragment(Fragment data) {
        mFragments.add(data);
        notifyDataSetChanged();
    }

    /**
     * Clears the data in this {@link FragmentPagerAdapter}
     */
    public void clear() {
        mFragments.clear();
        notifyDataSetChanged();
    }
}
