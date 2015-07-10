package reach.project.coreViews;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.entities.userApi.model.ContactsWrapper;
import reach.backend.entities.userApi.model.MusicContainer;
import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.ReachFriend;
import reach.backend.entities.userApi.model.ReachFriendCollection;
import reach.project.R;
import reach.project.adapter.ReachAllContactsAdapter;
import reach.project.adapter.ReachContactsAdapter;
import reach.project.core.ReachActivity;
import reach.project.core.StaticData;
import reach.project.database.ReachAlbumDatabase;
import reach.project.database.ReachArtistDatabase;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.viewHelpers.Contact;

public class ContactsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private ListView listView;
    private View rootView;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton actionButton;
    private SharedPreferences sharedPrefs;
    private MergeAdapter mergeAdapter;
    private ReachContactsAdapter reachContactsAdapter = null;
    private ReachAllContactsAdapter adapter = null;
    private final HashSet<String> inviteSentTo = new HashSet<>();
    private final String inviteKey = "invite_sent";
    private ProgressBar loading;
    private TextView emptyTV1,emptyTV2;

    private SuperInterface mListener;
    private String mCurFilter, selection;
    private String [] selectionArguments;
    private long serverId;
    private final AtomicBoolean pinging = new AtomicBoolean(false), refreshing = new AtomicBoolean(false);
    public static final LongSparseArray<Future<?>> isMusicFetching = new LongSparseArray<>();
    private ScaleAnimation translation;

    private final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            Object object = mergeAdapter.getItem(position);
            if (object instanceof Cursor) {
                final Cursor cursor = (Cursor) object;
                final long id = cursor.getLong(0) ;
                final short status = cursor.getShort(9);

                final Future<?> fetching = isMusicFetching.get(id, null);
                if(fetching == null || fetching.isDone() || fetching.isCancelled()) {


                    isMusicFetching.append(id,StaticData.threadPool.submit(new GetMusic(id,
                            getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS))));
                    //Inform only when necessary
                    if(status < 2 && cursor.getInt(7) == 0)
                        Toast.makeText(getActivity(), "Refreshing music list", Toast.LENGTH_SHORT).show();
                }

                if(mListener != null) {
                    if(status < 2)
                        mListener.onOpenLibrary(id);
                    else
                        mListener.onOpenProfileView(id);
                }
            }
            else if (object instanceof Contact){
                final Contact contact = (Contact) object;
                if (contact.isInviteSent()) return;
                StaticData.threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sendSMS(contact.getPhoneNumber());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                contact.setInviteSent(true);
                adapter.notifyDataSetChanged();
                inviteSentTo.add(contact.toString());
            }
        }
    };
    public static void sendSMS(String number) throws Exception
    {
        SendSMS smsObj = new SendSMS();
        smsObj.setparams("alerts.sinfini.com","sms","Aed8065339b18aedfbad998aeec2ce9b3","REACHM");
        smsObj.send_sms(number, "Hey! Checkout and download my phone music collection with just a click! Use my invite code "+MiscUtils.getInviteCode()+". https://play.google.com/store/apps/details?id=reach.project", "dlr_url");
        /*smsObj.schedule_sms("99xxxxxxxx", "message", "http://www.yourdomainname.domain/yourdlrpage&custom=XX",
                "YYYY-MM-DD HH:MM:SS");
        smsObj.unicode_sms("99xxxxxxxx", "message", "http://www.yourdomainname.domain/yourdlrpage&custom=XX","1");
        smsObj.messagedelivery_status("1xxx-x");
        smsObj.groupdelivery_status("1xxx");*/
        //smsObj.setsender_id("txxxxx");
        //smsObj.setworking_key("1ixxxxxxxxxxxxxx");
        //smsObj.setapi_url("sms_service_url");
    }
    private final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            if (!pinging.get()) {
                pinging.set(true);
                new SendPing().executeOnExecutor(StaticData.threadPool);
            }
        }
    };
    private final AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if (listView.getChildCount() > 0) {

                final boolean firstItemVisible = listView.getFirstVisiblePosition() == 0;
                final boolean topOfFirstItemVisible = listView.getChildAt(0).getTop() == 0;
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };
    private final View.OnClickListener pushLibraryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onOpenPushLibrary();
        }
    };

    public ContactsListFragment() {
    }

    private static WeakReference<ContactsListFragment> reference = null;
    public static ContactsListFragment newInstance(boolean first) {

        final Bundle args;
        ContactsListFragment fragment;
        if(reference == null || (fragment = reference.get()) == null) {

            Log.i("Ayush", "Creating new instance of contacts list fragment");
            reference = new WeakReference<>(fragment = new ContactsListFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing contacts list fragment object :)");
            args = fragment.getArguments();
        }

        args.putBoolean("first", first);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mListener.setUpDrawer();
        mListener.toggleDrawer(false);
        mListener.toggleSliding(true);
        /*if (getArguments().getBoolean("first", false))
            new InfoDialog().show(getChildFragmentManager(),"info_dialog");*/

        serverId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
        if(!refreshing.get()) {
            if (serverId > 0) {
                refreshing.set(true);
                new FindAndAddFriends().executeOnExecutor(StaticData.threadPool);
            }
        }

        /**
         * Invalidate everyone
         */
        final ContentValues contentValues = new ContentValues();
        contentValues.put(ReachFriendsHelper.COLUMN_LAST_SEEN, System.currentTimeMillis() + 31*1000);
        contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
        contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);
        getActivity().getContentResolver().update(
                ReachFriendsProvider.CONTENT_URI,
                contentValues,
                ReachFriendsHelper.COLUMN_STATUS + " = ?",
                new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + ""});
        contentValues.clear();
        StaticData.networkCache.clear();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachFriendsProvider.CONTENT_URI,
                ReachFriendsHelper.projection,
                selection,
                selectionArguments,
                ReachFriendsHelper.COLUMN_USER_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if(cursorLoader.getId() == StaticData.FRIENDS_LOADER && cursor != null && !cursor.isClosed()) {

            if(cursor.getCount() > 0) {
                if (!SharedPrefUtils.getFirstIntroSeen(sharedPrefs))  {
                    StaticData.threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap bmp = null;
                            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getActivity());
                            try {
                                bmp = Picasso.with(getActivity())
                                        .load("https://scontent-sin1-1.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/1011255_638449632916744_321328860_n.jpg?oh=5c1daa8d7d015f7ce698ee1793d5a929&oe=55EECF36&dl=1")
                                        .centerCrop()
                                        .resize(96, 96)
                                        .get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Intent intent = new Intent(getActivity(), ReachActivity.class);
                            //intent.putExtra("notifID",99910);
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                                    .setAutoCancel(true)
                                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                    .setSmallIcon(R.drawable.ic_icon_notif)
                                    .setLargeIcon(bmp)
                                    .setContentIntent(pendingIntent)
                                    //.addAction(0, "Okay! I got it", pendingIntent)
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText("I am Devika from Team Reach! \n" +
                                                    "Send me an access request by clicking on the lock icon beside my name to view my music collection. \n" +
                                                    "Keep Reaching ;)"))
                                    .setContentTitle("Hey!")
                                    .setTicker("Hey! I am Devika from Team Reach! Send me an access request by clicking on the lock icon beside my name to view my music collection. Keep Reaching ;)")
                                    .setContentText("I am Devika from Team Reach! \n" +
                                            "Send me an access request by clicking on the lock icon beside my name to view my music collection. \n" +
                                            "Keep Reaching ;)")
                                    .setPriority(NotificationCompat.PRIORITY_MAX);
                            managerCompat.notify(99910, builder.build());
                        }
                    });
                    SharedPrefUtils.setFirstIntroSeen(sharedPrefs.edit());
                }
                mergeAdapter.setActive(loading,false);
                mergeAdapter.setActive(emptyTV1,false);
                actionButton.setVisibility(View.VISIBLE);
                if (!translation.hasStarted())
                    actionButton.startAnimation(translation);
            }
            else
                actionButton.setVisibility(View.GONE);
            reachContactsAdapter.swapCursor(cursor);

            if(!refreshing.get() && cursor.getCount() == 0) {

                Log.i("Downloader", "LOADING EMPTY VIEW ON LOAD FINISHED");
                mergeAdapter.setActive(emptyTV1,true);
                MiscUtils.setEmptyTextforListView(listView, "No friends found :(");
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == StaticData.FRIENDS_LOADER) {
            reachContactsAdapter.swapCursor(null);
            actionButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.FRIENDS_LOADER);
        if(reachContactsAdapter != null && reachContactsAdapter.getCursor() != null && !reachContactsAdapter.getCursor().isClosed())
            reachContactsAdapter.getCursor().close();
        reachContactsAdapter = null;

        if (adapter!=null)
            adapter.cleanUp();

        sharedPrefs.edit().putStringSet(inviteKey, inviteSentTo).apply();

        swipeRefreshLayout.setOnRefreshListener(null);
        listView.setOnItemClickListener(null);
        //listView.setOnScrollListener(null);

        actionButton = null;
        rootView = null;
        if(searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        listView = null;
        swipeRefreshLayout = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if (actionBar!=null) {
            actionBar.show();
            actionBar.setTitle("My Reach");
            mListener.setUpNavigationViews();
        }
        sharedPrefs = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        inviteSentTo.clear();
        inviteSentTo.addAll(sharedPrefs.getStringSet(inviteKey, new HashSet<String>()));
        /**
         * All cursor operations should be background, else lag
         */
        translation = new ScaleAnimation(1f, 0.8f, 1f, 0.8f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        translation.setDuration(1000);
        translation.setInterpolator(new BounceInterpolator());

        if(reachContactsAdapter == null)
            reachContactsAdapter = new ReachContactsAdapter(getActivity(), R.layout.myreach_item, null, 0);
        selection = null;
        selectionArguments = null;

        if(serverId == 0)
            return rootView;

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainerContacts);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.reach_color), getResources().getColor(R.color.reach_blue));
        swipeRefreshLayout.setBackgroundResource(R.color.white);
        swipeRefreshLayout.setOnRefreshListener(refreshListener);

        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.contactsList));
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setOnItemClickListener(clickListener);
        mergeAdapter = new MergeAdapter();

        TextView textView = new TextView(getActivity());
        textView.setText("Friends on Reach");
        textView.setTextColor(getResources().getColor(R.color.reach_color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(10));
        mergeAdapter.addView(textView);

        loading = new ProgressBar(getActivity());
        loading.setIndeterminate(true);
        loading.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        mergeAdapter.addView(loading);
        mergeAdapter.addAdapter(reachContactsAdapter);

        emptyTV1 = new TextView(getActivity());
        emptyTV1.setText("No friends found");
        emptyTV1.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        //emptyTV1.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        emptyTV1.setPadding(MiscUtils.dpToPx(15),MiscUtils.dpToPx(15),0,MiscUtils.dpToPx(15));
        mergeAdapter.addView(emptyTV1,false);
        mergeAdapter.setActive(emptyTV1,false);

        TextView textView2 = new TextView(getActivity());
        textView2.setText("Invite Friends");
        textView2.setTextColor(getResources().getColor(R.color.reach_color));
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        textView2.setTypeface(textView2.getTypeface(), Typeface.BOLD);
        textView2.setPadding(MiscUtils.dpToPx(15),MiscUtils.dpToPx(15),0,MiscUtils.dpToPx(15));
        mergeAdapter.addView(textView2);

        emptyTV2 = new TextView(getActivity());
        emptyTV2.setText("No contacts found");
        emptyTV2.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        //emptyTV2.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        emptyTV2.setPadding(MiscUtils.dpToPx(15),MiscUtils.dpToPx(15),0,MiscUtils.dpToPx(15));
        mergeAdapter.addView(emptyTV2,false);
        mergeAdapter.setActive(emptyTV2,false);

        actionButton = (FloatingActionButton) rootView.findViewById(R.id.fab);
        actionButton.attachToListView(listView, null, scrollListener);
        actionButton.setOnClickListener(pushLibraryListener);

        if(!pinging.get()) {
            swipeRefreshLayout.setRefreshing(true);
            pinging.set(true);
            new SendPing().executeOnExecutor(StaticData.threadPool);
        }

        new InitializeData().executeOnExecutor(StaticData.threadPool);
        getLoaderManager().initLoader(StaticData.FRIENDS_LOADER, null, this);
        return rootView;
    }

    private class InitializeData extends AsyncTask<Void, String, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            if(getActivity() == null)
                return false;
            final Cursor phones = getActivity().getContentResolver().query(ContactsContract.
                    CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if(phones == null)
                return false;
            final HashSet<Contact> contacts = new HashSet<>(phones.getCount());
            while(phones.moveToNext()) {

                final Contact contact;
                final String number, displayName;
                final long userID;

                if(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) == -1 ||
                        phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) == -1 ||
                        phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID) == -1) continue;

                number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                displayName = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                userID = phones.getLong(phones.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID));

                if(TextUtils.isEmpty(displayName)) continue;
                contact = new Contact(displayName, number, userID);
                if(inviteSentTo.contains(contact.toString()))
                    contact.setInviteSent(true);
                contacts.add(contact);
            }
            phones.close();
            if(contacts.size() == 0 || getActivity() == null)
                return false;
            final ArrayList<Contact> contactArrayList = new ArrayList<>(contacts);
            Collections.sort(contactArrayList, new Comparator<Contact>() {
                @Override
                public int compare(Contact lhs, Contact rhs) {
                    return lhs.getUserName().compareToIgnoreCase(rhs.getUserName());
                }
            });
            adapter = new ReachAllContactsAdapter(getActivity(), R.layout.allcontacts_user, contactArrayList);
            adapter.setOnEmptyContactsListener(new ReachAllContactsAdapter.OnEmptyContactsListener() {
                @Override
                public void onEmptyContacts() {
                    mergeAdapter.setActive(emptyTV2,true);
                }

                @Override
                public void onNotEmptyContacts() {
                    mergeAdapter.setActive(emptyTV2,false);
                }
            });
            return true;
        }

        @Override
        protected void onPostExecute(Boolean gg) {
            super.onPostExecute(gg);

            if(isCancelled() || getActivity() == null || getActivity().isFinishing() || listView == null)
                return;

            if(gg) {
                mergeAdapter.setActive(emptyTV2,false);
                mergeAdapter.addAdapter(adapter);
                listView.setAdapter(mergeAdapter);
            } else {
                mergeAdapter.setActive(emptyTV2,true);
                MiscUtils.setEmptyTextforListView(listView,"No contacts found");
                Toast.makeText(getActivity(), "No contacts found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onClose() {

        searchView.setQuery(null, true);
        if (adapter!=null)
            adapter.getFilter().filter(null);
        selection = null;
        selectionArguments = null;
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if(searchView == null)
            return false;

        if (adapter!=null)
            adapter.getFilter().filter(newText);

        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) {
            return true;
        } if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;

        if(TextUtils.isEmpty(newText)) {
            selection = null;
            selectionArguments = null;
        } else {
            selection = ReachFriendsHelper.COLUMN_USER_NAME + " LIKE ?";
            selectionArguments = new String[]{"%" + mCurFilter + "%"};
        }
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
        return true;
    }

    private final class SendPing extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            StaticData.networkCache.clear();
            MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {
                    return StaticData.userEndpoint.pingMyReach(serverId).execute();
                }
            }, Optional.<Predicate<MyString>>absent()).orNull();
            if(isCancelled() || getActivity() == null || getActivity().isFinishing() || swipeRefreshLayout == null)
                return null;
            /**
             * Invalidate those who were online 30 secs ago
             * and send PING
             */
            final ContentValues contentValues = new ContentValues();
            contentValues.put(ReachFriendsHelper.COLUMN_LAST_SEEN, System.currentTimeMillis() + 31*1000);
            contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
            contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);
            getActivity().getContentResolver().update(
                    ReachFriendsProvider.CONTENT_URI,
                    contentValues,
                    ReachFriendsHelper.COLUMN_STATUS + " = ? and " +
                            ReachFriendsHelper.COLUMN_LAST_SEEN + " < ?",
                    new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "", (System.currentTimeMillis() - 30 * 1000) + ""});
            contentValues.clear();

            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pinging.set(false);
            if(isCancelled() || getActivity() == null || getActivity().isFinishing() || swipeRefreshLayout == null)
                return;
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private final class FindAndAddFriends extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            final HashSet<String> numbers = new HashSet<>();
            numbers.add("000000001");
            if(getActivity() == null) {
                refreshing.set(false);
                return 0;
            }
            final Cursor cursor = getActivity().getContentResolver().query(ContactsContract.
                    CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if(cursor != null) {

                while (cursor.moveToNext()) {

                    final int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (columnIndex == -1)
                        continue;
                    String phoneNumber = cursor.getString(columnIndex);
                    if (TextUtils.isEmpty(phoneNumber))
                        continue;
                    phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                    if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 10)
                        continue;
                    else if(phoneNumber.startsWith("91"))
                        phoneNumber = phoneNumber.substring(2);
                    else if(phoneNumber.startsWith("0"))
                        phoneNumber = phoneNumber.substring(1);
                    numbers.add(phoneNumber);
                }
                cursor.close();
            }

            final ContactsWrapper contactsWrapper = new ContactsWrapper();
            contactsWrapper.setContacts(ImmutableList.copyOf(numbers));

            final ReachFriendCollection dataToReturn = MiscUtils.autoRetry(new DoWork<ReachFriendCollection>() {
                @Override
                protected ReachFriendCollection doWork() throws IOException {
                    return StaticData.userEndpoint.returnUsersNew(serverId, contactsWrapper).execute();
                }
            }, Optional.<Predicate<ReachFriendCollection>>absent()).orNull();
            final List<ReachFriend> reachFriends;
            if(dataToReturn == null || (reachFriends = dataToReturn.getItems()) == null || reachFriends.size() == 0) {

                refreshing.set(false);
                return 0;
            }

            //add everything
            int i = 0;
            final ContentValues[] contentValues = new ContentValues[reachFriends.size()];
            final long currentTime = System.currentTimeMillis();

            for(ReachFriend reachFriend : reachFriends) {

                /**
                 * Actual last-seen
                 */
                reachFriend.setLastSeen(currentTime - reachFriend.getLastSeen());
                reachFriend.setNetworkType(Integer.parseInt(StaticData.networkCache.get(reachFriend.getId(), 0+"").trim()));
                contentValues[i++] = ReachFriendsHelper.contentValuesCreator(reachFriend);
            }
            if(getActivity() != null)
                Log.i("Ayush", "FRIENDS INSERTED " +
                        getActivity().getContentResolver().bulkInsert(ReachFriendsProvider.CONTENT_URI, contentValues));
            reachFriends.clear();
            for(ContentValues values : contentValues)
                values.clear();
            refreshing.set(false);
            return contentValues.length;
        }

        @Override
        protected void onPostExecute(Integer anInt) {
            super.onPostExecute(anInt);
            if (mergeAdapter!=null) {
                if (anInt == 0 && listView != null) {
                    mergeAdapter.setActive(emptyTV1, true);
                    MiscUtils.setEmptyTextforListView(listView, "No friends found");
                } else
                    mergeAdapter.setActive(emptyTV1, false);
            }
        }
    }

    private final class GetMusic implements Runnable {

        private final long hostId;
        private final SharedPreferences sharedPreferences;

        private GetMusic(long hostId, SharedPreferences sharedPreferences) {
            this.hostId = hostId;
            this.sharedPreferences = sharedPreferences;
        }

        @Override
        public void run() {

            //fetch music
            final MusicContainer musicContainer = MiscUtils.autoRetry(new DoWork<MusicContainer>() {
                @Override
                protected MusicContainer doWork() throws IOException {

                    return StaticData.userEndpoint.getMusicWrapper(
                            hostId,
                            serverId,
                            SharedPrefUtils.getPlayListCodeForUser(hostId, sharedPreferences),
                            SharedPrefUtils.getSongCodeForUser(hostId, sharedPreferences)).execute();
                }
            }, Optional.<Predicate<MusicContainer>>of(new Predicate<MusicContainer>() {
                @Override
                public boolean apply(@Nullable MusicContainer input) {
                    return input == null;
                }
            })).orNull();

            if(musicContainer == null && getActivity() != null)
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Music fetch failed", Toast.LENGTH_SHORT).show();
                    }
                });

            if(getActivity() == null || getActivity().isFinishing() || musicContainer == null)
                return;

            if(musicContainer.getSongsChanged()) {

                if(musicContainer.getReachSongs() == null || musicContainer.getReachSongs().size() == 0)
                    //All the songs got deleted
                    MiscUtils.deleteSongs(hostId, getActivity().getContentResolver());
                else {
                    final Pair<Collection<ReachAlbumDatabase>, Collection<ReachArtistDatabase>> pair =
                            MiscUtils.getAlbumsAndArtists(new HashSet<>(musicContainer.getReachSongs()));
                    final Collection<ReachAlbumDatabase> reachAlbumDatabases = pair.first;
                    final Collection<ReachArtistDatabase> reachArtistDatabases = pair.second;
                    MiscUtils.bulkInsertSongs(new HashSet<>(musicContainer.getReachSongs()),
                            reachAlbumDatabases,
                            reachArtistDatabases,
                            getActivity().getContentResolver());
                }
                SharedPrefUtils.storeSongCodeForUser(hostId, musicContainer.getSongsHash(), sharedPreferences.edit());
                Log.i("Ayush", "Fetching songs, song hash changed for " + hostId + " " + musicContainer.getSongsHash());
            }

            if(musicContainer.getPlayListsChanged() && getActivity() != null && !getActivity().isFinishing()) {

                if(musicContainer.getReachPlayLists() == null || musicContainer.getReachPlayLists().size() == 0)
                    //All playLists got deleted
                    MiscUtils.deletePlayLists(hostId, getActivity().getContentResolver());
                else
                    MiscUtils.bulkInsertPlayLists(new HashSet<>(musicContainer.getReachPlayLists()), getActivity().getContentResolver());
                SharedPrefUtils.storePlayListCodeForUser(hostId, musicContainer.getPlayListHash(), sharedPreferences.edit());
                Log.i("Ayush", "Fetching playLists, playList hash changed for " + hostId + " " + musicContainer.getPlayListHash());
            }
        }
    }

    public static class InfoDialog extends DialogFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            final View v = inflater.inflate(R.layout.import_dialog, container, false);
            final ImageView image = (ImageView) v.findViewById(R.id.image);

            Picasso.with(getActivity()).load(R.drawable.lock_pink).into(image);
            final int pad = MiscUtils.dpToPx(25);
            image.setPadding(pad,pad,pad,pad);
            final TextView text1 = (TextView) v.findViewById(R.id.text1);
            text1.setText("Use the lock icon to request access to files from friends");
            final TextView done = (TextView) v.findViewById(R.id.done);
            done.setText("Okay, I got it!");
            done.setVisibility(View.VISIBLE);
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            return v;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        if(menu == null || inflater == null)
            return;

        menu.clear();
        inflater.inflate(R.menu.search_menu, menu);
        /*final MenuItem notifButton = menu.findItem(R.id.notif_button);
        if(notifButton == null)
            return;
        notifButton.setActionView(R.layout.reach_queue_counter);

        final FrameLayout rqContainer = (FrameLayout)notifButton.getActionView().findViewById(R.id.counterContainer);
        if(rqContainer == null)
            return;

        rqContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.startInvitation();
            }
        });*/

        searchView = (SearchView) menu.findItem(R.id.search_button).getActionView();
        if(searchView == null)
            return;
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
    }

    /*@Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        final int id = item.getItemId();
        switch(id){

            case R.id.notif_button: {
                mListener.startInvitation();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}