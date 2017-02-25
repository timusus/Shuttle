package com.simplecity.amp_library.ui.views;

import android.content.Context;
import android.graphics.LightingColorFilter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.Breadcrumb;
import com.simplecity.amp_library.interfaces.BreadcrumbListener;
import com.simplecity.amp_library.utils.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A view that holds the navigation breadcrumb pattern
 */
public class BreadcrumbView extends RelativeLayout implements Breadcrumb, OnClickListener {

    HorizontalScrollView mScrollView;
    private ViewGroup mBreadcrumbBar;
    private int mTextColor = -1;

    private List<BreadcrumbListener> mBreadcrumbListeners;

    /**
     * Constructor of <code>BreadcrumbView</code>
     *
     * @param context
     */
    public BreadcrumbView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>BreadcrumbView</code>
     *
     * @param context
     * @param attrs   The attributes of the XML tag that is inflating the view
     */
    public BreadcrumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>BreadcrumbView</code>
     *
     * @param context
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *                 will be applied (beyond what is included in the theme). This may
     *                 either be an attribute resource, whose value will be retrieved
     *                 from the current theme, or an explicit style resource.
     */
    public BreadcrumbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Initialises the view. Loads all necessary
     * information and creates an appropriate layout for the view
     */
    private void init() {
        //Initialise the listeners
        this.mBreadcrumbListeners = Collections.synchronizedList(new ArrayList<BreadcrumbListener>());

        //Add the view of the breadcrumb
        addView(inflate(getContext(), R.layout.breadcrumb_view, null));

        //Recover all views
        this.mScrollView = (HorizontalScrollView) findViewById(R.id.breadcrumb_scrollview);
        this.mBreadcrumbBar = (ViewGroup) findViewById(R.id.breadcrumb);

    }

    @Override
    public void addBreadcrumbListener(BreadcrumbListener listener) {
        this.mBreadcrumbListeners.add(listener);
    }

    @Override
    public void removeBreadcrumbListener(BreadcrumbListener listener) {
        this.mBreadcrumbListeners.remove(listener);
    }

    @Override
    public void setTextColor(int textColor) {
        mTextColor = textColor;
    }

    @Override
    public void changeBreadcrumbPath(final String newPath) {

        if (TextUtils.isEmpty(newPath)) {
            return;
        }

        //Remove all views
        this.mBreadcrumbBar.removeAllViews();

        this.mBreadcrumbBar.addView(createBreadcrumbItem(new File(FileHelper.ROOT_DIRECTORY)));

        //Add the rest of the path
        String[] dirs = newPath.split(File.separator);
        int cc = dirs.length;

        for (int i = 1; i < cc; i++) {
            this.mBreadcrumbBar.addView(createItemDivider());
            this.mBreadcrumbBar.addView(createBreadcrumbItem(createFile(dirs, i)));
        }

        //Set scrollbar at the end
        this.mScrollView.post(() -> BreadcrumbView.this.mScrollView.fullScroll(View.FOCUS_RIGHT));
    }

    /**
     * Creates a new path divider
     *
     * @return View divider icon
     */
    private ImageView createItemDivider() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView imageView = (ImageView) inflater.inflate(R.layout.breadcrumb_item_divider, this.mBreadcrumbBar, false);
        imageView.setColorFilter(new LightingColorFilter(mTextColor, 0));
        return imageView;
    }

    /**
     * Creates a new split path
     *
     * @param dir The path
     * @return BreadcrumbItem The view to create
     */
    private BreadcrumbItem createBreadcrumbItem(File dir) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        BreadcrumbItem item = (BreadcrumbItem) inflater.inflate(R.layout.breadcrumb_item, this.mBreadcrumbBar, false);
        item.setText(dir.getName().length() != 0 ? dir.getName() : dir.getPath());
        item.setItemPath(dir.getPath());
        item.setOnClickListener(this);
        item.setTextColor(mTextColor);

        return item;
    }

    /**
     * Creates the a new file reference for a partial breadcrumb item.
     *
     * @param dirs The split strings directory
     * @param pos  The position up to which to create
     * @return File The file reference
     */
    private File createFile(String[] dirs, int pos) {
        File parent = new File(FileHelper.ROOT_DIRECTORY);
        for (int i = 1; i < pos; i++) {
            parent = new File(parent, dirs[i]);
        }
        return new File(parent, dirs[pos]);
    }

    @Override
    public void onClick(View v) {
        BreadcrumbItem item = (BreadcrumbItem) v;
        int cc = this.mBreadcrumbListeners.size();
        for (int i = 0; i < cc; i++) {
            this.mBreadcrumbListeners.get(i).onBreadcrumbItemClick(item);
        }
    }
}
