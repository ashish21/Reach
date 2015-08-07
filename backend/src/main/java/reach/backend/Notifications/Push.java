package reach.backend.Notifications;

import com.googlecode.objectify.annotation.Subclass;

import java.io.Serializable;

/**
 * Created by dexter on 06/07/15.
 */
@Subclass (name = "Push")
public class Push extends NotificationBase implements Serializable {

    private static final long serialVersionUID = 1L;

    private int size = 0;
    private String firstSongName = "";
    private String pushContainer = "";

    public String getPushContainer() {
        return pushContainer;
    }

    public void setPushContainer(String pushContainer) {
        this.pushContainer = pushContainer;
    }

    @Override
    public Types getTypes() {
        return super.getTypes();
    }

    @Override
    public void setTypes(Types types) { //fix ?
        if (types != Types.PUSH)
            throw new IllegalStateException("Illegal type");
        super.setTypes(types);
    }

    @Override
    public String getImageId() {
        return super.getImageId();
    }

    @Override
    public void setImageId(String imageId) {
        super.setImageId(imageId);
    }

    @Override
    public String getHostName() {
        return super.getHostName();
    }

    @Override
    public void setHostName(String hostName) {
        super.setHostName(hostName);
    }

    @Override
    public long getSystemTime() {
        return super.getSystemTime();
    }

    @Override
    public void setSystemTime(long systemTime) {
        super.setSystemTime(systemTime);
    }

    @Override
    public long getHostId() {
        return super.getHostId();
    }

    @Override
    public void setHostId(long hostId) {
        super.setHostId(hostId);
    }

    @Override
    public int getNotificationId() {
        return super.getNotificationId();
    }

    @Override
    public void setNotificationId(int notificationId) {
        super.setNotificationId(notificationId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Push)) return false;
        if (!super.equals(o)) return false;

        Push push = (Push) o;

        return !(pushContainer != null ? !pushContainer.equals(push.pushContainer) : push.pushContainer != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pushContainer != null ? pushContainer.hashCode() : 0);
        return result;
    }

    @Override
    public int getRead() {
        return super.getRead();
    }

    @Override
    public void setRead(int read) {
        super.setRead(read);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getFirstSongName() {
        return firstSongName;
    }

    public void setFirstSongName(String firstSongName) {
        this.firstSongName = firstSongName;
    }
}
