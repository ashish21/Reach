package reach.project.coreViews.yourProfile;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import reach.project.R;
import reach.project.ancillaryViews.SettingsActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.yourProfile.apps.YourProfileAppFragment;
import reach.project.coreViews.yourProfile.music.YourProfileMusicFragment;
import reach.project.notificationCentre.NotificationActivity;
import reach.project.player.PlayerActivity;
import reach.project.utils.MiscUtils;


// If a friend is added, then this activity is displayed
public class YourProfileActivity extends AppCompatActivity {

    private static final String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    private static final String OPEN_MY_PROFILE_SONGS = "OPEN_MY_PROFILE_SONGS";

    public static void openProfile(long userId, Context context) {

        final Intent intent = new Intent(context, YourProfileActivity.class);
        //intent.setAction(OPEN_PROFILE);
        intent.putExtra("userId", userId);

        context.startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        MiscUtils.navigateUp(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_profile);

        final MaterialViewPager materialViewPager = (MaterialViewPager) findViewById(R.id.materialViewPager);
        final Toolbar toolbar = materialViewPager.getToolbar();
        final ViewPager viewPager = materialViewPager.getViewPager();

        toolbar.setTitle("Profile");
        toolbar.setTitleTextColor(Color.WHITE);

        toolbar.inflateMenu(R.menu.yourprofile_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.player_button:
                    PlayerActivity.openActivity(this);
                    return true;
                case R.id.notif_button:
                    NotificationActivity.openActivity(this, NotificationActivity.OPEN_NOTIFICATIONS);
                    return true;
                case R.id.settings_button:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                default:
                    return false;
            }
        });
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        final Intent intent = getIntent();
        final long userId = intent.getLongExtra("userId", 0L);
        if (userId == 0) {
            finish();
            return;
        }

        final Cursor cursor = getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_PHONE_NUMBER,
                        ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS,
                        ReachFriendsHelper.COLUMN_IMAGE_ID,
                        ReachFriendsHelper.COLUMN_NETWORK_TYPE,
                        ReachFriendsHelper.COLUMN_STATUS,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_APPS,
                        ReachFriendsHelper.COLUMN_COVER_PIC_ID},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        int numberOfSongs = 0;

        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {

            final String uName = cursor.getString(1);
            numberOfSongs = cursor.getInt(2);
            final int numberOfApps = cursor.getInt(6);
            final String imageId = cursor.getString(3);
            final short status = cursor.getShort(5);
            if (status == ReachFriendsHelper.Status.OFFLINE_REQUEST_GRANTED.getValue())
                Snackbar.make(findViewById(R.id.root_layout), uName + " is currently offline. You will be able to transfer music when the user comes online.", Snackbar.LENGTH_INDEFINITE).show();
            if (userId != StaticData.DEVIKA) {
                final View rootView = findViewById(R.id.root_layout);
                if (status == ReachFriendsHelper.UPLOADS_DISABLED)
                    Snackbar.make(rootView, uName + " has disabled uploads. You will be only be able to transfer music when the user enables it", Snackbar.LENGTH_INDEFINITE).show();
                else if (status == ReachFriendsHelper.OFFLINE_REQUEST_GRANTED)
                    Snackbar.make(rootView, uName + " is currently offline. You will be able to transfer music when the user comes online.", Snackbar.LENGTH_INDEFINITE).show();
            }

            final RelativeLayout headerRoot = (RelativeLayout) materialViewPager.findViewById(R.id.headerRoot);
            final TextView userName = (TextView) headerRoot.findViewById(R.id.userName);
            final TextView musicCount = (TextView) headerRoot.findViewById(R.id.musicCount);
            final TextView appCount = (TextView) headerRoot.findViewById(R.id.appCount);
            final TextView userHandle = (TextView) headerRoot.findViewById(R.id.userHandle);
            final SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);
            final SimpleDraweeView coverPic = (SimpleDraweeView) headerRoot.findViewById(R.id.coverPic);

            userName.setText(uName);
            musicCount.setText(numberOfSongs + "");
            appCount.setText(numberOfApps + "");
            userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
            profilePic.setController(MiscUtils.getControllerResize(profilePic.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + imageId), 100, 100));
            coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + cursor.getString(7)), 500, 300));
//            coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
//                    Uri.parse(MiscUtils.getRandomPic()), 500, 500));

            cursor.close();
        }

        final int finalNumberOfSongs = numberOfSongs;
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position) {

                    case 0:
                        return YourProfileAppFragment.newInstance(userId);
                    case 1:
                        return YourProfileMusicFragment.newInstance(userId);
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
                            R.color.reach_color,
                            "");
                case 1:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.reach_color,
                            "");
            }
            return null;
        });

        viewPager.setOffscreenPageLimit(viewPager.getAdapter().getCount());
        viewPager.setPageMargin(-1 * (MiscUtils.dpToPx(20)));
        /*viewPager.setPageTransformer(true, (view, position) -> {

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
        });*/
        materialViewPager.getPagerTitleStrip().setViewPager(viewPager);

        final String action = intent.getAction();
        if (TextUtils.isEmpty(action))
            return;
        if (action.equals(OPEN_MY_PROFILE_APPS))
            viewPager.setCurrentItem(0);
        else if (action.equals(OPEN_MY_PROFILE_SONGS))
            viewPager.setCurrentItem(1);

    }
}