//package reach.project.coreViews.push.music;
//
//import android.content.Context;
//import android.database.Cursor;
//import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.LoaderManager;
//import android.support.v4.content.CursorLoader;
//import android.support.v4.content.Loader;
//import android.support.v7.widget.RecyclerView;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import javax.annotation.Nonnull;
//
//import reach.project.R;
//import reach.project.core.ReachActivity;
//import reach.project.core.StaticData;
//import reach.project.music.ReachDatabase;
//import reach.project.music.SongHelper;
//import reach.project.music.SongProvider;
//import reach.project.coreViews.myProfile.EmptyRecyclerView;
//import reach.project.music.MySongsHelper;
//import reach.project.music.MySongsProvider;
//import reach.project.music.Song;
//import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
//import reach.project.utils.viewHelpers.HandOverMessage;
//
///**
// * Created by dexter on 25/11/15.
// */
//public class MyLibraryFragment extends Fragment implements HandOverMessage<Song>,
//        LoaderManager.LoaderCallbacks<Cursor> {
//
//    private static final String NO_SONGS_SHARE_TEXT = "No songs\nto\nshare!";
//    private EmptyRecyclerView mRecyclerView;
//
//    public static MyLibraryFragment getInstance(String header) {
//
//        final Bundle args = new Bundle();
//        args.putString("header", header);
//        MyLibraryFragment fragment = new MyLibraryFragment();
//        fragment.setArguments(args);
//        return fragment;
//    }
//
//    @Nullable
//    private ParentAdapter parentAdapter = null;
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//
//        final View rootView = inflater.inflate(R.layout.fragment_push_songs, container, false);
//        mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.recyclerView);
//        final View emptyView = rootView.findViewById(R.id.empty_imageView);
//        final Context context = mRecyclerView.getContext();
//        final TextView emptyViewText = (TextView) rootView.findViewById(R.id.empty_textView);
//        emptyViewText.setText(NO_SONGS_SHARE_TEXT);
//        mRecyclerView.setEmptyView(emptyView);
//        parentAdapter = new ParentAdapter(this);
//        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
//        mRecyclerView.setAdapter(parentAdapter);
//
//        getLoaderManager().initLoader(StaticData.PUSH_DOWNLOADED_LOADER, null, this);
//        getLoaderManager().initLoader(StaticData.PUSH_MY_LIBRARY_LOADER, null, this);
//
//        return rootView;
//    }
//
//
//    @Override
//    public void onDestroyView() {
//
//        super.onDestroyView();
//        getLoaderManager().destroyLoader(StaticData.PUSH_DOWNLOADED_LOADER);
//        getLoaderManager().destroyLoader(StaticData.PUSH_MY_LIBRARY_LOADER);
//        if (parentAdapter != null)
//            parentAdapter.close();
//    }
//
//    @Override
//    public void handOverMessage(@Nonnull Song song) {
//
//        if (ReachActivity.SELECTED_SONG_IDS.get(song.songId, false)) {
//
//            ReachActivity.SELECTED_SONGS.remove(song);
//            ReachActivity.SELECTED_SONG_IDS.remove(song.songId);
//            Log.i("Ayush", "Removing " + song.displayName);
//        } else {
//
//            ReachActivity.SELECTED_SONGS.add(song);
//            ReachActivity.SELECTED_SONG_IDS.append(song.songId, true);
//            Log.i("Ayush", "Adding " + song.displayName);
//        }
//
//        if (parentAdapter != null)
//            parentAdapter.setItemSelected(song.songId); //position not known TODO optimize
//    }
//
//    @Override
//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//
//        if (id == StaticData.PUSH_MY_LIBRARY_LOADER)
//            return new CursorLoader(getActivity(),
//                    MySongsProvider.CONTENT_URI,
//                    MySongsHelper.SONG_LIST,
//                    MySongsHelper.COLUMN_VISIBILITY + " = ?",
//                    new String[]{"1"},
//                    MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE"); //show all songs !
//        else if (id == StaticData.PUSH_DOWNLOADED_LOADER)
//            return new CursorLoader(getActivity(),
//                    SongProvider.CONTENT_URI,
//                    SongHelper.SONG_LIST,
//                    SongHelper.COLUMN_STATUS + " = ? and " + //show only finished
//                            SongHelper.COLUMN_VISIBILITY + " = ? and " + //show only visible
//                            SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
//                    new String[]{ReachDatabase.Status.FINISHED.getString(), "1", "0"},
//                    SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");
//
//        return null;
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//
//        if (data == null || data.isClosed() || parentAdapter == null)
//            return;
//
//        final int count = data.getCount();
//
//        if (loader.getId() == StaticData.PUSH_MY_LIBRARY_LOADER) {
//
//            parentAdapter.setNewMyLibraryCursor(data);
//            if (count != parentAdapter.myLibraryCount) //update only if count has changed
//                parentAdapter.updateRecentMusic(getRecentMyLibrary());
//
//
//        } else if (loader.getId() == StaticData.PUSH_DOWNLOADED_LOADER) {
//
//            parentAdapter.setNewDownLoadCursor(data);
//            if (count != parentAdapter.downloadedCount) //update only if count has changed
//                parentAdapter.updateRecentMusic(getRecentDownloaded());
//
//        }
//        mRecyclerView.checkIfEmpty(parentAdapter.getItemCount());
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Cursor> loader) {
//
//        if (parentAdapter == null)
//            return;
//
//        if (loader.getId() == StaticData.PUSH_MY_LIBRARY_LOADER)
//            parentAdapter.setNewMyLibraryCursor(null);
//        else if (loader.getId() == StaticData.PUSH_DOWNLOADED_LOADER)
//            parentAdapter.setNewDownLoadCursor(null);
//    }
//
//    @NonNull
//    private List<Song> getRecentDownloaded() {
//
//        final Cursor cursor = getContext().getContentResolver().query(SongProvider.CONTENT_URI,
//                SongHelper.SONG_LIST,
//                SongHelper.COLUMN_STATUS + " = ? and " + //show only finished
//                        SongHelper.COLUMN_VISIBILITY + " = ? and " + //show only visible
//                        SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
//                new String[]{ReachDatabase.Status.FINISHED.getString(), "1", "0"},
//                SongHelper.COLUMN_DATE_ADDED + " DESC, " +
//                        SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20"); //top 20
//
//        if (cursor == null)
//            return Collections.emptyList();
//
//        final List<Song> latestDownloaded = new ArrayList<>(cursor.getCount());
//        while (cursor.moveToNext())
//            latestDownloaded.add(SongHelper.getSong(cursor));
//
//        cursor.close();
//
//        return latestDownloaded;
//    }
//
//    @NonNull
//    private List<Song> getRecentMyLibrary() {
//
//        final Cursor cursor = getContext().getContentResolver().query(MySongsProvider.CONTENT_URI,
//                MySongsHelper.SONG_LIST,
//                MySongsHelper.COLUMN_VISIBILITY + " = ?"
//                , new String[]{"1"}, //show only visible
//                MySongsHelper.COLUMN_DATE_ADDED + " DESC, " +
//                        MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20");
//
//        if (cursor == null)
//            return Collections.emptyList();
//
//        final List<Song> latestMyLibrary = new ArrayList<>(cursor.getCount());
//        while (cursor.moveToNext())
//            latestMyLibrary.add(MySongsHelper.getSong(cursor));
//
//        cursor.close();
//
//        return latestMyLibrary;
//    }
//}
