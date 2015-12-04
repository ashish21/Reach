package reach.project.coreViews.push;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.Song;
import reach.project.music.TransferSong;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * A placeholder fragment containing a simple view.
 */
public class PushFilesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage {

    private static WeakReference<PushFilesFragment> reference = null;
    private static long myUserId = 0;

    public static PushFilesFragment getInstance(String header) {

        final Bundle args;
        PushFilesFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new PushFilesFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing PushFilesFragment object :)");
            args = fragment.getArguments();
        }
        args.putString("header", header);
        return fragment;
    }

    //Initialize data sets
    private final RecentAppsAdapter recentAppsAdapter;
    private final RecentMusicAdapter recentMusicAdapter;

    //TODO test
    public final Set<TransferSong> selectedSongs = MiscUtils.getSet(5);
    public final Set<String> selectedPackages = MiscUtils.getSet(5);

    {

        final List<App> appList = new ArrayList<>(100);
//        appList.add(new App.Builder().build());

        final List<Song> songList = new ArrayList<>(100);
//        songList.add(new Song.Builder().build());

        recentAppsAdapter = new RecentAppsAdapter(appList, this, R.layout.push_files_item);
        recentMusicAdapter = new RecentMusicAdapter(songList, this, R.layout.push_files_item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_push_files, container, false);
        final RecyclerView parent = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.pushToolbar);
        toolbar.setTitle("Send Files");
