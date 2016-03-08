package reach.project.core;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.wire.Wire;

import org.joda.time.DateTime;
import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import reach.backend.entities.feedBackApi.model.FeedBack;
import reach.project.R;
import reach.project.ancillaryViews.SettingsActivity;
import reach.project.apps.App;
import reach.project.coreViews.explore.ExploreFragment;
import reach.project.coreViews.fileManager.apps.ApplicationFragment;
import reach.project.coreViews.fileManager.music.downloading.DownloadingFragment;
import reach.project.coreViews.fileManager.music.myLibrary.MyLibraryFragment;
import reach.project.coreViews.friends.FriendsFragment;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.myProfile.MyProfileFragment;
import reach.project.coreViews.push.PushActivity;
import reach.project.coreViews.push.PushContainer;
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.notificationCentre.NotificationActivity;
import reach.project.player.PlayerActivity;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.FireOnce;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.ancillaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.PagerFragment;

public class ReachActivity extends AppCompatActivity implements SuperInterface {

    private static final String TAG = ReachActivity.class.getSimpleName();
    private AlertDialog alertDialog;

    public static void openActivity(Context context) {

        final Intent intent = new Intent(context, ReachActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    public static Intent getIntent(Context context) {

        final Intent intent = new Intent(context, ReachActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    ////////////////////////////////////////public static final

    public static final String OPEN_MY_FRIENDS = "OPEN_MY_FRIENDS";
    public static final String OPEN_PUSH = "OPEN_PUSH";
    public static final String OPEN_EXPLORE = "OPEN_EXPLORE";
    public static final String OPEN_MANAGER_APPS = "OPEN_MANAGER_APPS";
    public static final String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    public static final String OPEN_MY_PROFILE_APPS_FIRST = "OPEN_MY_PROFILE_APPS_FIRST";
    public static final String OPEN_MY_PROFILE_SONGS = "OPEN_MY_PROFILE_SONGS";
    public static final String OPEN_MANAGER_SONGS_DOWNLOADING = "OPEN_MANAGER_SONGS_DOWNLOADING";
    public static final String OPEN_MANAGER_SONGS_LIBRARY = "OPEN_MANAGER_SONGS_LIBRARY";
    public static final String ADD_PUSH_SONG = "ADD_PUSH_SONG";
    private static final int DOWNLOADED_COUNT_LOADER_ID = 2222;
    private static final String DOWNLOADED_COUNT_SHARED_PREF_KEY = "downloaded_count";
    private static final String SHOW_RATING_DIALOG_SHARED_PREF_KEY = "show_rating_dialog";
    private static final String FIRST_TIME_DOWNLOADED_COUNT_SHARED_PREF_KEY = "first_time_downloaded_count";

    public static final Set<Song> SELECTED_SONGS = MiscUtils.getSet(5);
    public static final Set<App> SELECTED_APPS = MiscUtils.getSet(5);
    public static final LongSparseArray<Boolean> SELECTED_SONG_IDS = new LongSparseArray<>(5);
    SharedPreferences preferences;
    ////////////////////////////////////////private static final

    @SuppressWarnings("unchecked")
    private static final Bundle DOWNLOAD_PAGER_BUNDLE = PagerFragment.getBundle("My Files",
            new PagerFragment.Pages(
                    new Class[]{MyLibraryFragment.class, DownloadingFragment.class},
                    new String[]{"My Library", "Downloading"},
                    "Songs"),
            new PagerFragment.Pages(
                    new Class[]{ApplicationFragment.class},
                    new String[]{"My Applications"},
                    "Apps"));

    @SuppressWarnings("unchecked")
    private static final Bundle PUSH_PAGER_BUNDLE = PagerFragment.getBundle("Share",
//            new PagerFragment.Pages(
//                    new Class[]{reach.project.coreViews.push.apps.ApplicationFragment.class},
//                    new String[]{"My Applications"},
//                    "Apps"),
            new PagerFragment.Pages(
                    new Class[]{reach.project.coreViews.push.music.MyLibraryFragment.class},
                    new String[]{"My Library"},
                    "Songs"));

    ////////////////////////////////////////

    private final Toolbar.OnMenuItemClickListener menuClickListener = item -> {

        switch (item.getItemId()) {

            case R.id.push_button: {

                if (SELECTED_SONGS.isEmpty() && SELECTED_APPS.isEmpty()) {
                    Toast.makeText(this, "First select some songs", Toast.LENGTH_SHORT).show();
                    return false;
                }
                final PushContainer pushContainer = new PushContainer.Builder()
                        .senderId(SharedPrefUtils.getServerId(preferences))
                        .userName(SharedPrefUtils.getUserName(preferences))
                        .userImage(SharedPrefUtils.getImageId(preferences))
                        .firstSongName(SELECTED_SONGS.isEmpty() ? "" : SELECTED_SONGS.iterator().next().displayName)
                        .firstAppName(SELECTED_APPS.isEmpty() ? "" : SELECTED_APPS.iterator().next().applicationName)
                        .song(ImmutableList.copyOf(SELECTED_SONGS))
                        .app(ImmutableList.copyOf(SELECTED_APPS))
                        .songCount(SELECTED_SONGS.size())
                        .appCount(SELECTED_APPS.size())
                        .build();

                try {
                    PushActivity.startPushActivity(pushContainer, this);
                } catch (IOException e) {

                    e.printStackTrace();
                    //TODO Track
                    Toast.makeText(this, "Could not push", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            case R.id.player_button:
                final Intent playerIntent = new Intent(this, PlayerActivity.class);
                playerIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(playerIntent);
                return true;
            case R.id.notif_button:
                NotificationActivity.openActivity(this, NotificationActivity.OPEN_NOTIFICATIONS);
                return true;
            case R.id.my_profile_button:
                final Intent myProfileIntent = new Intent(ReachActivity.this, MyProfileActivity.class);
                //settingsIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(myProfileIntent);
                return true;

        }

        return false;
    };

    @Nullable
    private static WeakReference<ReachActivity> reference = null;

    private FragmentTabHost mTabHost;

    private static long serverId = 0;

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (reference != null)
            reference.clear();
        reference = null;
        mTabHost = null;
        FireOnce.INSTANCE.close(); //app is getting killed
        //viewPager = null;
    }

    @Override
    protected void onPostResume() {

        super.onPostResume();
        Log.i("Ayush", "Called onPostResume");
        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {

        Log.d("Ayush", "Received new Intent");
        processIntent(intent);
        super.onNewIntent(intent);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reach);

        reference = new WeakReference<>(this);

        preferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);
        serverId = SharedPrefUtils.getServerId(preferences);

        //track app open event
        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, serverId + "");
        simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(this));
        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        simpleParams.put(PostParams.SCREEN_NAME, "my_reach");
        try {
            simpleParams.put(PostParams.APP_VERSION,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + "");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        try {
            UsageTracker.trackEvent(simpleParams, UsageTracker.APP_OPEN);
        } catch (JSONException ignored) {
        }

//        Crittercism.setUsername(SharedPrefUtils.getUserName(preferences) + " - " +
//                SharedPrefUtils.getPhoneNumber(preferences));

        ////////////////////////////////////////

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);

        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View exploreTabView = LayoutInflater.from(ReachActivity.this).inflate(R.layout.tab_view,null);
        TextView text=(TextView)exploreTabView.findViewById(R.id.tab_text);
        ImageView image = (ImageView)exploreTabView.findViewById(R.id.tab_image);
        text.setText("Explore");
        image.setImageResource(R.drawable.explore_tab_selector);

        mTabHost.addTab(mTabHost.newTabSpec("explore_page").setIndicator(setUpTabView(inflater,
                "Explore",
                R.drawable.explore_tab_selector)), ExploreFragment.class,null);

        /*text.setText("Friends");
        image.setImageResource(R.drawable.friends_tab_selector);*/

        mTabHost.addTab(mTabHost.newTabSpec("manager_page").setIndicator(setUpTabView(
                inflater,
                "My Files",
                R.drawable.manager_tab_selector
                ))
                ,PagerFragment.class,DOWNLOAD_PAGER_BUNDLE);

        mTabHost.addTab(mTabHost.newTabSpec("friends_page").setIndicator(setUpTabView(
                inflater,
                "Friends",
                R.drawable.friends_tab_selector
        ))
                ,FriendsFragment.class,null);



        /*mTabHost.addTab(
                mTabHost.newTabSpec("friends_page").setIndicator("Friends",
                        ContextCompat.getDrawable(this, R.drawable.friends_tab_selector)),
                FriendsFragment.class, null);*/
        /*mTabHost.addTab(
                mTabHost.newTabSpec("push_page").setIndicator("",
                        ContextCompat.getDrawable(this, R.drawable.push_tab_selector)),
                PagerFragment.class, PUSH_PAGER_BUNDLE);*/
        /*mTabHost.addTab(
                mTabHost.newTabSpec("manager_page").setIndicator("File Manager",
>>>>>>> 5417a1e6a1c83c1ed8f89032f70783f78d658682
                        ContextCompat.getDrawable(this, R.drawable.manager_tab_selector)),
                PagerFragment.class, DOWNLOAD_PAGER_BUNDLE);*/
        /*mTabHost.addTab(
                mTabHost.newTabSpec("myprofile_page").setIndicator("",
                        ContextCompat.getDrawable(this, R.drawable.my_profile_tab_selector)),
<<<<<<< HEAD
                MyProfileFragment.class, null);
=======
                MyProfileFragment.class, null);*/
        mTabHost.setCurrentTab(0);

        if (showRatingDialogOrNot()) {
            Log.d(TAG, "Created Downloaded Loader");
            getSupportLoaderManager().initLoader(DOWNLOADED_COUNT_LOADER_ID,
                    null,
                    new android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>() {
                        @Override
                        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                            return new CursorLoader(ReachActivity.this,
                                    SongProvider.CONTENT_URI,
                                    SongHelper.MUSIC_DATA_LIST,
                                    SongHelper.COLUMN_STATUS + " = ? and " + //show only finished
                                            SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                                    new String[]{ReachDatabase.Status.FINISHED.getString(),
                                            ReachDatabase.OperationKind.DOWNLOAD_OP.getString()},
                                    SongHelper.COLUMN_DATE_ADDED + " DESC");
                        }

                        @Override
                        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

                            if (data == null) {
                                Log.d(TAG, "Inside OnLoadFinished, cursor == null");
                                return;
                            }

                            if(getValueFirstTimeDownloadedCountCalled()){
                                putDownloadedCountInSharedPref(data.getCount());
                                putValueFirstTimeDownloadedCountCalled(false);
                                return;
                            }

                            if (!showRatingDialogOrNot()) {
                                return;
                            }

                            if (getDownloadedCountFromSharedPref() < data.getCount()) {
                                Log.d(TAG, "Show Rating Dialog Called");
                                putDownloadedCountInSharedPref(data.getCount());
                                showRatingDialog();


                            }
                            Log.d(TAG, "Inside OnLoadFinished, cursor count = " + data.getCount());


                        }

                        @Override
                        public void onLoaderReset(Loader<Cursor> loader) {

                        }
                    });

        }

        //check for update, need activity to check
        FireOnce.checkUpdate(reference);
    }


    private View setUpTabView( final LayoutInflater inflater, final String tab_text, final int tab_drawable_res){
        View tabView = inflater.inflate(R.layout.tab_view,null);
        TextView text= (TextView) tabView.findViewById(R.id.tab_text);
        ImageView image = (ImageView) tabView.findViewById(R.id.tab_image);
        text.setText(tab_text);
        image.setImageResource(tab_drawable_res);

        return tabView;

    }

    private void putRatingValueInSharedPref(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_RATING_DIALOG_SHARED_PREF_KEY, value);
        editor.commit();
    }

