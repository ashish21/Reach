package reach.project.music.songs;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.music.playLists.ReachPlayListHelper;
import reach.project.music.playLists.ReachPlayListProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;


public class MusicListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private static SearchView searchView;

    private SuperInterface mListener;
    private ReachMusicAdapter reachMusicAdapter = null;
    private String mCurFilter;
    private String whereClause;
    private String[] whereArgs;
    private long userId;
    private Toolbar toolbar;

    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mListener != null) {

                final Cursor cursor = (Cursor) reachMusicAdapter.getItem(position);
                final long senderId = cursor.getLong(2);

                final Cursor senderCursor = getActivity().getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + senderId),
                        new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                                ReachFriendsHelper.COLUMN_STATUS,
                                ReachFriendsHelper.COLUMN_NETWORK_TYPE},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{senderId + ""}, null);

                if (senderCursor == null)
                    return;
                if (!senderCursor.moveToFirst()) {
                    senderCursor.close();
                    return;
                }
                if (searchView != null)
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
                if (SharedPrefUtils.getReachQueueSeen(sharedPreferences)) {
                    Snackbar.make(parent, cursor.getString(3) + " added to your Queue", Snackbar.LENGTH_LONG)
                            .setAction("VIEW", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mListener.anchorFooter();
                                }
                            })
                            .show();
                } else {
                    SharedPrefUtils.setReachQueueSeen(sharedPreferences);
                    mListener.anchorFooter();
                }

                mListener.addSongToQueue(
                        cursor.getLong(1), //songId
                        cursor.getLong(2), //senderId
                        cursor.getLong(8), //size
                        cursor.getString(3), //displayName
                        cursor.getString(4), //actualName
                        false, //multiple
                        senderCursor.getString(0), //userName
                        senderCursor.getShort(1) + "", //onlineStatus
                        senderCursor.getShort(2) + "", //networkType
                        cursor.getString(5), //artistName
                        cursor.getLong(7), //duration
                        cursor.getString(6), //album
                        cursor.getString(11), //genre
                        cursor.getBlob(10)); //albumArtData
                senderCursor.close();
            }
        }
    };

    private static WeakReference<MusicListFragment> pagerReference;
    private static WeakReference<MusicListFragment> typeReference;

    public static MusicListFragment newPagerInstance(long id, int type, SearchView sView) {

        if (type == 0)
            searchView = sView;
        final Bundle args;
        MusicListFragment fragment;
        if (pagerReference == null || (fragment = pagerReference.get()) == null) {
            pagerReference = new WeakReference<>(fragment = new MusicListFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing Music list fragment object :)");
            args = fragment.getArguments();
        }

        args.putLong("id", id);
        args.putInt("type", type);
        //0 = songs, 1 = albums, 2 = artists, 3 = playlists
        return fragment;
    }

    public static MusicListFragment newTypeInstance(long id, String albumName, String artistName, String playListName, int type) {

        final Bundle args;
        MusicListFragment fragment;
        if (typeReference == null || (fragment = typeReference.get()) == null) {
            typeReference = new WeakReference<>(fragment = new MusicListFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing Music list fragment object :)");
            args = fragment.getArguments();
        }

        args.putLong("id", id);
        args.putString("albumName", albumName);
        args.putString("artistName", artistName);
        args.putString("playListName", playListName);
        args.putInt("type", type);
        //0 = songs, 1 = albums, 2 = artists, 3 = playlists
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView musicList = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        toolbar = ((Toolbar)rootView.findViewById(R.id.listToolbar));
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        final int type = getArguments().getInt("type");
        if (type != 0) {
            toolbar.inflateMenu(R.menu.search_menu);
            searchView = (SearchView) toolbar.getMenu().findItem(R.id.search_button).getActionView();
        }
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        switch (type) {
            case 0:
                rootView.findViewById(R.id.musicListShadow).setVisibility(View.INVISIBLE);
                ((LinearLayout) rootView).removeViewAt(0);
                break;
            case 1:
                toolbar.setTitle(getArguments().getString("albumName"));
                break;
            case 2:
                toolbar.setTitle(getArguments().getString("artistName"));
                break;
            case 3:
                toolbar.setTitle(getArguments().getString("playListName"));
                break;
        }

        userId = getArguments().getLong("id");
        switch (type) {

            case 0: {

                whereClause = ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                        ReachSongHelper.COLUMN_VISIBILITY + " = ? ";
                whereArgs = new String[]{userId + "", 1 + ""};
                break;
            }
            case 1: {

                whereClause = ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                        ReachSongHelper.COLUMN_VISIBILITY + " = ? and " +
                        ReachSongHelper.COLUMN_ALBUM + " = ? ";
                whereArgs = new String[]{userId + "", 1 + "", getArguments().getString("albumName")};
                break;
            }
            case 2: {

                whereClause = ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                        ReachSongHelper.COLUMN_VISIBILITY + " = ? and " +
                        ReachSongHelper.COLUMN_ARTIST + " = ? ";
                whereArgs = new String[]{userId + "", 1 + "", getArguments().getString("artistName")};
                break;
            }
            case 3: {

                final Cursor cursor = getActivity().getContentResolver().query(
                        ReachPlayListProvider.CONTENT_URI,
                        new String[]{ReachPlayListHelper.COLUMN_ARRAY_OF_SONG_IDS},
                        ReachPlayListHelper.COLUMN_USER_ID + " = ? and " +
                                ReachPlayListHelper.COLUMN_PLAY_LIST_NAME + " = ?",
                        new String[]{
                                userId + "",
                                getArguments().getString("playListName")},
                        null);

                final String[] ids;
                if (cursor == null ||
                        !cursor.moveToFirst() ||
                        (ids = ReachPlayListHelper.toStringArray(cursor.getBlob(0))) == null ||
                        ids.length == 0) {

                    if (cursor != null)
                        cursor.close();
                    MiscUtils.setEmptyTextForListView(musicList, "No visible songs found");
                    return rootView;
                }
                cursor.close();

                Log.i("Ayush", "Looking for playLists in " + Arrays.toString(ids));
                final int argCount = ids.length; // number of IN arguments
                final StringBuilder inList = new StringBuilder(argCount * 2);
                for (int i = 0; i < argCount; i++) {
                    if (i > 0)
                        inList.append(",");
                    inList.append("?");
                }
                whereClause = ReachSongHelper.COLUMN_SONG_ID + " IN (" + inList.toString() + ") and " +
                        ReachSongHelper.COLUMN_VISIBILITY + " = 1 and " +
                        ReachSongHelper.COLUMN_USER_ID + " = " + userId;
                whereArgs = ids;
                break;
            }
        }

        reachMusicAdapter = new ReachMusicAdapter(getActivity(), R.layout.musiclist_item, null, 0, ReachMusicAdapter.LIST);
        musicList.setAdapter(reachMusicAdapter);
        musicList.setOnItemClickListener(listener);
        getLoaderManager().initLoader(StaticData.SONGS_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {

        if (getArguments().getInt("type") != 0) {
            toolbar.setSubtitle("");
        }
        getLoaderManager().destroyLoader(StaticData.SONGS_LOADER);
        if (reachMusicAdapter != null && reachMusicAdapter.getCursor() != null && !reachMusicAdapter.getCursor().isClosed())
            reachMusicAdapter.getCursor().close();
        reachMusicAdapter = null;

        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        //searchView = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachSongProvider.CONTENT_URI,
                StaticData.DISK_COMPLETE_NO_PATH,
                whereClause,
                whereArgs,
                ReachSongHelper.COLUMN_DATE_ADDED + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() == StaticData.SONGS_LOADER && cursor != null && !cursor.isClosed()) {

            reachMusicAdapter.swapCursor(cursor);
//            int count = cursor.getCount();
//            final Future<?> isMusicFetching = ContactsListFragment.isMusicFetching.get(userId);
//            if((isMusicFetching == null || isMusicFetching.isCancelled() || isMusicFetching.isDone()) && count==0 && musicList!=null)
//                MiscUtils.setEmptyTextForListView(musicList,"No visible songs found");
//            if (getArguments().getInt("type") != 0) {
//
//                final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//                if (actionBar != null)
//                    actionBar.setSubtitle(count + " Songs");
//            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.SONGS_LOADER)
            reachMusicAdapter.swapCursor(null);
    }

    @Override
    public boolean onClose() {

        searchView.setQuery(null, true);
        whereClause = ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                ReachSongHelper.COLUMN_VISIBILITY + " = ? ";
        whereArgs = new String[]{userId + "", 1 + ""};
        getLoaderManager().restartLoader(StaticData.SONGS_LOADER, null, this);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if (searchView == null)
            return false;

        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;
        final String filter = "%" + mCurFilter + "%";

        if (TextUtils.isEmpty(newText)) {
            whereClause = ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                    ReachSongHelper.COLUMN_VISIBILITY + " = ? ";
            whereArgs = new String[]{userId + "", 1 + ""};
        } else {
            whereClause =
                    ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                            ReachSongHelper.COLUMN_VISIBILITY + " = ? and (" +
                            ReachSongHelper.COLUMN_ACTUAL_NAME + " LIKE ? or " +
                            ReachSongHelper.COLUMN_ARTIST + " LIKE ? or " +
                            ReachSongHelper.COLUMN_ALBUM + " LIKE ? or " +
                            ReachSongHelper.COLUMN_DISPLAY_NAME + " LIKE ?)";
            whereArgs = new String[]{userId + "", 1 + "", filter, filter};
        }
        Log.i("Ayush", "Selection " + whereClause + " SelectionArguments " + Arrays.toString(whereArgs));
        getLoaderManager().restartLoader(StaticData.SONGS_LOADER, null, this);
        return true;
    }
}