//        toolbar.inflateMenu(R.menu.menu_push);
//        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                return false;
//            }
//        });

        final Activity activity = getActivity();
        parent.setLayoutManager(new CustomLinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        parent.setAdapter(new ParentAdapter(recentAppsAdapter, recentMusicAdapter));

        getLoaderManager().initLoader(StaticData.PRIVACY_DOWNLOADED_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER, null, this);
        new GetApplications().executeOnExecutor(StaticData.temporaryFix, activity);

        return rootView;
    }

    @Override
    public void handOverMessage(@Nonnull Object message) {

        if (message instanceof App) {

            final App app = (App) message;
            final boolean isSelected;
            if (recentAppsAdapter.selected.containsKey(app.packageName))
                isSelected = recentAppsAdapter.selected.get(app.packageName);
            else
                isSelected = false;

            final String packageName = app.packageName;

            if (!isSelected) {

                if (selectedPackages.size() < 5) {

                    recentAppsAdapter.selected.put(packageName, true);
                    selectedPackages.add(packageName);
                } else
                    Toast.makeText(getContext(), "Maximum 5 apps allowed", Toast.LENGTH_SHORT).show();
            } else {

                recentAppsAdapter.selected.put(packageName, false);
                selectedPackages.remove(packageName);
            }

            recentAppsAdapter.selectionChanged(packageName);

        } else if (message instanceof Song) {

            final Song song = (Song) message;
            final boolean isSelected = recentMusicAdapter.selected.get(song.songId, false);

            //create transfer song object
            final TransferSong transferSong = new TransferSong(
                    song.size, //size of song
                    song.songId, //songId
                    song.duration, //duration
                    song.displayName, //displayName
                    song.actualName, //actualName
                    song.artist, //artistName
                    song.album, //album
                    "bitch", //genre TODO
                    new byte[]{}); //albumArtData

            if (!isSelected) {

                if (selectedSongs.size() < 5) {

                    recentMusicAdapter.selected.append(song.songId, true);
                    selectedSongs.add(transferSong);
                } else
                    Toast.makeText(getContext(), "Maximum 5 Songs allowed", Toast.LENGTH_SHORT).show();
            } else {

                recentMusicAdapter.selected.append(song.songId, false);
                selectedSongs.remove(transferSong);
            }

            recentMusicAdapter.selectionChanged(song.songId);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.PRIVACY_MY_LIBRARY_LOADER)
            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    projectionMyLibrary,
                    null, null, null); //show all songs !
        else if (id == StaticData.PRIVACY_DOWNLOADED_LOADER)
            return new CursorLoader(getActivity(),
                    ReachDatabaseProvider.CONTENT_URI,
                    projectionDownloaded,
                    ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.FINISHED + "", "0"}, null);

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER) {
            recentMusicAdapter.updateRecent(getRecentMyLibrary());
        } else if (loader.getId() == StaticData.PRIVACY_DOWNLOADED_LOADER) {
            recentMusicAdapter.updateRecent(getRecentDownloaded());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER) {
            //TODO
        } else if (loader.getId() == StaticData.PRIVACY_DOWNLOADED_LOADER) {
            //TODO
        }
    }

    private final String[] projectionMyLibrary =
            {
                    MySongsHelper.COLUMN_ID, //0

                    MySongsHelper.COLUMN_SONG_ID, //1
                    MySongsHelper.COLUMN_USER_ID, //2

                    MySongsHelper.COLUMN_DISPLAY_NAME, //3
                    MySongsHelper.COLUMN_ACTUAL_NAME, //4

                    MySongsHelper.COLUMN_ARTIST, //5
                    MySongsHelper.COLUMN_ALBUM, //6

                    MySongsHelper.COLUMN_DURATION, //7
                    MySongsHelper.COLUMN_SIZE, //8

                    MySongsHelper.COLUMN_VISIBILITY, //9
                    MySongsHelper.COLUMN_GENRE //10
            };

    private final String[] projectionDownloaded =
            {
                    ReachDatabaseHelper.COLUMN_ID, //0

                    ReachDatabaseHelper.COLUMN_UNIQUE_ID, //1
                    ReachDatabaseHelper.COLUMN_RECEIVER_ID, //2

                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //3
                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //4

                    ReachDatabaseHelper.COLUMN_ARTIST, //5
                    ReachDatabaseHelper.COLUMN_ALBUM, //6

                    ReachDatabaseHelper.COLUMN_DURATION, //7
                    ReachDatabaseHelper.COLUMN_SIZE, //8

                    ReachDatabaseHelper.COLUMN_VISIBILITY, //9
                    ReachDatabaseHelper.COLUMN_GENRE //10
            };

    @NonNull
    private List<Song> getRecentDownloaded() {

        final Cursor cursor = getContext().getContentResolver().query(ReachDatabaseProvider.CONTENT_URI,
                projectionDownloaded,
                ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                        ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                new String[]{ReachDatabase.FINISHED + "", "0"}, null); //no need to sort here

        if (cursor == null || cursor.getCount() == 0)
            return Collections.emptyList();

        final List<Song> latestDownloaded = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {

            final Song.Builder songBuilder = new Song.Builder();
            songBuilder.songId(cursor.getLong(1));
            songBuilder.displayName(cursor.getString(3));
            songBuilder.actualName(cursor.getString(4));
            songBuilder.artist(cursor.getString(5));
            songBuilder.album(cursor.getString(6));
            songBuilder.duration(cursor.getLong(7));
            songBuilder.size(cursor.getLong(8));
            songBuilder.visibility(cursor.getShort(9) == 1);

            latestDownloaded.add(songBuilder.build());
        }

        return latestDownloaded;
    }

    @NonNull
    private List<Song> getRecentMyLibrary() {

        final Cursor cursor = getContext().getContentResolver().query(MySongsProvider.CONTENT_URI,
                projectionMyLibrary,
                null, null, null); //no need to sort here

        if (cursor == null || cursor.getCount() == 0)
            return Collections.emptyList();

        final List<Song> latestDownloaded = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {

            final Song.Builder songBuilder = new Song.Builder();
            songBuilder.songId(cursor.getLong(1));
            songBuilder.displayName(cursor.getString(3));
            songBuilder.actualName(cursor.getString(4));
            songBuilder.artist(cursor.getString(5));
            songBuilder.album(cursor.getString(6));
            songBuilder.duration(cursor.getLong(7));
            songBuilder.size(cursor.getLong(8));
            songBuilder.visibility(cursor.getShort(9) == 1);

            latestDownloaded.add(songBuilder.build());
        }

        return latestDownloaded;
    }

    private static final class GetApplications extends AsyncTask<Context, Void, List<App>> {

        @Override
        protected List<App> doInBackground(Context... params) {

            final SharedPreferences preferences = params[0].getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final PackageManager packageManager = params[0].getPackageManager();

            return MiscUtils.getApplications(packageManager, preferences);
        }

        @Override
        protected void onPostExecute(List<App> appList) {

            super.onPostExecute(appList);

            MiscUtils.useFragment(reference, fragment -> {
                fragment.recentAppsAdapter.updateRecent(appList);
            });

        }
    }
}