    private boolean showRatingDialogOrNot() {
        return preferences.getBoolean(SHOW_RATING_DIALOG_SHARED_PREF_KEY, true);
    }

    private void putValueFirstTimeDownloadedCountCalled(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(FIRST_TIME_DOWNLOADED_COUNT_SHARED_PREF_KEY, value);
        editor.commit();
    }

    private boolean getValueFirstTimeDownloadedCountCalled() {
        return preferences.getBoolean(FIRST_TIME_DOWNLOADED_COUNT_SHARED_PREF_KEY, true);
    }



    private int getDownloadedCountFromSharedPref() {
        return preferences.getInt(DOWNLOADED_COUNT_SHARED_PREF_KEY, 0);
    }

    private void putDownloadedCountInSharedPref(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DOWNLOADED_COUNT_SHARED_PREF_KEY, value);
        editor.commit();
    }

    private void showRatingDialog() {
        if(alertDialog!=null){
            if(alertDialog.isShowing()){
                return;
            }
        }
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View vv = inflater.inflate(R.layout.rating_dialog_custom_view, null, false);
        //final View vv2 =inflater.inflate(R.layout.feedback_custom_layout,container,false);
        final RatingBar rating = (RatingBar) vv.findViewById(R.id.ratingBar);
        rating.getProgressDrawable().setColorFilter(Color.parseColor("#FFD700"), PorterDuff.Mode.SRC_ATOP);
        final View feedbackContainer = vv.findViewById(R.id.feedbackContainer);
        final View ratingContainer = vv.findViewById(R.id.ratingContainer);
        final EditText feedbackEditText = (EditText) vv.findViewById(R.id.feedback_edt);
        final View submitBtn = vv.findViewById(R.id.submit_btn);
        final View cancelBtn = vv.findViewById(R.id.cancel_btn);


        //vv.findViewById(R.id.feedbackEditText);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String feebackText = feedbackEditText.getText().toString();
                if ((feebackText.trim()).length() == 0) {
                    Toast.makeText(ReachActivity.this, "Please enter a feedback", Toast.LENGTH_LONG).show();
                    return;
                }
                final long serverId = SharedPrefUtils.getServerId(preferences);
                final FeedBack feedback = new FeedBack();
                feedback.setClientId(serverId);
                feedback.setReply3(feebackText);

                MiscUtils.autoRetryAsync(() -> StaticData.FEED_BACK_API.insert(feedback).execute(),
                        Optional.absent());
                //TODO: Only change the value in shared pref if the feedback gets submitted
                if(alertDialog!=null) {
                    alertDialog.dismiss();
                }
                putRatingValueInSharedPref(false);

            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog != null) {
                    alertDialog.dismiss();
                }
                putRatingValueInSharedPref(false);
            }
        });

        vv.findViewById(R.id.rateNow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Uri uri = Uri.parse("market://details?id=" + /*getActivity().getPackageName()*/"reach.project");
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + /*getActivity().getPackageName()*/ "reach.project")));
                } finally {
                    putRatingValueInSharedPref(false);
                    if (alertDialog != null) {
                        alertDialog.dismiss();
                    }
                }


            }
        });
        vv.findViewById(R.id.later).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                            /*alertDialogBuilder.setView(vv2);
                            alertDialogBuilder.show();*/
                ratingContainer.setVisibility(View.GONE);
                feedbackContainer.setVisibility(View.VISIBLE);


            }
        });


        alertDialogBuilder.setView(vv);
        alertDialog = alertDialogBuilder.show();


    }


    private synchronized void processIntent(Intent intent) {

        if (intent == null)
            return;

        Log.i("Ayush", "Processing Intent + " + intent.getAction());

        final String action = intent.getAction();
        if (TextUtils.isEmpty(action))
            return;
        try {
            switch (action) {
                case ADD_PUSH_SONG:
                    Log.i("Ayush", "FOUND PUSH DATA");

                    final String compressed = intent.getStringExtra("data");
                    if (compressed == null)
                        return;

                    byte[] unCompressed;
                    try {
                        unCompressed = StringCompress.deCompressStringToBytes(compressed);
                    } catch (IOException e) {
                        e.printStackTrace();
                        unCompressed = null;
                    }

                    if (unCompressed != null && unCompressed.length > 0) {

                        PushContainer pushContainer;
                        try {
                            pushContainer = new Wire(PushContainer.class).parseFrom(unCompressed, PushContainer.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                            pushContainer = null;
                        }

                        if (pushContainer != null && pushContainer.song != null && !pushContainer.song.isEmpty()) {

                            for (Song song : pushContainer.song) {

                                if (song == null)
                                    continue;

                                addSongToQueue(song.songId,
                                        pushContainer.senderId,
                                        song.size,
                                        song.displayName,
                                        song.actualName,
                                        true,
                                        pushContainer.userName,
                                        ReachFriendsHelper.Status.ONLINE_REQUEST_GRANTED,
                                        "0",
                                        song.artist,
                                        song.duration,
                                        song.album,
                                        song.genre);
                            }
                            FireOnce.refreshOperations(reference);
                            if (mTabHost != null) {
                                mTabHost.postDelayed(() -> {
                                    if (mTabHost == null || isFinishing())
                                        return;
                                    mTabHost.setCurrentTab(3);
                                    mTabHost.postDelayed(() -> {
                                        final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                                .findFragmentByTag("manager_page");
                                        if (fragment == null)
                                            return;
                                        fragment.setItem(0);
                                        fragment.setInnerItem(0, 1);
                                    }, 500L);
                                }, 1000L);
                            }
                        }
                    }
                    break;
                case OPEN_MANAGER_APPS:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(3);
                            mTabHost.postDelayed(() -> {
                                final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                        .findFragmentByTag("manager_page");
                                fragment.setItem(1);
                            }, 500L);
                        }, 1000L);
                    }

                    break;
                case OPEN_MANAGER_SONGS_DOWNLOADING:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(3);
                            mTabHost.postDelayed(() -> {
                                final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                        .findFragmentByTag("manager_page");
                                if (fragment == null)
                                    return;
                                fragment.setItem(0);
                                fragment.setInnerItem(0, 1);
                            }, 500L);
                        }, 1000L);
                    }

                    break;
                case OPEN_MANAGER_SONGS_LIBRARY:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(3);
                            mTabHost.postDelayed(() -> {
                                final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                        .findFragmentByTag("manager_page");
                                fragment.setItem(0);
                                fragment.setInnerItem(0, 0);
                            }, 500L);
                        }, 1000L);
                    }
                    break;
                case OPEN_MY_PROFILE_APPS:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(4);
                            MyProfileFragment.setItem(0);
                        }, 1000L);
                    }
                    break;
                case OPEN_MY_PROFILE_APPS_FIRST:
                    if (mTabHost != null && !isFinishing()) {
                        mTabHost.setCurrentTab(4);
                        MyProfileFragment.setItem(0);
                    }
                    break;
                case OPEN_MY_PROFILE_SONGS:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(4);
                            MyProfileFragment.setItem(1);
                        }, 1000L);
                    }
                    break;
                case OPEN_EXPLORE:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(2);
                        }, 1000L);
                    }
                    break;
                case OPEN_MY_FRIENDS:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(0);
                        }, 1000L);
                    }
                    break;
                case OPEN_PUSH:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(1);
                        }, 1000L);
                    }
                    break;
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public Toolbar.OnMenuItemClickListener getMenuClickListener() {
        return menuClickListener;
    }

    @Override
    public void addSongToQueue(long songId, long senderId, long size,
                               String displayName, String actualName,
                               boolean multiple, String userName, ReachFriendsHelper.Status onlineStatus,
                               String networkType, String artistName, long duration,
                               String albumName, String genre) {

        final ContentResolver contentResolver = getContentResolver();
        if (contentResolver == null)
            return;

        /**
         * DISPLAY_NAME, ACTUAL_NAME, SIZE & DURATION all can not be same, effectively its a hash
         */

        final Cursor cursor;
        if (multiple)
            cursor = contentResolver.query(
                    SongProvider.CONTENT_URI,
                    new String[]{SongHelper.COLUMN_ID},
                    SongHelper.COLUMN_DISPLAY_NAME + " = ? and " +
                            SongHelper.COLUMN_ACTUAL_NAME + " = ? and " +
                            SongHelper.COLUMN_SIZE + " = ? and " +
                            SongHelper.COLUMN_DURATION + " = ?",
                    new String[]{displayName, actualName, size + "", duration + ""},
                    null);
        else
            //this cursor can be used to play if entry exists
            cursor = contentResolver.query(
                    SongProvider.CONTENT_URI,
                    new String[]{

                            SongHelper.COLUMN_ID, //0
                            SongHelper.COLUMN_PROCESSED, //1
                            SongHelper.COLUMN_PATH, //2

                            SongHelper.COLUMN_IS_LIKED, //3
                            SongHelper.COLUMN_SENDER_ID, //4
                            SongHelper.COLUMN_RECEIVER_ID, //5
                            SongHelper.COLUMN_SIZE, //6
                            SongHelper.COLUMN_META_HASH //7

                    },

                    SongHelper.COLUMN_DISPLAY_NAME + " = ? and " +
                            SongHelper.COLUMN_ACTUAL_NAME + " = ? and " +
                            SongHelper.COLUMN_SIZE + " = ? and " +
                            SongHelper.COLUMN_DURATION + " = ?",
                    new String[]{displayName, actualName, size + "", duration + ""},
                    null);

        if (cursor != null) {

            if (cursor.moveToFirst()) {

                //song already found
                if (!multiple) {

                    //if not multiple addition, play the song
                    final boolean liked;
                    final String temp = cursor.getString(3);
                    liked = !TextUtils.isEmpty(temp) && temp.equals("1");

                    final MusicData musicData = new MusicData(
                            cursor.getLong(0), //id
                            cursor.getString(7), //meta-hash
                            size,
                            senderId,
                            cursor.getLong(1),
                            0,
                            cursor.getString(2),
                            displayName,
                            artistName,
                            "",
                            liked,
                            duration,
                            MusicData.Type.DOWNLOADED);
                    MiscUtils.playSong(musicData, this);
                }
                //in both cases close and continue
                cursor.close();
                return;
            }
            cursor.close();
        }

        //new song

        final ReachDatabase reachDatabase = new ReachDatabase.Builder()
                .setId(-1)
                .setSongId(songId)
                .setReceiverId(serverId)
                .setSenderId(senderId)
                .setOperationKind(ReachDatabase.OperationKind.DOWNLOAD_OP)
                .setUserName(userName)
                .setArtistName(artistName)
                .setDisplayName(displayName)
                .setActualName(actualName)
                .setLength(size)
                .setDateAdded(DateTime.now())
                .setUniqueId(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                .setDuration(duration)
                .setAlbumName(albumName)
                .setAlbumArtData(new byte[0])
                .setGenre(genre)
                .setLiked(false)
                .setOnlineStatus(onlineStatus)
                .setVisibility(true)
                .setPath("hello_world")
                .setProcessed(0)
                .setLogicalClock((short) 0)
                .setStatus(ReachDatabase.Status.NOT_WORKING).build();

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        //We call bulk starter always
        MiscUtils.startDownload(reachDatabase, this, null, "PUSH");
    }

    @Override
    public void showSwipeCoach() {
        new Handler().postDelayed(() -> {
            final ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
            final View coachView = LayoutInflater.from(this).inflate(R.layout.swipe_coach, viewGroup, false);
            viewGroup.addView(coachView);
            coachView.setOnClickListener(v -> viewGroup.removeView(coachView));
        }, 1000L);
    }


}