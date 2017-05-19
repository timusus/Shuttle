package test.com.androidnavigation.fragment;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import test.com.androidnavigation.base.Controller;
import test.com.androidnavigation.base.NavigationController;

/**
 * An abstract implementation of {@link Controller}, which can be used as a base for {@link Fragment}s
 * who wish to be aware of their parent {@link NavigationController}
 */
public abstract class BaseController extends Fragment implements Controller<Fragment> {

    @NonNull
    @Override
    public NavigationController<Fragment> getNavigationController() {
        return findNavigationController(this);
    }

    /**
     * Traverses the fragment hierarchy searching for the first available {@link NavigationController}.
     * If none are found, then this method checks whether the parent {@link android.app.Activity} is
     * a {@link NavigationController} and returns that instead.
     *
     * @param fragment the fragment whose hierarchy will be searched.
     */
    @NonNull
    public static NavigationController<Fragment> findNavigationController(@NonNull Fragment fragment) {

        Fragment parent = fragment.getParentFragment();

        if (parent instanceof NavigationController) {
            return (NavigationController) parent;
        }

        if (parent != null) {
            return findNavigationController(parent);
        } else {
            if (fragment.getActivity() instanceof NavigationController) {
                return (NavigationController) fragment.getActivity();
            }
        }

        throw new IllegalStateException("Couldn't find parent navigation controller.");
    }
}