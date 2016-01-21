package reach.project.coreViews.friends;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.MyString;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

final class FriendsViewHolder extends SingleItemViewHolder {

    public final TextView userNameList, telephoneNumberList, appCount, lockText;
    public final ImageView lockIcon, optionsIcon;
    public final SimpleDraweeView profilePhotoList, coverPic;

    @Nullable
    private static WeakReference<HandOverWithContext> reference = null;

    protected FriendsViewHolder(View itemView,
                                HandOverWithContext handOverWithContext) {

        super(itemView, handOverWithContext);
        reference = new WeakReference<>(handOverWithContext);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        this.lockText = (TextView) itemView.findViewById(R.id.lockText);

        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setTag(new Object[]{getAdapterPosition(), null}); //int, popMenu (reuse)
        this.optionsIcon.setOnClickListener(optionsClickListener);
    }

    protected FriendsViewHolder(View itemView,
                                HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);
        reference = null; //not used

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        this.lockText = (TextView) itemView.findViewById(R.id.lockText);
        itemView.findViewById(R.id.optionsIcon).setVisibility(View.GONE);
        optionsIcon = null;
    }

    private static final View.OnClickListener optionsClickListener = view -> {

        final Object object = view.getTag();
        if (object == null || !(object instanceof Object[]))
            throw new IllegalStateException("Object array tag is expected");

        final Object[] objectArray = (Object[]) object;
        if (objectArray.length != 2 || !(objectArray[0] instanceof Integer))
            throw new IllegalStateException("Tag length of 2 is expected and int and popMenu");

        final int position = (int) objectArray[0];
        final Context context = view.getContext();
        PopupMenu popupMenu = (PopupMenu) objectArray[1];

        final Cursor cursor = MiscUtils.useReference(reference, handOverWithContext -> {
            return handOverWithContext.getCursor(position);
        }).orNull();

        if (cursor == null)
            return;

        if (popupMenu == null) {

            popupMenu = new PopupMenu(context, view);
            popupMenu.inflate(R.menu.friends_popup_menu);
            //cache the popMenu
            view.setTag(new Object[]{position, popupMenu});
        }

        popupMenu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {

                case R.id.friends_menu_1:
                    MiscUtils.useReference(reference, integerHandOverMessage -> {
                        integerHandOverMessage.handOverMessage(position);
                    });
                    return true;
                case R.id.friends_menu_2:

                    final WeakReference<ContentResolver> weakReference = new WeakReference<>(context.getContentResolver());
                    final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                    final long myId = SharedPrefUtils.getServerId(preferences);
                    final long hostId = cursor.getLong(0);
                    new RemoveFriend(weakReference).execute(myId, hostId);

                    return true;
                default:
                    return false;
            }
        });

        if (cursor.getShort(6) == ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED)
            popupMenu.getMenu().findItem(R.id.friends_menu_2).setTitle("Cancel Request");

        popupMenu.show();
    };

    private static final class RemoveFriend extends AsyncTask<Long, Void, Long> {

        private final WeakReference<ContentResolver> resolverWeakReference;
        private RemoveFriend(WeakReference<ContentResolver> resolverWeakReference) {
            this.resolverWeakReference = resolverWeakReference;
        }

        /**
         * @param params 0 : myID, 1 : hostID
         * @return hostId if success so that we can toggle
         */
        @Override
        protected Long doInBackground(Long... params) {

            try {
                final MyString response = StaticData.USER_API.removeFriend(params[0], params[1]).execute();
                return response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false") ? 0L : params[1];
            } catch (IOException e) {
                e.printStackTrace();
                return 0L;
            }
        }

        @Override
        protected void onPostExecute(Long hostId) {
            
            super.onPostExecute(hostId);
            if (hostId == 0)
                Log.d("Ayush", "Friend removal failed");
            else {

                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);
                MiscUtils.useReference(resolverWeakReference, contentResolver -> {

                    contentResolver.update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostId),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{hostId + ""});
                });
                
            }
        }
    }
}

