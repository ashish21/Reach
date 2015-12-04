package reach.project.apps;// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto/app.proto
import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import static com.squareup.wire.Message.Datatype.BOOL;
import static com.squareup.wire.Message.Datatype.INT64;
import static com.squareup.wire.Message.Datatype.STRING;

public final class App extends Message {
    private static final long serialVersionUID = 0L;

    public static final Boolean DEFAULT_LAUNCHINTENTFOUND = false;
    public static final Boolean DEFAULT_VISIBLE = false;
    public static final String DEFAULT_APPLICATIONNAME = "hello_world";
    public static final String DEFAULT_DESCRIPTION = "hello_world";
    public static final String DEFAULT_PACKAGENAME = "hello_world";
    public static final String DEFAULT_PROCESSNAME = "hello_world";
    public static final Long DEFAULT_INSTALLDATE = 0L;

    @ProtoField(tag = 1, type = BOOL)
    public final Boolean launchIntentFound;

    @ProtoField(tag = 2, type = BOOL)
    public final Boolean visible;

    @ProtoField(tag = 3, type = STRING)
    public final String applicationName;

    @ProtoField(tag = 4, type = STRING)
    public final String description;

    @ProtoField(tag = 5, type = STRING)
    public final String packageName;

    @ProtoField(tag = 6, type = STRING)
    public final String processName;

    @ProtoField(tag = 7, type = INT64)
    public final Long installDate;

    public App(Boolean launchIntentFound, Boolean visible, String applicationName, String description, String packageName, String processName, Long installDate) {
        this.launchIntentFound = launchIntentFound;
        this.visible = visible;
        this.applicationName = applicationName;
        this.description = description;
        this.packageName = packageName;
        this.processName = processName;
        this.installDate = installDate;
    }

    private App(Builder builder) {
        this(builder.launchIntentFound, builder.visible, builder.applicationName, builder.description, builder.packageName, builder.processName, builder.installDate);
        setBuilder(builder);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof App)) return false;
        App o = (App) other;
        return equals(launchIntentFound, o.launchIntentFound)
                && equals(visible, o.visible)
                && equals(applicationName, o.applicationName)
                && equals(description, o.description)
                && equals(packageName, o.packageName)
                && equals(processName, o.processName)
                && equals(installDate, o.installDate);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = launchIntentFound != null ? launchIntentFound.hashCode() : 0;
            result = result * 37 + (visible != null ? visible.hashCode() : 0);
            result = result * 37 + (applicationName != null ? applicationName.hashCode() : 0);
            result = result * 37 + (description != null ? description.hashCode() : 0);
            result = result * 37 + (packageName != null ? packageName.hashCode() : 0);
            result = result * 37 + (processName != null ? processName.hashCode() : 0);
            result = result * 37 + (installDate != null ? installDate.hashCode() : 0);
            hashCode = result;
        }
        return result;
    }

    public static final class Builder extends Message.Builder<App> {

        public Boolean launchIntentFound;
        public Boolean visible;
        public String applicationName;
        public String description;
        public String packageName;
        public String processName;
        public Long installDate;

        public Builder() {
        }

        public Builder(App message) {
            super(message);
            if (message == null) return;
            this.launchIntentFound = message.launchIntentFound;
            this.visible = message.visible;
            this.applicationName = message.applicationName;
            this.description = message.description;
            this.packageName = message.packageName;
            this.processName = message.processName;
            this.installDate = message.installDate;
        }

        public Builder launchIntentFound(Boolean launchIntentFound) {
            this.launchIntentFound = launchIntentFound;
            return this;
        }

        public Builder visible(Boolean visible) {
            this.visible = visible;
            return this;
        }

        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder processName(String processName) {
            this.processName = processName;
            return this;
        }

        public Builder installDate(Long installDate) {
            this.installDate = installDate;
            return this;
        }

        @Override
        public App build() {
            return new App(this);
        }
    }
}