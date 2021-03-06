package reach.project.utils;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

import reach.backend.entities.userApi.model.Friend;
import reach.backend.entities.userApi.model.StringList;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;

/**
 * Created by dexter on 19/07/15.
 */
final class ForceSyncFriends implements Runnable {

    private final WeakReference<Context> reference;
    private final long serverId;
    private final String myNumber;

    ForceSyncFriends(WeakReference<Context> reference, long serverId, String myNumber) {
        this.reference = reference;
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    @Override
    public void run() {

        //First we fetch the list of 'KNOWN' friends
        final List<Friend> fullSync = serverId == 0 ? null : MiscUtils.autoRetry(() -> StaticData.USER_API.longSync(serverId).execute().getItems(), Optional.absent()).orNull();

        //Now we collect the phoneNumbers on device
        final HashSet<String> numbers = new HashSet<>();
        numbers.add(StaticData.DEVIKA_PHONE_NUMBER); //Devika
        //scan phoneBook, add if found
        MiscUtils.useContextFromContext(reference, context -> {
            numbers.addAll(MiscUtils.scanPhoneBook(context.getContentResolver()));
            return null;
        });
        Log.i("Ayush", "Prepared numbers" + numbers.size());

        //Now we remove the duplicate numbers and sync phoneBook
        if (fullSync != null)
            for (Friend friend : fullSync)
                numbers.remove(friend.getPhoneNumber());
        numbers.remove(myNumber); //sanity check

        final List<Friend> phoneBookSync;
        if (numbers.isEmpty())
            phoneBookSync = null;
        else {

            Log.i("Ayush", "Prepared callData phoneBookSync" + numbers.size());
            final StringList stringList = new StringList();
            stringList.setStringList(ImmutableList.copyOf(numbers));
            stringList.setUserId(serverId);
            phoneBookSync = MiscUtils.autoRetry(() -> StaticData.USER_API.phoneBookSyncEvenNew(stringList).execute().getItems(), Optional.absent()).orNull();
        }

        //Finally we insert the received contacts
        final int size1 = (fullSync == null) ? 0 : fullSync.size();
        final int size2 = (phoneBookSync == null) ? 0 : phoneBookSync.size();
        Log.i("Ayush", "Sizes " + size1 + " " + size2);
        if (size1 + size2 == 0)
            return;

        final HashSet<ContentValues> values;
        values = new HashSet<>();
        if (size1 > 0)
            for (Friend friend : fullSync) {
                Log.i("Ayush", friend.getUserName() + friend.getStatus() + " " + friend.getId());
                values.add(ReachFriendsHelper.contentValuesCreator(friend));
            }
        if (size2 > 0)
            for (Friend friend : phoneBookSync) {
                Log.i("Ayush", friend.getUserName() + friend.getStatus() + " " + friend.getId());
                values.add(ReachFriendsHelper.contentValuesCreator(friend));
            }

        MiscUtils.useContextFromContext(reference, context -> context.getContentResolver().bulkInsert(ReachFriendsProvider.CONTENT_URI,
                values.toArray(new ContentValues[size1 + size2])) > 0).or(false);

        MiscUtils.closeQuietly(fullSync, numbers, values);
    }
}