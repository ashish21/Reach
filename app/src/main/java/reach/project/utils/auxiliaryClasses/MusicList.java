package reach.project.utils.auxiliaryClasses;

// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: /Users/ashish/Documents/proto/musiclist.proto
import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.util.Collections;
import java.util.List;

import static com.squareup.wire.Message.Datatype.INT64;
import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.REPEATED;

public final class MusicList extends Message {
  private static final long serialVersionUID = 0L;

  public static final Long DEFAULT_CLIENTID = 0L;
  public static final List<String> DEFAULT_GENRES = Collections.emptyList();
  public static final List<Song> DEFAULT_SONG = Collections.emptyList();
  public static final List<Playlist> DEFAULT_PLAYLIST = Collections.emptyList();

  @ProtoField(tag = 1, type = INT64)
  public final Long clientId;

  @ProtoField(tag = 2, type = STRING, label = REPEATED)
  public final List<String> genres;

  @ProtoField(tag = 3, label = REPEATED, messageType = Song.class)
  public final List<Song> song;

  @ProtoField(tag = 4, label = REPEATED, messageType = Playlist.class)
  public final List<Playlist> playlist;

  public MusicList(Long clientId, List<String> genres, List<Song> song, List<Playlist> playlist) {
    this.clientId = clientId;
    this.genres = immutableCopyOf(genres);
    this.song = immutableCopyOf(song);
    this.playlist = immutableCopyOf(playlist);
  }

  private MusicList(Builder builder) {
    this(builder.clientId, builder.genres, builder.song, builder.playlist);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof MusicList)) return false;
    MusicList o = (MusicList) other;
    return equals(clientId, o.clientId)
        && equals(genres, o.genres)
        && equals(song, o.song)
        && equals(playlist, o.playlist);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = clientId != null ? clientId.hashCode() : 0;
      result = result * 37 + (genres != null ? genres.hashCode() : 1);
      result = result * 37 + (song != null ? song.hashCode() : 1);
      result = result * 37 + (playlist != null ? playlist.hashCode() : 1);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<MusicList> {

    public Long clientId;
    public List<String> genres;
    public List<Song> song;
    public List<Playlist> playlist;

    public Builder() {
    }

    public Builder(MusicList message) {
      super(message);
      if (message == null) return;
      this.clientId = message.clientId;
      this.genres = copyOf(message.genres);
      this.song = copyOf(message.song);
      this.playlist = copyOf(message.playlist);
    }

    public Builder clientId(Long clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder genres(List<String> genres) {
      this.genres = checkForNulls(genres);
      return this;
    }

    public Builder song(List<Song> song) {
      this.song = checkForNulls(song);
      return this;
    }

    public Builder playlist(List<Playlist> playlist) {
      this.playlist = checkForNulls(playlist);
      return this;
    }

    @Override
    public MusicList build() {
      return new MusicList(this);
    }
  }
}
