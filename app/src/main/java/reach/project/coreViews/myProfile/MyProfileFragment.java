package reach.project.coreViews.myProfile;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.ancillaryViews.TermsActivity;
import reach.project.coreViews.myProfile.apps.ApplicationFragment;
import reach.project.coreViews.myProfile.music.MyLibraryFragment;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.player.PlayerActivity;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class MyProfileFragment extends Fragment {

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);

    private final Toolbar.OnMenuItemClickListener menuItemClickListener = item -> {

        boolean check;
        switch (item.getItemId()) {
            /*case R.id.show_visible:
                check = !item.isChecked();
                //TODO show visible files
                item.setChecked(check);
                return true;
            case R.id.show_invisible:
                check = !item.isChecked();
                //TODO show invisible files
                item.setChecked(check);
                return true;*/
            case R.id.player_button:
                PlayerActivity.openActivity(getContext());
                return true;
            case R.id.settings_button:
                //startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
            case R.id.useMobileData:
                final SharedPreferences preferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
                if(item.isChecked()){
                    item.setChecked(false);
                    SharedPrefUtils.setDataOff(preferences);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getContext().getContentResolver().delete(
                                    SongProvider.CONTENT_URI,
                                    SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                            SongHelper.COLUMN_STATUS + " != ?",
                                    new String[]{"1", ReachDatabase.Status.PAUSED_BY_USER.getString()});
                        }
                    }).start();
                }
                else{
                    item.setChecked(true);
                    SharedPrefUtils.setDataOn(preferences);
                }
                return true;

            case R.id.conditions:{
                startActivity(new Intent(getContext(), TermsActivity.class));
                return true;
            }

            case R.id.rateOurApp:{

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=reach.project")));
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Play store app not installed", Toast.LENGTH_SHORT).show();
                }
                finally {
                    return true;
                }

            }
        }
        return false;
    };

    private static final ViewPager.PageTransformer PAGE_TRANSFORMER = (view, position) -> {

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
    };

    private static final MaterialViewPager.Listener MATERIAL_LISTENER = page -> {

        switch (page) {
            case 0:
                return HeaderDesign.fromColorResAndUrl(
                        R.color.reach_color,
                        "");
            case 1:
                return HeaderDesign.fromColorResAndUrl(
                        R.color.reach_color,
                        "");
            default:
                throw new IllegalStateException("Size of 2 expected");
        }
    };

    private static ViewPager viewPager = null;

    public static void setItem(int position) {

        if (viewPager != null)
            viewPager.setCurrentItem(position, true);
    }

//    public void onDestroyView() {
//        super.onDestroyView();
//        Log.d("Ashish", "MyProfileFragment - onDestroyView");
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.d("Ashish", "MyProfileFragment - onCreate");
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        Log.d("Ashish", "MyProfileFragment - onDestroy");
//    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.activity_your_profile, container, false);
        Log.d("Ashish", "MyProfileFragment - onCreateView");

        final MaterialViewPager materialViewPager = (MaterialViewPager) rootView.findViewById(R.id.materialViewPager);
        viewPager = materialViewPager.getViewPager();
        final Toolbar toolbar = materialViewPager.getToolbar();
        final Activity activity = getActivity();

        final SharedPreferences preferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);

        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle("My Profile");
        toolbar.inflateMenu(R.menu.myprofile_menu);
        final Menu menu = toolbar.getMenu();
        final MenuItem useMobileData =  menu.findItem(R.id.useMobileData);
        if(SharedPrefUtils.getMobileData(preferences)) {
            useMobileData.setChecked(true);
        }
        else{
            useMobileData.setChecked(false);
        }

        final MenuItem menuItem = toolbar.getMenu().findItem(R.id.edit_button);
        MenuItemCompat.setActionView(menuItem, R.layout.edit_profile_button);
        final View editProfileContainer = MenuItemCompat.getActionView(menuItem).findViewById(R.id.editProfileLayout);
        editProfileContainer.setOnClickListener(v -> startActivity(new Intent(activity, EditProfileActivity.class)));
        toolbar.setNavigationIcon(null);
        toolbar.setOnMenuItemClickListener(menuItemClickListener);

        final RelativeLayout headerRoot = (RelativeLayout) materialViewPager.findViewById(R.id.headerRoot);
        final String userName = SharedPrefUtils.getUserName(preferences);

        ((TextView) headerRoot.findViewById(R.id.userName)).setText(userName);
        ((TextView) headerRoot.findViewById(R.id.userHandle)).setText("@" + userName.toLowerCase().split(" ")[0]);

        final SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);
        profilePic.setImageURI(AlbumArtUri.getUserImageUri(
                SharedPrefUtils.getServerId(preferences),
                "imageId",
                "rw", //webP
                true, //circular
                PROFILE_PHOTO_RESIZE.width,
                PROFILE_PHOTO_RESIZE.height));

        final SimpleDraweeView coverPic = (SimpleDraweeView) headerRoot.findViewById(R.id.coverPic);
        coverPic.setImageURI(AlbumArtUri.getUserImageUri(
                SharedPrefUtils.getServerId(preferences),
                "coverPicId",
                "rw", //webP
                false, //simple crop
                COVER_PHOTO_RESIZE.width,
                COVER_PHOTO_RESIZE.height));

        final PagerAdapter pagerAdapter = new FragmentPagerAdapter(getChildFragmentManager()) {

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

            //
            @Override
            public int getCount() {
                return 0;
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
        };

        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(viewPager.getAdapter().getCount());
        viewPager.setPageMargin(-1 * (MiscUtils.dpToPx(20)));
        //viewPager.setPageTransformer(true, PAGE_TRANSFORMER);

        materialViewPager.setMaterialViewPagerListener(MATERIAL_LISTENER);
        materialViewPager.getPagerTitleStrip().setViewPager(viewPager);
        materialViewPager.getPagerTitleStrip().setOnTouchListener((v, event) -> true);

        new CountUpdater((TextView) headerRoot.findViewById(R.id.musicCount),
                (TextView) headerRoot.findViewById(R.id.appCount)).execute(activity);

        return rootView;
    }

    private final static class CountUpdater extends AsyncTask<Context, Void, Pair<String, String>> {

        private final WeakReference<TextView> musicCountReference, appCountReference;

        private CountUpdater(TextView musicCount, TextView appCount) {
            this.musicCountReference = new WeakReference<>(musicCount);
            this.appCountReference = new WeakReference<>(appCount);
        }

        @Override
        protected Pair<String, String> doInBackground(Context... params) {

            final ContentResolver contentResolver = params[0].getContentResolver();
            final PackageManager packageManager = params[0].getPackageManager();

            final Cursor cursor = contentResolver.query(MySongsProvider.CONTENT_URI,
                    new String[]{MySongsHelper.COLUMN_ID}, null, null, null);
            final int songCount;
            if (cursor != null) {
                songCount = cursor.getCount();
                cursor.close();
            } else
                songCount = 0;

            final int appCount = MiscUtils.getInstalledApps(packageManager).size();

            return new Pair<>(songCount + "", appCount + "");
        }

        @Override
        protected void onPostExecute(Pair<String, String> countPair) {
            super.onPostExecute(countPair);

            MiscUtils.useReference(musicCountReference, textView -> {
                textView.setText(countPair.first);
            });

            MiscUtils.useReference(appCountReference, textView -> {
                textView.setText(countPair.second);
            });
        }
    }
}