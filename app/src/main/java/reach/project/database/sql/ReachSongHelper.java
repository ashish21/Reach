package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.utils.auxiliaryClasses.Song;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachSongHelper extends SQLiteOpenHelper {

    public static final String SONG_TABLE = "songs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SONG_ID = "songId";
    public static final String COLUMN_FILE_HASH = "fileHash";
    public static final String COLUMN_DISPLAY_NAME = "displayName";
    public static final String COLUMN_ACTUAL_NAME = "actualName";
    public static final String COLUMN_GENRE = "genre";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_ARTIST = "artist";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_ALBUM = "album";
    public static final String COLUMN_FORMATTED_DATE_ADDED = "formattedDateAdded";

    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_DATE_ADDED = "dateAdded";
    public static final String COLUMN_VISIBILITY = "visibility";

    private static final String DATABASE_NAME = "reach.database.sql.ReachSongHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + SONG_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_FILE_HASH + " text" + "," +
            COLUMN_SONG_ID + " long" + "," +
            COLUMN_DISPLAY_NAME + " text" + "," +
            COLUMN_ACTUAL_NAME + " text" + "," +
            COLUMN_GENRE + " text" + "," +
            COLUMN_PATH + " text" + "," +
            COLUMN_ARTIST + " text" + "," +
            COLUMN_DURATION + " long" + "," +
            COLUMN_ALBUM + " text" + "," +
            COLUMN_FORMATTED_DATE_ADDED + " text" + "," +

            COLUMN_USER_ID + " long" + "," +
            COLUMN_SIZE + " long" + "," +
            COLUMN_YEAR + " long" + "," +
            COLUMN_DATE_ADDED + " long" + "," +
            COLUMN_VISIBILITY + " short" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,
                    COLUMN_FILE_HASH,
                    COLUMN_SONG_ID,
                    COLUMN_DISPLAY_NAME,
                    COLUMN_ACTUAL_NAME,
                    COLUMN_GENRE,
                    COLUMN_PATH,
                    COLUMN_ARTIST,
                    COLUMN_DURATION,
                    COLUMN_ALBUM,
                    COLUMN_FORMATTED_DATE_ADDED,

                    COLUMN_USER_ID,
                    COLUMN_SIZE,
                    COLUMN_YEAR,
                    COLUMN_DATE_ADDED,
                    COLUMN_VISIBILITY
            };

    public static ContentValues contentValuesCreator(Song song,
                                                     long serverId) {

        final ContentValues values = new ContentValues();

        values.put(COLUMN_SONG_ID, song.songId);
        values.put(COLUMN_FILE_HASH, "hello_world");
        values.put(COLUMN_DISPLAY_NAME, song.displayName);
        values.put(COLUMN_ACTUAL_NAME, song.actualName);
        values.put(COLUMN_GENRE, song.genre);
        values.put(COLUMN_PATH, song.path);
        values.put(COLUMN_ARTIST, song.artist);
        values.put(COLUMN_DURATION, song.duration);
        values.put(COLUMN_ALBUM, song.album);
        values.put(COLUMN_FORMATTED_DATE_ADDED, song.formattedDataAdded);
        values.put(COLUMN_USER_ID, serverId);
        values.put(COLUMN_SIZE, song.size);
        values.put(COLUMN_YEAR, song.year);
        values.put(COLUMN_DATE_ADDED, song.dateAdded);
        values.put(COLUMN_VISIBILITY, song.visibility);
        return values;
    }

    public static final String[] DISK_LIST = new String[]{ //count = 8
            ReachSongHelper.COLUMN_SONG_ID, //0
            ReachSongHelper.COLUMN_SIZE, //1
            ReachSongHelper.COLUMN_PATH, //2
            ReachSongHelper.COLUMN_DISPLAY_NAME, //3
            ReachSongHelper.COLUMN_ARTIST, //4
            ReachSongHelper.COLUMN_DURATION, //5
            ReachSongHelper.COLUMN_ALBUM, //6
            ReachSongHelper.COLUMN_ID //7 //useless
    };

    public static MusicData getMusicData(final Cursor cursor, final long serverId) {

        return new MusicData(
                cursor.getLong(0), //songId
                cursor.getLong(1), //length
                serverId, //senderId
                cursor.getLong(1), //processed = length
                cursor.getString(2), //path
                cursor.getString(3), //displayName
                cursor.getString(4), //artistName
                false, //liked
                cursor.getLong(5), //duration
                (byte) 1); //type
    }

    public ReachSongHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(ReachAlbumHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + SONG_TABLE);
        onCreate(db);
    }
}
