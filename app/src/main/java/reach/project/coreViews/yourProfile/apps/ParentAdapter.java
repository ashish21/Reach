//package reach.project.coreViews.yourProfile.apps;
//
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
//import com.squareup.wire.Message;
//
//import javax.annotation.Nonnull;
//
//import reach.project.R;
//import reach.project.apps.App;
//import reach.project.coreViews.yourProfile.blobCache.CacheAdapterInterface;
//import reach.project.utils.ThreadLocalRandom;
//import reach.project.utils.viewHelpers.CustomGridLayoutManager;
//import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
//import reach.project.utils.viewHelpers.HandOverMessage;
//import reach.project.utils.viewHelpers.MoreListHolder;
//import reach.project.utils.viewHelpers.RecyclerViewMaterialAdapter;
//
//
///**
// * Created by dexter on 13/11/15.
// */
//class ParentAdapter<T extends Message> extends RecyclerViewMaterialAdapter<RecyclerView.ViewHolder> {
//
//    private static final byte APP_ITEM_TYPE = 1;
//    private static final byte RECENT_LIST_TYPE = 2;
//    private static final byte SMART_LIST_TYPE = 3;
//
//    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
//    private final long smartHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
//
//    private final CacheAdapterInterface<T, App> cacheAdapterInterface;
//    private final HandOverMessage<Integer> handOverMessage = new HandOverMessage<Integer>() {
//        @Override
//        public void handOverMessage(@Nonnull Integer position) {
//
//            if (position < 1)
//                throw new IllegalArgumentException(" Invalid position: " + (position-1)
//                        + " provided in HandOverMessage of YourProfile->Apps->ParentAdapter");
//
//            final T message = cacheAdapterInterface.getItem(position - 1);
//            if (message instanceof App)
//                cacheAdapterInterface.handOverMessage((App) message);
//            else
//                throw new IllegalArgumentException("App item holder passed on an illegal value type");
//        }
//    };
//
//    public ParentAdapter(CacheAdapterInterface<T, App> cacheAdapterInterface) {
//
//        this.cacheAdapterInterface = cacheAdapterInterface;
//        setHasStableIds(true);
//    }
//
//    @Override
//    protected void newBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//
//        final Message message = cacheAdapterInterface.getItem(position);
//        if (message instanceof App && holder instanceof AppItemHolder) {
//
//            final App app = (App) message;
//            final AppItemHolder appAppItemHolder = (AppItemHolder) holder;
//
//            appAppItemHolder.appName.setText(app.applicationName);
//            appAppItemHolder.extraButton.setVisibility(View.INVISIBLE);
//
//            final PackageManager packageManager = appAppItemHolder.appName.getContext().getPackageManager();
//
//            try {
//                appAppItemHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(app.packageName));
//            } catch (PackageManager.NameNotFoundException ignored) {
//                appAppItemHolder.appIcon.setImageURI(Uri.parse("http://52.74.53.245:8080/getImage/appLogo?packageName=" + app.packageName));
//            }
//
//        } else if (message instanceof RecentApps && holder instanceof MoreListHolder) {
//
//            final RecentApps recentApp = (RecentApps) message;
//            final MoreListHolder listHolder = (MoreListHolder) holder;
//            listHolder.headerText.setText(recentApp.title);
//            if (listHolder.listOfItems.getLayoutManager() == null)
//                listHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(listHolder.listOfItems.getContext(), 2));
//            listHolder.listOfItems.setAdapter(new MoreAdapter(recentApp.appList, cacheAdapterInterface, R.layout.app_grid_item));
//
//        } else if (message instanceof SmartApps && holder instanceof MoreListHolder) {
//
//            final SmartApps smartApp = (SmartApps) message;
//            final MoreListHolder listHolder = (MoreListHolder) holder;
//            listHolder.itemView.setBackgroundResource(R.drawable.border_shadow1);
//            listHolder.headerText.setText(smartApp.title);
//            if (listHolder.listOfItems.getLayoutManager() == null)
//                listHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(listHolder.listOfItems.getContext(), LinearLayoutManager.HORIZONTAL, false));
//            listHolder.listOfItems.setAdapter(new MoreAdapter(smartApp.appList, cacheAdapterInterface, R.layout.app_grid_item));
//        }
//    }
//
//    @Override
//    protected RecyclerView.ViewHolder newCreateViewHolder(ViewGroup parent, int viewType) {
//
//        switch (viewType) {
//
//            case APP_ITEM_TYPE:
//                return new AppItemHolder(LayoutInflater.from(parent.getContext())
//                        .inflate(R.layout.app_list_item, parent, false), handOverMessage);
//            case RECENT_LIST_TYPE:
//                return new MoreListHolder(parent,
//                        R.layout.list_with_more_button_padding, //Main resource id
//                        R.id.headerText, //id for header text
//                        R.id.listOfItems, //id for list (recycler view)
//                        R.id.moreButton,
//                        true); //id of more button
//            case SMART_LIST_TYPE:
//                return new MoreListHolder(parent,true);
//            default:
//                throw new IllegalArgumentException("Unknown view type found");
//        }
//    }
//
//    @Override
//    protected int newGetItemCount() {
//        return cacheAdapterInterface.getItemCount();
//    }
//
//    @Override
//    protected int newGetItemViewType(int position) {
//
//        final Message message = cacheAdapterInterface.getItem(position);
//        if (message instanceof App)
//            return APP_ITEM_TYPE;
//        else if (message instanceof RecentApps)
//            return RECENT_LIST_TYPE;
//        else if (message instanceof SmartApps)
//            return SMART_LIST_TYPE;
//        else
//            throw new IllegalArgumentException("Unknown message found in list");
//    }
//
//    @Override
//    protected long newGetItemId(int position) {
//        final Message message = cacheAdapterInterface.getItem(position);
//        if (message instanceof App)
//            return cacheAdapterInterface.getItem(position).hashCode();
//        else if (message instanceof RecentApps)
//            return recentHolderId;
//        else if (message instanceof SmartApps)
//            return smartHolderId;
//        else
//            throw new IllegalArgumentException("Unknown message found in list");
//    }
//
//    @Override
//    protected RecyclerView.ViewHolder inflatePlaceHolder(View view) {
//        return new RecyclerView.ViewHolder(view) {
//        };
//    }
//}
