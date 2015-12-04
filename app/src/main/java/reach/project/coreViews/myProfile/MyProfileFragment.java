package reach.project.coreViews.myProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.myProfile.apps.ApplicationFragment;
import reach.project.coreViews.myProfile.music.MyLibraryFragment;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class MyProfileFragment extends Fragment {

    private static WeakReference<MyProfileFragment> reference = null;

    public static MyProfileFragment newInstance() {

        MyProfileFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new MyProfileFragment());
        else
            Log.i("Ayush", "Reusing YourProfileAppFragment object :)");
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.activity_your_profile, container, false);
        final MaterialViewPager materialViewPager = (MaterialViewPager) rootView.findViewById(R.id.materialViewPager);
        final Toolbar toolbar = materialViewPager.getToolbar();

        toolbar.inflateMenu(R.menu.myprofile_menu);
        toolbar.setTitleTextColor(Color.WHITE);

        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.edit_button:
                    startActivity(new Intent(getActivity(), EditProfileActivity.class));
                    return true;
                default:
                    return true;
            }
        });

        final RelativeLayout headerRoot = (RelativeLayout) materialViewPager.findViewById(R.id.headerRoot);
        final TextView userName = (TextView) headerRoot.findViewById(R.id.userName);
        final TextView musicCount = (TextView) headerRoot.findViewById(R.id.musicCount);
        final TextView userHandle = (TextView) headerRoot.findViewById(R.id.userHandle);
        final SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);

        SharedPreferences preferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);

        final String uName = SharedPrefUtils.getUserName(preferences);
        toolbar.setTitle(uName);
        userName.setText(uName);
        //musicCount.setText(SharedPrefUtils.get);
        userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
        profilePic.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + SharedPrefUtils.getImageId(preferences)));

        ViewPager viewPager = materialViewPager.getViewPager();
        viewPager.setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position) {

                    case 0:
                        return ApplicationFragment.getInstance("Apps");
                    case 1:
                        return MyLibraryFragment.getInstance("Songs");
                    default:
                        throw new IllegalStateException("Count and size clash");
                }
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {

                    case 0:
                        return "Apps";
                    case 1:
                        return "Songs";
                    default:
                        throw new IllegalStateException("Count and size clash");
                }
            }
        });

        materialViewPager.setMaterialViewPagerListener(page -> {
            switch (page) {
                case 0:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.reach_grey,
                            "");
                case 1:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.reach_color,
                            "");
            }
            return null;
        });

        viewPager.setOffscreenPageLimit(viewPager.getAdapter().getCount());
        viewPager.setPageMargin(-1 * (MiscUtils.dpToPx(50)));
        viewPager.setPageTransformer(true, (view, position) -> {

            if (position <= 1) {
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                float vertMargin = view.getHeight() * (1 - scaleFactor) / 2;
                float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
                if (position < 0)
                    view.setTranslationX(horzMargin - vertMargin / 2);
                else
                    view.setTranslationX(-horzMargin + vertMargin / 2);

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        });
        materialViewPager.getPagerTitleStrip().setViewPager(viewPager);

        return rootView;
    }
}