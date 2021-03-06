package reach.project.coreViews.explore;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import reach.backend.entities.messaging.model.MyString;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.saved_songs.SaveSongInDatabaseTask;
import reach.project.coreViews.saved_songs.SavedSongsDataModel;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.YouTubeDataModel;
import reach.project.utils.ancillaryClasses.SuperInterface;
import reach.project.utils.ancillaryClasses.UseActivityWithResult;
import reach.project.utils.ancillaryClasses.UseContext;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ViewPagerCustomDuration;

import static reach.project.coreViews.explore.ExploreJSON.MiscMetaInfo;

public class ExploreFragment extends Fragment implements ExploreRecyclerViewAdapter.Explore,
        ExploreBuffer.ExplorationCallbacks<JsonObject>, HandOverMessage<Object>, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ExploreFragment.class.getSimpleName();
    @Nullable
    private static WeakReference<ExploreFragment> reference = null;
    private static long myServerId = 0;
    private SharedPreferences preferences;
    private View noFriendsDiscoverLayoutContainer;
    private final ExecutorService requestSender = MiscUtils.getRejectionExecutor();
    private AlertDialog fbShareDialog;
    private ExploreRecyclerViewAdapter exploreRecyclerViewAdapter;
    private int lastItemPosition;
    private View mPlayAllContainer;

    public ExploreFragment() {
        reference = new WeakReference<>(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: static reference is null now");
        reference = null;
        super.onPause();

    }


    @Override
    public void onResume() {
        Log.d(TAG, "onResume: ");
        if (reference == null) {
            Log.d(TAG, "onResume: Initializing static reference");
            reference = new WeakReference<ExploreFragment>(this);
        }
        super.onResume();

    }

    /**
     * @param data the byte[] to transform from
     * @return collection of explore stories, take care to remove loading / done for today etc...
     */
    public static final Function<byte[], Collection<JsonObject>> BYTES_TO_JSON = new Function<byte[], Collection<JsonObject>>() {
        @javax.annotation.Nullable
        @Override
        public Collection<JsonObject> apply(@Nullable byte[] input) {

            if (input == null || input.length == 0)
                return Collections.emptyList();

            final String jsonString;
            try {
                jsonString = new String(input, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }

            final JsonArray jsonElements = new JsonParser().parse(jsonString).getAsJsonArray();
            final Iterator<JsonElement> elementIterator = jsonElements.iterator();
            final List<JsonObject> toReturn = new ArrayList<>(jsonElements.size());

            for (JsonElement jsonElement : jsonElements) {
                final JsonObject exploreJSON = jsonElement.getAsJsonObject();
                final ExploreTypes exploreTypes = ExploreTypes.valueOf(MiscUtils.get(exploreJSON, ExploreJSON.TYPE).getAsString());
                if(exploreTypes== null)
                    continue;
                switch (exploreTypes){
                    case APP:
                        continue;
                    default:
                        toReturn.add(exploreJSON);
                }
            }

            return toReturn;
        }
    };

    /**
     * @param data the collection to transform into byte[]
     * @return byte[] explore stories, take care to remove loading / done for today etc...
     */
    public static final Function<List<JsonObject>, byte[]> JSON_TO_BYTES = new Function<List<JsonObject>, byte[]>() {
        @javax.annotation.Nullable
        @Override
        public byte[] apply(@Nullable List<JsonObject> input) {

            if (input == null || input.isEmpty())
                return new byte[0];

            final JsonArray array = new JsonArray();
            for (JsonObject jsonObject : input)
                array.add(jsonObject);

            final String easyRepresentation = array.toString();
            try {
                return easyRepresentation.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return new byte[0];
            }
        }
    };

    private static final CacheLoader<Long, Pair<String, String>> PAIR_CACHE_LOADER = new CacheLoader<Long, Pair<String, String>>() {
        @Override
        public Pair<String, String> load(@NonNull Long key) {

            return MiscUtils.useContextFromFragment(reference, context -> {

                final Cursor cursor = context.getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + key),
                        new String[]{
                                ReachFriendsHelper.COLUMN_USER_NAME,
                                ReachFriendsHelper.COLUMN_IMAGE_ID
                        },
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{key + ""}, null);

                final Pair<String, String> toReturn;

                if (cursor == null)
                    toReturn = new Pair<>("", "");
                else if (!cursor.moveToFirst()) {
                    cursor.close();
                    toReturn = new Pair<>("", "");
                } else
                    toReturn = new Pair<>(
                            cursor.getString(0),
                            cursor.getString(1));

                return toReturn;
            }).or(new Pair<>("", ""));
        }
    };
    static final LoadingCache<Long, Pair<String, String>> USER_INFO_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .build(PAIR_CACHE_LOADER);

    static final ResizeOptions FULL_IMAGE_SIZE = new ResizeOptions(450, 450);
    static final ResizeOptions SMALL_IMAGE_SIZE = new ResizeOptions(200, 200);

    /*
        Fetch new batch of Explore items
     */
    private static final Callable<Collection<JsonObject>> FETCH_NEXT_BATCH = () -> {

        final boolean onlineStatus = MiscUtils.useContextFromFragment(reference, (UseActivityWithResult<Activity, Boolean>) MiscUtils::isOnline).or(false);

        if (!onlineStatus) {

            MiscUtils.runOnUiThreadFragment(reference, (UseContext) context -> {
                Toast.makeText(context, "Could not connect to internet", Toast.LENGTH_SHORT).show();
            });
            return Collections.emptyList();
        }

        final JsonObject jsonObject = MiscUtils.useContextFromFragment(reference, context -> {

            //retrieve list of online friends
            final Cursor cursor = context.getContentResolver().query(
                    ReachFriendsProvider.CONTENT_URI,
                    new String[]{
                            ReachFriendsHelper.COLUMN_ID,
                            ReachFriendsHelper.COLUMN_USER_NAME,
                            ReachFriendsHelper.COLUMN_IMAGE_ID,
                            ReachFriendsHelper.COLUMN_STATUS
                    },
                    null, null, null);

            if (cursor == null)
                return null;

            final JsonArray jsonArray = new JsonArray();

            while (cursor.moveToNext()) {
                if (cursor.getShort(3) > 1)
                    continue;
                final long onlineId = cursor.getLong(0);
                final String userName = cursor.getString(1);
                final String imageId = cursor.getString(2);

                Log.i("Ayush", "Adding online friend id " + onlineId);
                jsonArray.add(onlineId);

                //cache the details
                USER_INFO_CACHE.put(onlineId, new Pair<>(userName, imageId));
            }
            cursor.close();

            final JsonObject toReturn = new JsonObject();
            toReturn.addProperty("userId", myServerId);
            /*MiscUtils.useFragment(reference, fragment -> {
                final long lastRequestTime = SharedPrefUtils.getLastRequestTime(fragment.preferences);
                    toReturn.addProperty("lastRequestTime", lastRequestTime);
            });*/
            Log.d("Ashish", "jsonArray size = " + jsonArray.size());
            toReturn.add("friends", jsonArray);

            return toReturn;

        }).or(new JsonObject());

        Log.i("Ayush", jsonObject.toString());

        final RequestBody body = RequestBody
                .create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
        final Request request = new Request.Builder()
                .url("http://52.74.175.56:8080/explore/getObjects")
                .post(body)
                .build();
        final Response response = ReachApplication.OK_HTTP_CLIENT.newCall(request).execute();
        if (response.code() != HttpStatusCodes.STATUS_CODE_OK)
            return Collections.emptyList();

        final JsonArray receivedData = new JsonParser().parse(response.body().string()).getAsJsonArray();
        if (receivedData.size() == 0) {

            ExploreBuffer.DONE_FOR_TODAY.set(true);
            return Collections.emptyList();
        }

        final ImagePipeline imagePipeline = Fresco.getImagePipeline();

        final List<JsonObject> containers = new ArrayList<>();
        for (int index = 0; index < receivedData.size(); index++) {

            final JsonObject object = receivedData.get(index).getAsJsonObject();
            Log.d("Ayush", object.toString());

            final ExploreTypes exploreTypes = ExploreTypes.valueOf(MiscUtils.get(object, ExploreJSON.TYPE).getAsString());
            final JsonObject viewInfo = MiscUtils.get(object, ExploreJSON.VIEW_INFO).getAsJsonObject();
            final ImageRequest imageRequest;
            final String image;

            switch (exploreTypes) {

                case MUSIC:

                    final JsonElement youtubeId = MiscUtils.get(object, ExploreJSON.YOUTUBE_ID);
                    if (youtubeId == null)
                        continue;
                    image = "https://i.ytimg.com/vi/" + youtubeId.getAsString() + "/hqdefault.jpg";
                    imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(image))
                            //.setResizeOptions(FULL_IMAGE_SIZE)
                            .build();
                    //TODO: Add song to the playList
                    //ReachActivity.explorePlayList.add(youtubeId.getAsString());



                    break;
                case APP:

                    image = MiscUtils.get(viewInfo, ExploreJSON.AppViewInfo.SMALL_IMAGE_URL, "").getAsString();
                    imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(image))
                            .setResizeOptions(SMALL_IMAGE_SIZE)
                            .build();
                    break;
                case MISC:

                    final String typeText = MiscUtils.get(viewInfo, ExploreJSON.MiscViewInfo.TYPE_TEXT, "").getAsString();
                    //TODO remove later
                    if (!typeText.equalsIgnoreCase("Small"))
                        continue;
                    image = MiscUtils.get(viewInfo, ExploreJSON.MiscViewInfo.LARGE_IMAGE_URL, "").getAsString();
                    imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(image))
                            .setResizeOptions(SMALL_IMAGE_SIZE)
                            .build();
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected type in explore" + exploreTypes.getName());
            }

            imagePipeline.prefetchToDiskCache(imageRequest, null);
            containers.add(object);
        }

        if (containers.size() > 0)
            MiscUtils.useFragment(reference, fragment -> {
                SharedPrefUtils.storeLastRequestTime(fragment.preferences);
            });

        Log.i("Ayush", "Explore has " + containers.size() + " stories");
        return containers;
    };

    /*private static final PopupMenu.OnMenuItemClickListener POP_MENU_CLICK = item -> {

        switch (item.getItemId()) {

            case R.id.explore_menu_1:
                if (item.isChecked())
                    item.setChecked(false);
                else
                    item.setChecked(true);
                return true;
            case R.id.explore_menu_2:
                if (item.isChecked())
                    item.setChecked(false);
                else
                    item.setChecked(true);
                return true;
            default:
                return false;
        }
    };*/

    @Nullable
    private View rootView = null;
    /*@Nullable
    private ViewPagerCustomDuration explorePager = null;*/

    @Nullable
    private RecyclerView exploreRecyclerView = null;

    /*@Nullable
    private ExploreAdapter exploreAdapter = null;*/

    @Nullable
    private ExploreBuffer<JsonObject> buffer = null;
    @Nullable
    private SuperInterface mListener = null;

    private YouTubePlayer player = null;

    private TextView playerText = null;
    private List<SearchResult> searchResults = new ArrayList<>(20);


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        preferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        myServerId = SharedPrefUtils.getServerId(preferences);
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_explore, container, false);
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.exploreToolbar);
        toolbar.setTitle("Discover");
        toolbar.inflateMenu(R.menu.explore_menu);
        toolbar.setOnMenuItemClickListener(mListener != null ? mListener.getMenuClickListener() : null);

        exploreRecyclerView = (RecyclerView) rootView.findViewById(R.id.exploreRecyclerView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false);
        exploreRecyclerView.setLayoutManager(layoutManager);
        //explorePager = (ViewPagerCustomDuration) rootView.findViewById(R.id.explorer);
        // For changing the speed of page transition
        //explorePager.setScrollDurationFactor(0.4);
        //explorePager.setClipToPadding(false);
        final int size = MiscUtils.dpToPx(16);
        mPlayAllContainer = rootView.findViewById(R.id.playButtonContainer);

        exploreRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(!(dy>-1 && dy<1)){

                    final int firstItemPosition = layoutManager.findFirstVisibleItemPosition();
                    if(firstItemPosition == -1){
                        return;
                    }

                    if(lastItemPosition == firstItemPosition) {
                        return;
                    }

                    lastItemPosition = firstItemPosition;
                    ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("Explore - Page Swiped")
                            .setAction("User Name - " + SharedPrefUtils.getUserName(preferences))
                            .setValue(1)
                            .build());
                    Log.d(TAG, "onScrolled: dy = " + dy + " firstVisibleItemPosition = " + firstItemPosition);

                    if(firstItemPosition == 10){

                        if (SharedPrefUtils.getShowFacebookShareOrNot(preferences)) {

                            //Toast.makeText(getActivity(), "11th item visible", Toast.LENGTH_SHORT).show();
                            if (preferences == null) {
                                preferences = SharedPrefUtils.getPreferences(getContext());
                            }

                            showFbDialog();
                            SharedPrefUtils.storeFacebookShareButtonVisibleOrNot(preferences, false);
                        }

                    }
                }
            }
        });

        //explorePager.setPadding(size, 0, size, (size * -1));
        //explorePager.setPageTransformer(false, new FlipHorizontalTransformer());
        //explorePager.setPageMargin(-1 * (MiscUtils.dpToPx(25)));
        /*explorePager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("Explore - Page Swiped")
                        .setAction("User Name - " + SharedPrefUtils.getUserName(preferences))
                        .setValue(1)
                        .build());

                if (SharedPrefUtils.getShowFacebookShareOrNot(preferences)) {
                    if (position == 10) {
                        if (preferences == null) {
                            preferences = SharedPrefUtils.getPreferences(getContext());
                        }

                        showFbDialog();
                        SharedPrefUtils.storeFacebookShareButtonVisibleOrNot(preferences, false);
                    }
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });*/
        noFriendsDiscoverLayoutContainer = rootView.findViewById(R.id.exploreNoFriendsContainer);
        final Button sendRequestToDevikaButton = (Button) rootView.findViewById(R.id.sendRequestButton);
        sendRequestToDevikaButton.setOnClickListener(v ->
                new SendRequest().executeOnExecutor(requestSender, StaticData.DEVIKA,
                        SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE))));
        getLoaderManager().initLoader(StaticData.FRIENDS_VERTICAL_LOADER, null, this);
        mPlayAllContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buffer != null) {
                    Vector<JsonObject> bufferData = buffer.getAllBufferData();
                    ReachActivity.nowPlaying.clear();
                    for (int i = 0; i < bufferData.size(); i++) {

                        JsonObject object = bufferData.get(i);
                        final JsonElement youtubeId = MiscUtils.get(object, ExploreJSON.YOUTUBE_ID);
                        if (youtubeId == null)
                            continue;
                        //TODO: Add song to the playList
                        ReachActivity.nowPlaying.add(youtubeId.getAsString());
                    }
                    if(mListener!=null){
                        mListener.playYoutubePlayList();
                    }
                    else{
                        Toast.makeText(getActivity().getApplicationContext(), "Sorry, an error occured!", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    //TODO: Modify the text message
                    Toast.makeText(getActivity().getApplicationContext(), "Sorry, an error occured!", Toast.LENGTH_SHORT).show();

                }
            }
        });

        return rootView;
    }

    private void showFbDialog() {

        if (fbShareDialog != null) {
            Log.d(TAG, "Invite Dialog is already showing");
            if (fbShareDialog.isShowing()) {
                return;
            }
        }


        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View vv = inflater.inflate(R.layout.fb_share_dialog_layout, null, false);
        ((TextView) vv.findViewById(R.id.inviteText)).setText(Html.fromHtml("Enjoying <b>Reach</b>? Share your love!"));
        vv.findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("Fb Share button in dialog clicked")
                        .setAction("Username = " + SharedPrefUtils.getUserName(preferences))
                        .setAction("User id = " + SharedPrefUtils.getServerId(preferences))
                        .setValue(1)
                        .build());
                sharePostOnFb();
                if (fbShareDialog != null)
                    fbShareDialog.dismiss();
            }
        });

        vv.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fbShareDialog != null)
                    fbShareDialog.dismiss();
            }
        });

        Log.d(TAG, "Show Invite Dialog");
        alertDialogBuilder.setView(vv);
        fbShareDialog = alertDialogBuilder.create();
        fbShareDialog.show();
        ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Facebook Dialog is shown")
                .setAction("Username = " + SharedPrefUtils.getUserName(preferences))
                .setAction("User id = " + SharedPrefUtils.getServerId(preferences))
                .setValue(1)
                .build());

    }

    private void sharePostOnFb() {

        try {
            ApplicationInfo info = getActivity().getPackageManager().
                    getApplicationInfo("com.facebook.katana", 0);

        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(getActivity().getApplicationContext(), "Please install the facebook application first!", Toast.LENGTH_SHORT).show();
            SharedPrefUtils.storeFacebookShareButtonVisibleOrNot(preferences, false);
            return;
        }

        final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View vv = inflater.inflate(R.layout.fb_share_layout, null);
        final TextView username = (TextView) vv.findViewById(R.id.username);
        username.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                "permanentmarker.ttf")
        );
        username.setText("- " + SharedPrefUtils.getUserName(preferences == null ? SharedPrefUtils.getPreferences(getActivity()) : preferences));
        vv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        vv.layout(0, 0, vv.getMeasuredWidth(), vv.getMeasuredHeight());

        final Bitmap vBitmap = Bitmap.createBitmap(vv.getMeasuredWidth(),
                vv.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(vBitmap);
        vv.draw(canvas);

        SharePhoto photo = new SharePhoto.Builder()
                .setBitmap(vBitmap)
                .build();
        SharePhotoContent content = new SharePhotoContent.Builder()
                .addPhoto(photo)
                .build();

        Toast.makeText(getActivity().getApplicationContext(), "Sharing On Facebook", Toast.LENGTH_SHORT).show();
        SharedPrefUtils.storeFacebookShareButtonVisibleOrNot(preferences, false);
        ShareDialog.show(getActivity(), content);

    }

    private void showDiscoverAdapter() {

        /*if (explorePager == null || exploreAdapter != null || explorePager.getAdapter() != null)
            return;*/

        if (exploreRecyclerView == null || exploreRecyclerViewAdapter != null || exploreRecyclerView.getAdapter() != null)
            return;


        /*exploreAdapter = new ExploreAdapter(this, this);
        explorePager.setAdapter(exploreAdapter);*/

        mPlayAllContainer.setVisibility(View.VISIBLE);
        exploreRecyclerViewAdapter = new ExploreRecyclerViewAdapter(getActivity(),this,this);

        exploreRecyclerView.setAdapter(exploreRecyclerViewAdapter);
//        explorePager.setOffscreenPageLimit(1);

        /*if (!SharedPrefUtils.getExploreCoach1Seen(preferences)) {
            if (mListener != null)
                mListener.showSwipeCoach();
            SharedPrefUtils.setExploreCoach1Seen(preferences);
        }*/
        noFriendsDiscoverLayoutContainer.setVisibility(View.GONE);
        /*explorePager.setVisibility(View.VISIBLE);*/
        exploreRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showNoFriendsDiscoverPage() {

        fetchDevikaDetails();

        if (exploreRecyclerViewAdapter != null)
            exploreRecyclerViewAdapter = null;
        noFriendsDiscoverLayoutContainer.setVisibility(View.VISIBLE);
        mPlayAllContainer.setVisibility(View.GONE);
        if (exploreRecyclerView != null) {
            exploreRecyclerView.setAdapter(null);
            exploreRecyclerView.setVisibility(View.GONE);
        }
    }

    private void fetchDevikaDetails() {

        final Cursor cursor = getActivity().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + StaticData.DEVIKA),
                new String[]{
                        ReachFriendsHelper.COLUMN_USER_NAME, //0
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS, //1
                        ReachFriendsHelper.COLUMN_IMAGE_ID, //2
                        ReachFriendsHelper.COLUMN_STATUS, //3
                        ReachFriendsHelper.COLUMN_NUMBER_OF_APPS, //4
                        ReachFriendsHelper.COLUMN_COVER_PIC_ID}, //5
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{StaticData.DEVIKA + ""}, null);

        if (cursor == null) {
            return;
        }

        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        final TextView userNameTxt = (TextView) rootView.findViewById(R.id.userName);
        final TextView musicCountTxt = (TextView) rootView.findViewById(R.id.musicCount);
        final TextView appCountTxt = (TextView) rootView.findViewById(R.id.appCount);
        final SimpleDraweeView profilePic = (SimpleDraweeView) rootView.findViewById(R.id.profilePic);
        userNameTxt.setText(cursor.getString(0));
        musicCountTxt.setText(cursor.getString(1));
        appCountTxt.setText(cursor.getString(4));
        profilePic.setController(MiscUtils.getControllerResize(profilePic.getController(),
                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + cursor.getString(2)), 100, 100));

        SimpleDraweeView coverPic = (SimpleDraweeView) rootView.findViewById(R.id.coverPic);
        coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + cursor.getString(5)), 500, 300));

    }


    public void onDestroyView() {
        super.onDestroyView();
        Log.d("Ayush", "ExploreFragment - onDestroyView");

        rootView = null;
        //explorePager = null;
        exploreRecyclerView = null;
        //exploreAdapter = null;
        exploreRecyclerViewAdapter = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d("Ayush", "ExploreFragment - onCreate");
    }

    @Override
    public synchronized Callable<Collection<JsonObject>> fetchNextBatch() {
//        Toast.makeText(activity, "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return FETCH_NEXT_BATCH;
    }

    @Override
    public JsonObject getContainerForIndex(int index) {

        //return data
        return buffer.getViewItem(index); //test can not be null
    }

    @Override
    public int getCount() {
        return buffer != null ? buffer.currentBufferSize() : 0; //test can not be null
    }

    @Override
    public boolean isDoneForToday() {
        return buffer == null || ExploreBuffer.DONE_FOR_TODAY.get();
    }

    @Override
    public void playYTVideo(String search) {
        //new YTTest().execute(search);
    }

    @Override
    public synchronized void notifyDataAvailable() {

        if (exploreRecyclerViewAdapter != null)
            exploreRecyclerViewAdapter.notifyDataSetChanged();

        /*if(buffer!=null) {
            Vector<JsonObject> bufferData = buffer.getAllBufferData();
            for(int i=ReachActivity.explorePlayList.size();i<bufferData.size();i++) {

                JsonObject object = bufferData.get(i);
                final JsonElement youtubeId = MiscUtils.get(object, ExploreJSON.YOUTUBE_ID);
                if (youtubeId == null)
                    continue;
                //TODO: Add song to the playList
                ReachActivity.explorePlayList.add(youtubeId.getAsString());
            }

        }*/
        /*if(mListener!=null){
            mListener.cueVideos();
        }*/
        //This is UI thread !
        Log.i("Ayush", "Notifying data set changed on explore adapter");

    }

    @Override
    public void loadedFromCache(int count) {

        notifyDataAvailable();
        /*if (explorePager != null)
            explorePager.postDelayed(new ScrollToLast(count), 1500L);*/
        if(exploreRecyclerView!=null){
            exploreRecyclerView.scrollToPosition(count-2);
        }
    }

    @Override
    public void handOverMessage(@Nonnull Object object) {

        if (object instanceof Integer) {
            //retrieve full json
            final JsonObject exploreJson = buffer.getViewItem((Integer) object); //test can not be null

            if (exploreJson == null)
                return;

            final ExploreTypes type = ExploreTypes.valueOf(MiscUtils.get(exploreJson, ExploreJSON.TYPE).getAsString());

            switch (type) {

                case MUSIC:
                    //addToDownload(exploreJson);
                    //mListener.playVideoAtParticularAdapterPosition((int)object);

                    break;

                case APP:
                    MiscUtils.openAppInPlayStore(getActivity(), MiscUtils.get(exploreJson, ExploreJSON.PACKAGE_NAME)
                            .getAsString(), MiscUtils.get(exploreJson, ExploreJSON.ID).getAsLong(), "EXPLORE");
                    break;

                case MISC:
                    final JsonObject metaInfo = exploreJson.get(ExploreJSON.META_INFO.getName()).getAsJsonObject();
                    final String activityClass = MiscUtils.get(metaInfo, MiscMetaInfo.CLASS_NAME).getAsString();
                    Class<?> mClass = null;
                    if (activityClass != null) {
                        try {
                            mClass = Class.forName(activityClass);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    final Intent intent = new Intent(getActivity(), mClass);
                    startActivity(intent);
                    break;
            }
        } else if (object instanceof SavedSongsDataModel) {
            final SavedSongsDataModel data = (SavedSongsDataModel) object;
            //Type: 1 = Saved, 2 = History
            if(data.getType() == 2) {
                mListener.showYTVideo(data.getYoutube_id());
                return;
            }
            new SaveSongInDatabaseTask(getActivity(),data).execute();


        } else if (object instanceof YouTubeDataModel) {

            final YouTubeDataModel data = (YouTubeDataModel) object;
            Log.d(TAG, "handOverMessage: fb_share_button, id = " + data.getId());
            MiscUtils.shareTextUrl(getActivity(),
                    "Hey! Checkout this song I found on the Reach App\nhttp://youtu.be/" + data.getId());
            /*ShareLinkContent.Builder content = new ShareLinkContent.Builder();
                    content.setContentUrl(Uri.parse("http://www.youtube.com/watch?v="+data.getId()));
                    content.setShareHashtag(new ShareHashtag.Builder()
                            .setHashtag("#IamReachable")
                            .build());
                    content.setQuote("Enjoying Reach");
                            if(data.getImageUrl() != null)
                    content.setImageUrl(Uri.parse(data.getImageUrl()));

            ShareDialog.show(getActivity(),content.build());*/

        } else if (object instanceof Long) {
            final long userId = (long) object;
            mListener.displayYourProfileFragment(userId);
        }

    }

    public void addToDownload(JsonObject exploreJSON) {
        /*final ContentResolver contentResolver = activity.getContentResolver();

        //extract meta info to process current click request
        final JsonObject metaInfo = exploreJSON.get(ExploreJSON.META_INFO.getName()).getAsJsonObject();

        //get user name and imageId
        final long senderId = MiscUtils.get(metaInfo, MusicMetaInfo.SENDER_ID).getAsLong();
        final String userName;

        Cursor cursor = contentResolver.query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + senderId),
                new String[]{ReachFriendsHelper.COLUMN_USER_NAME},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{senderId + ""}, null);

        if (cursor == null)
            return;

        try {
            if (cursor.moveToFirst())
                userName = cursor.getString(0);
            else
                return;
        } finally {
            cursor.close();
        }

        final ReachDatabase reachDatabase = new ReachDatabase.Builder()
                .setId(-1)
                .setSongId(MiscUtils.get(metaInfo, MusicMetaInfo.SONG_ID).getAsLong())
                .setReceiverId(myServerId)
                .setSenderId(senderId)
                .setOperationKind(ReachDatabase.OperationKind.DOWNLOAD_OP)
                .setUserName(userName)
                .setArtistName(MiscUtils.get(metaInfo, MusicMetaInfo.ARTIST, "").getAsString())
                .setDisplayName(MiscUtils.get(metaInfo, MusicMetaInfo.DISPLAY_NAME).getAsString())
                .setActualName(MiscUtils.get(metaInfo, MusicMetaInfo.ACTUAL_NAME).getAsString())
                .setLength(MiscUtils.get(metaInfo, MusicMetaInfo.SIZE).getAsLong())
                .setDateAdded(DateTime.now())
                .setUniqueId(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                .setDuration(MiscUtils.get(metaInfo, MusicMetaInfo.DURATION).getAsLong())
                .setAlbumName(MiscUtils.get(metaInfo, MusicMetaInfo.ALBUM, "").getAsString())
                .setAlbumArtData(new byte[0])
                .setGenre("")
                .setLiked(false)
                .setOnlineStatus(ReachFriendsHelper.Status.ONLINE_REQUEST_GRANTED)
                .setVisibility(true)
                .setPath("hello_world")
                .setProcessed(0)
                .setLogicalClock((short) 0)
                .setStatus(ReachDatabase.Status.NOT_WORKING).build();

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        MiscUtils.startDownload(reachDatabase, getActivity(), rootView, "EXPLORE");*/
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        buffer = ExploreBuffer.getInstance(this, context.getCacheDir(), BYTES_TO_JSON, JSON_TO_BYTES);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        Log.d("Ayush", "ExploreFragment - onDetach");
        mListener = null;
        if (buffer != null)
            buffer.close();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == StaticData.FRIENDS_VERTICAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI, null,
                    ReachFriendsHelper.COLUMN_STATUS + " < ?",
                    new String[]{ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED + ""}, null);
        return null;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER) {
            if (data.getCount() > 0) {
                showDiscoverAdapter();
                Log.d("ExploreFragment", "The user has friends and friends = " + data.getCount());
            } else {
                Log.d("ExploreFragment", "The user has no friends");
                showNoFriendsDiscoverPage();
            }
        } else
            throw new IllegalArgumentException("Illegal cursor inside ExploreFragment cursor id = " + loader.getId());

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /*private final class ScrollToLast implements Runnable {

        private final int scrollTo;

        private ScrollToLast(int scrollTo) {
            this.scrollTo = scrollTo;
        }

        @Override
        public void run() {

            //sanity check
            if (exploreRecyclerView == null || rootView == null)
                return;

            //magic scroll position should be available
            if (!(scrollTo > 1))
                return;

            final int currentItem = exploreRecyclerView.getVi;
            //user has somehow started scrolling
            if (currentItem > 0)
                return;

            explorePager.setCurrentItem(scrollTo - 2, true);
        }
    }*/

    private static final class SendRequest extends AsyncTask<Long, Void, Long> {

        @Override
        protected Long doInBackground(final Long... params) {

            /**
             * params[0] = other id
             * params[1] = my id
             * params[2] = status
             */

            final MyString dataAfterWork = MiscUtils.autoRetry(
                    () -> StaticData.MESSAGING_API.requestAccess(params[1], params[0]).execute(),
                    Optional.of(input -> (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false")))).orNull();

            final String toParse;
            if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()) || toParse.equals("false"))
                return params[0];
            return null;
        }

        @Override
        protected void onPostExecute(final Long response) {

            super.onPostExecute(response);

            if (response != null && response > 0) {

                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);

                //response becomes the id of failed person
                MiscUtils.useContextFromFragment(reference, activity -> {

                    Toast.makeText(activity, "Request Failed", Toast.LENGTH_SHORT).show();

                    activity.getContentResolver().update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + response),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{response + ""});
                    //activity.setRequestNotSent();
                });
            }

        }
    }

//    "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)"




}