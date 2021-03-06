package reach.project.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import reach.backend.entities.userApi.model.Friend;
import reach.backend.entities.userApi.model.JsonMap;
import reach.backend.entities.userApi.model.QuickSync;
import reach.backend.entities.userApi.model.StringList;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;

/**
 * Created by dexter on 19/07/15.
 */
class QuickSyncFriends implements Callable<QuickSyncFriends.Status> {

    enum Status {
        DEAD,
        OK,
        FULL_SYNC
    }

    private final WeakReference<Context> reference;
    private final long serverId;
    private final String myNumber;

    QuickSyncFriends(WeakReference<Context> reference, long serverId, String myNumber) {
        this.reference = reference;
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    @Override
    public QuickSyncFriends.Status call() {

        //prepare data for quickSync call
        final Cursor currentIds = MiscUtils.useContextFromContext(reference, context -> {
            return context.getContentResolver().query(
                    ReachFriendsProvider.CONTENT_URI,
                    new String[]{
                            ReachFriendsHelper.COLUMN_ID, //long
                            ReachFriendsHelper.COLUMN_HASH, //int
                            ReachFriendsHelper.COLUMN_PHONE_NUMBER, //int
                            ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS, //old number of songs
                            ReachFriendsHelper.COLUMN_NUMBER_OF_APPS //old number of apps
                    }, null, null, null);
        }).orNull();

        //if no previous data found run full sync
        if (currentIds == null || currentIds.getCount() == 0) {
            new ForceSyncFriends(reference, serverId, myNumber).run();
            return Status.FULL_SYNC;
        }

        //add data for quickSync
        final List<Long> ids = new ArrayList<>();
        final List<Integer> hashes = new ArrayList<>();
        final LongSparseArray<Integer> oldNumberOfSongs = new LongSparseArray<>();
        final LongSparseArray<Integer> oldNumberOfApps = new LongSparseArray<>();

        //Now we collect the phoneNumbers on device
        final HashSet<String> numbers = new HashSet<>();
        numbers.add("8860872102"); //Devika
        //scan phoneBook, add if found
        MiscUtils.useContextFromContext(reference, context -> {
            numbers.addAll(MiscUtils.scanPhoneBook(context.getContentResolver()));
            return null;
        });
        Log.i("Ayush", "Prepared numbers" + numbers.size());

        while (currentIds.moveToNext()) {

            final long id = currentIds.getLong(0); //id
            ids.add(id);
            hashes.add(currentIds.getInt(1)); //hash
            numbers.remove(currentIds.getString(2)); //phoneNumber
            oldNumberOfSongs.append(id, currentIds.getInt(3)); //old numberOfSongs
            oldNumberOfApps.append(id, currentIds.getInt(4)); //old oldNumberOfApps
        }
        currentIds.close();

        //run Quick sync
        final HashSet<Friend> newFriends = new HashSet<>();
        final HashSet<Long> toDelete = new HashSet<>();
        Log.i("Ayush", "Prepared callData quickSync" + ids.size() + " " + hashes.size());
        final QuickSync quickSync = MiscUtils.autoRetry(() -> StaticData.USER_API.quickSync(serverId, hashes, ids).execute(), Optional.absent()).orNull();
        if (quickSync != null) {

            Log.i("Ayush", "Found quick sync");

            if (quickSync.getNewFriends() != null)
                for (Friend friend : quickSync.getNewFriends()) {
                    //new friend found in quick sync
                    numbers.remove(friend.getPhoneNumber());
                    newFriends.add(friend);
                }

            if (quickSync.getToUpdate() != null) {
                /**
                 * We update by deletion followed by insertion.
                 * Friends with hash = 0 are meant for removal
                 */
                for (Friend friend : quickSync.getToUpdate()) {

                    numbers.remove(friend.getPhoneNumber());
                    toDelete.add(friend.getId());

                    if (friend.getHash() != 0)
                        newFriends.add(friend); //its an update
                    //else it was a deletion
                }
            }
        }

        //removed all present phoneNumbers, now sync phoneBook
        numbers.remove(myNumber);
        if (!numbers.isEmpty()) {
            Log.i("Ayush", "Prepared callData phoneBookSync" + numbers.size());
            final StringList stringList = new StringList();
            stringList.setStringList(ImmutableList.copyOf(numbers));
            stringList.setUserId(serverId);
            newFriends.addAll(MiscUtils.autoRetry(() -> StaticData.USER_API.phoneBookSyncEvenNew(stringList).execute().getItems(), Optional.absent()).or(new ArrayList<>()));
        }

        //START DB COMMITS
        MiscUtils.useContextFromContext(reference, context -> {

            bulkInsert(context,
                    context.getContentResolver(),
                    newFriends,
                    toDelete,
                    quickSync != null ? quickSync.getNewStatus() : null,
                    oldNumberOfSongs,
                    oldNumberOfApps);
            return null;
        });

        MiscUtils.closeQuietly(numbers, ids, hashes, newFriends, toDelete);
        oldNumberOfSongs.clear();
        return Status.OK;
    }

    private void bulkInsert(Context context,
                            ContentResolver resolver,
                            Iterable<Friend> toInsert,
                            Iterable<Long> toDelete,
                            JsonMap statusChange,
                            LongSparseArray<Integer> oldNumberOfSongs,
                            LongSparseArray<Integer> oldNumberOfApps) {

        final ReachFriendsHelper reachFriendsHelper = new ReachFriendsHelper(context);
        final SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        sqlDB.beginTransaction();

        try {

            if (statusChange != null && statusChange.size() > 0) {

                ContentValues values = new ContentValues();
                for (Map.Entry<String, Object> newStatus : statusChange.entrySet()) {

                    values.put(ReachFriendsHelper.COLUMN_ID, newStatus.getKey());
                    values.put(ReachFriendsHelper.COLUMN_STATUS, newStatus.getValue() + "");
//                    Log.i("Ayush", "Updating status " + newStatus.getKey() + " " + newStatus.getValue());
                    sqlDB.update(ReachFriendsHelper.FRIENDS_TABLE,
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = " + newStatus.getKey(), null);
                }
                statusChange.clear();
            }

            for (Long id : toDelete) {

                Log.i("Ayush", "Deleting " + id);
                sqlDB.delete(ReachFriendsHelper.FRIENDS_TABLE,
                        ReachFriendsHelper.COLUMN_ID + "=" + id, null);
            }

            for (Friend friend : toInsert) {

                Log.i("Ayush", "Inserting " + friend.getUserName());
                final ContentValues values = ReachFriendsHelper.contentValuesCreator(friend, oldNumberOfSongs.get(friend.getId(), 0), oldNumberOfApps.get(friend.getId(), 0));
                sqlDB.insert(ReachFriendsHelper.FRIENDS_TABLE, null, values);
            }
            sqlDB.setTransactionSuccessful();

        } finally {

            sqlDB.endTransaction();
            reachFriendsHelper.close();
        }
        resolver.notifyChange(ReachFriendsProvider.CONTENT_URI, null);
    }
}