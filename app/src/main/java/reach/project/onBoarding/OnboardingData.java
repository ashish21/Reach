package reach.project.onBoarding;// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto/OnboardingData.proto

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import java.util.Collections;
import java.util.List;

import reach.project.utils.KeyValuePair;

import static com.squareup.wire.Message.Datatype.INT32;
import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.REPEATED;

public final class OnboardingData extends Message {
  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_VERSION = 0;
  public static final String DEFAULT_DEVICEID = "hello_world";
  public static final String DEFAULT_USERNAME = "hello_world";
  public static final String DEFAULT_PHONENUMBER = "hello_world";
  public static final String DEFAULT_PROFILEPICURI = "hello_world";
  public static final String DEFAULT_COVERPICURI = "hello_world";
  public static final String DEFAULT_EMAILID = "hello_world";
  public static final String DEFAULT_PROMOCODE = "hello_world";
  public static final List<KeyValuePair> DEFAULT_UTMPAIRS = Collections.emptyList();

  @ProtoField(tag = 1, type = INT32)
  public final Integer version;

  @ProtoField(tag = 2, type = STRING)
  public final String deviceId;

  @ProtoField(tag = 3, type = STRING)
  public final String userName;

  @ProtoField(tag = 4, type = STRING)
  public final String phoneNumber;

  @ProtoField(tag = 5, type = STRING)
  public final String profilePicUri;

  @ProtoField(tag = 6, type = STRING)
  public final String coverPicUri;

  @ProtoField(tag = 7, type = STRING)
  public final String emailId;

  @ProtoField(tag = 8, type = STRING)
  public final String promoCode;

  @ProtoField(tag = 9, label = REPEATED, messageType = KeyValuePair.class)
  public final List<KeyValuePair> utmPairs;

  public OnboardingData(Integer version, String deviceId, String userName, String phoneNumber, String profilePicUri, String coverPicUri, String emailId, String promoCode, List<KeyValuePair> utmPairs) {
    this.version = version;
    this.deviceId = deviceId;
    this.userName = userName;
    this.phoneNumber = phoneNumber;
    this.profilePicUri = profilePicUri;
    this.coverPicUri = coverPicUri;
    this.emailId = emailId;
    this.promoCode = promoCode;
    this.utmPairs = immutableCopyOf(utmPairs);
  }

  private OnboardingData(Builder builder) {
    this(builder.version, builder.deviceId, builder.userName, builder.phoneNumber, builder.profilePicUri, builder.coverPicUri, builder.emailId, builder.promoCode, builder.utmPairs);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OnboardingData)) return false;
    OnboardingData o = (OnboardingData) other;
    return equals(version, o.version)
        && equals(deviceId, o.deviceId)
        && equals(userName, o.userName)
        && equals(phoneNumber, o.phoneNumber)
        && equals(profilePicUri, o.profilePicUri)
        && equals(coverPicUri, o.coverPicUri)
        && equals(emailId, o.emailId)
        && equals(promoCode, o.promoCode)
        && equals(utmPairs, o.utmPairs);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = version != null ? version.hashCode() : 0;
      result = result * 37 + (deviceId != null ? deviceId.hashCode() : 0);
      result = result * 37 + (userName != null ? userName.hashCode() : 0);
      result = result * 37 + (phoneNumber != null ? phoneNumber.hashCode() : 0);
      result = result * 37 + (profilePicUri != null ? profilePicUri.hashCode() : 0);
      result = result * 37 + (coverPicUri != null ? coverPicUri.hashCode() : 0);
      result = result * 37 + (emailId != null ? emailId.hashCode() : 0);
      result = result * 37 + (promoCode != null ? promoCode.hashCode() : 0);
      result = result * 37 + (utmPairs != null ? utmPairs.hashCode() : 1);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<OnboardingData> {

    public Integer version;
    public String deviceId;
    public String userName;
    public String phoneNumber;
    public String profilePicUri;
    public String coverPicUri;
    public String emailId;
    public String promoCode;
    public List<KeyValuePair> utmPairs;

    public Builder() {
    }

    public Builder(OnboardingData message) {
      super(message);
      if (message == null) return;
      this.version = message.version;
      this.deviceId = message.deviceId;
      this.userName = message.userName;
      this.phoneNumber = message.phoneNumber;
      this.profilePicUri = message.profilePicUri;
      this.coverPicUri = message.coverPicUri;
      this.emailId = message.emailId;
      this.promoCode = message.promoCode;
      this.utmPairs = copyOf(message.utmPairs);
    }

    public Builder version(Integer version) {
      this.version = version;
      return this;
    }

    public Builder deviceId(String deviceId) {
      this.deviceId = deviceId;
      return this;
    }

    public Builder userName(String userName) {
      this.userName = userName;
      return this;
    }

    public Builder phoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
    }

    public Builder profilePicUri(String profilePicUri) {
      this.profilePicUri = profilePicUri;
      return this;
    }

    public Builder coverPicUri(String coverPicUri) {
      this.coverPicUri = coverPicUri;
      return this;
    }

    public Builder emailId(String emailId) {
      this.emailId = emailId;
      return this;
    }

    public Builder promoCode(String promoCode) {
      this.promoCode = promoCode;
      return this;
    }

    public Builder utmPairs(List<KeyValuePair> utmPairs) {
      this.utmPairs = checkForNulls(utmPairs);
      return this;
    }

    @Override
    public OnboardingData build() {
      return new OnboardingData(this);
    }
  }
}
