package com.simplecity.amp_library.ui.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

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
    public PagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
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
