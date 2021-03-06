//package reach.project.player;
//
//import android.database.Cursor;
//import android.database.CursorIndexOutOfBoundsException;
//import android.net.Uri;
//import android.support.annotation.Nullable;
//import android.support.v7.widget.RecyclerView;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.ViewGroup;
//
//import com.facebook.drawee.backends.pipeline.Fresco;
//import com.facebook.drawee.interfaces.DraweeController;
//import com.facebook.imagepipeline.common.ResizeOptions;
//import com.facebook.imagepipeline.request.ImageRequest;
//import com.facebook.imagepipeline.request.ImageRequestBuilder;
//import com.google.common.base.Optional;
//
//import java.io.Closeable;
//
//import javax.annotation.Nonnull;
//
//import reach.project.R;
//import reach.project.utils.AlbumArtUri;
//import reach.project.utils.MiscUtils;
//import reach.project.utils.viewHelpers.HandOverMessage;
//
///**
// * Created by dexter on 25/11/15.
// */
//
////TODO: To distinguish between downloaded songs and my library songs, cursor count is used, what if both cursor counts are the same
////To distinguish between them, two different type of cursor subclasses should be used instead
//class MusicListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {
//
//    //public static final byte VIEW_TYPE_RECENT = 0;
//    public static final byte VIEW_TYPE_ALL = 1;
//    private final HandOverMessage<Cursor> handOverCursor;
//    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
//    private static final String TAG = MusicListAdapter.class.getSimpleName();
//
//    private final HandOverMessage<Integer> handOverMessage = new HandOverMessage<Integer>() {
//
//        @Override
//        public void handOverMessage(@Nonnull Integer position) {
//
//            final Object object = getItem(position);
//            if (object instanceof Cursor) {
//
//                Cursor cursor = (Cursor) object;
//                handOverCursor.handOverMessage(cursor);
//
//                //if (cursor.getColumnCount() == SongHelper.MUSIC_DATA_LIST.length) {
//                currentlyPlayingSongId = cursor.getString(2);
//                notifyItemChanged(currentlyPlayingSongPosition);
//                notifyItemChanged(position);
///*
//                } else if (cursor.getColumnCount() == MySongsHelper.DISK_LIST.length) {
//                    currentlyPlayingSongId = cursor.getString(2);
//                    notifyItemChanged(currentlyPlayingSongPosition);
//                    notifyItemChanged(position);
//
//                } else
//                    throw new IllegalArgumentException("Unknown cursor type found");*/
//            } else
//                throw new IllegalStateException("Position must correspond with a cursor");
//        }
//    };
//
//    private String currentlyPlayingSongId;
//    public int currentlyPlayingSongPosition;
//
//    public MusicListAdapter(HandOverMessage<Cursor> handOverCursor,
//                            String currentlyPlayingSongId) {
//
//        this.handOverCursor = handOverCursor;
//        this.currentlyPlayingSongId = currentlyPlayingSongId;
//        setHasStableIds(true);
//    }
//
//    public void setCurrentlyPlayingSongId(String id) {
//
//        this.currentlyPlayingSongId = id;
//    }
//
//    public String getCurrentlyPlayingSongId() {
//        return currentlyPlayingSongId;
//    }
//
//    ///////////Data set ops
//    @Nullable
//    private Cursor myLibraryCursor = null;
//
//    public int myLibraryCount = 0;
//
//    public void setNewMyLibraryCursor(@Nullable Cursor newMyLibraryCursor) {
//
//        //destroy
//        if (this.myLibraryCursor != null)
//            myLibraryCursor.close();
//
//        //set
//        this.myLibraryCursor = newMyLibraryCursor;
//        //notifyDataSetChanged();
//    }
//
//    @Override
//    public void close() {
//
//        MiscUtils.closeQuietly(myLibraryCursor);
//        myLibraryCursor = null;
//    }
//    ///////////
//
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//
//        Log.i("Ayush", "Creating ViewHolder " + getClass().getName());
//
//        if (viewType == VIEW_TYPE_ALL)
//
//            return new SongItemHolder(LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.music_list_song_item, parent, false), handOverMessage);
//
//        throw new IllegalStateException("Unknown view type " + viewType);
//    }
//
//    @Override
//    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//
//        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());
//
//        final Object friend = getItem(position);
//        //TODO: avoid CursorIndexOutOfBoundsException
//        try {
//            //if (friend instanceof Cursor) {
//            holder.itemView.setSelected(false);
//            final Cursor cursorExactType = (Cursor) friend;
//            final SongItemHolder songItemHolder = (SongItemHolder) holder;
//            //holder.itemView.setBackgroundResource(0);
//            /*holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),R.color.));*/
//            final String displayName, artist, album, actualName;
//                /*if (cursorExactType.getColumnCount() == SongHelper.MUSIC_DATA_LIST.length) {*/
//
//            displayName = cursorExactType.getString(3);
//            artist = cursorExactType.getString(5);
//            album = cursorExactType.getString(6);
//            //Log.i("MusicListAdapter","downloaded song unique id = " + cursorExactType.getLong(20) +
//            //        " downloaded song id = " + cursorExactType.getLong(14) +
//            //        " currently playing song id = " + currentlyPlayingSongId );
//
//            //MetaHash is a String with cursor index of 2. Checkout SongHelper.Projection
//            if (cursorExactType.getString(2).equals(currentlyPlayingSongId)) {
//                //   Log.i("MusicListAdapter","Currently playing song is downloaded song");
//                holder.itemView.setSelected(true);
//                currentlyPlayingSongPosition = position;
//            }
//            ;
////                actualName = cursorExactType.getString(17);
//                /*} else if (cursorExactType.getColumnCount() == MySongsHelper.DISK_LIST.length) {
//                    displayName = cursorExactType.getString(3);
//                    artist = cursorExactType.getString(4);
//                    album = cursorExactType.getString(6);
//                    final long x = cursorExactType.getLong(0);
//                    if (cursorExactType.getString(2) == currentlyPlayingSongId) {
//                        //   Log.i("MusicListAdapter","Currently playing song is my library song");
//                        holder.itemView.setSelected(true);
//                        currentlyPlayingSongPosition = position;
//                    }
//                    ;
////                actualName = cursorExactType.getString(9);
//                } else
//                    throw new IllegalArgumentException("Unknown cursor type found");*/
//
//            songItemHolder.songName.setText(displayName);
//            songItemHolder.artistName.setText(artist);
//
//            final Optional<Uri> uriOptional = AlbumArtUri.getUri(album, artist, displayName, false);
//
//            if (uriOptional.isPresent()) {
//
////            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
//                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
//                        .setResizeOptions(resizeOptions)
//                        .build();
//
//                final DraweeController controller = Fresco.newDraweeControllerBuilder()
//                        .setOldController(songItemHolder.albumArt.getController())
//                        .setImageRequest(request)
//                        .build();
//
//                songItemHolder.albumArt.setController(controller);
//            } else
//                songItemHolder.albumArt.setImageBitmap(null);
//
//            //use
//            //}
//        } catch (CursorIndexOutOfBoundsException e) {
//            Log.e(TAG, "Exception thrown at song id = " + currentlyPlayingSongId + " and position = " + position);
//        }
//    }
//
//    /**
//     * Will either return Cursor object OR flag for recent list
//     *
//     * @param position position to load
//     * @return object
//     */
//    @Nonnull
//    public Cursor getItem(int position) {
//
//        //account for recent shit
//        if (myLibraryCursor == null || myLibraryCursor.isClosed() || !myLibraryCursor.moveToPosition(position))
//            throw new IllegalStateException("Resource cursor has been corrupted");
//        return myLibraryCursor;
//    }
//
//    @Override
//    public int getItemViewType(int position) {
//        return VIEW_TYPE_ALL;
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return getItem(position).getString(2).hashCode(); //_id || song_id
//    }
//
//    @Override
//    public int getItemCount() {
//
//        if (myLibraryCursor != null && !myLibraryCursor.isClosed())
//            myLibraryCount = myLibraryCursor.getCount();
//        else
//            myLibraryCount = 0;
//
//        return myLibraryCount;
//
//    }
//}
