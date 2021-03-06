package reach.project.coreViews.explore;

import android.support.annotation.NonNull;

import reach.project.R;
import reach.project.utils.EnumHelper;

/**
 * Created by dexter on 15/10/15.
 */
enum ExploreTypes implements EnumHelper<String> {

    MUSIC(R.layout.explore_music),
    APP(R.layout.explore_app),
    MISC(R.layout.explore_misc);

    private final int layoutResId;

    ExploreTypes(int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public int getLayoutResId() {
        return layoutResId;
    }

    @NonNull
    @Override
    public String getName() {
        return name();
    }
}