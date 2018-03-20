package com.samsung.context.sdk.samsunganalytics;

public class Configuration {
    private int auidType = -1;
    private String deviceId;
    private boolean enableAutoDeviceId = false;
    private boolean enableFastReady = false;
    private boolean enableUseInAppLogging = false;
    private boolean isAlwaysRunningApp = false;
    private int networkTimeoutInMilliSeconds = 0;
    private String overrideIp;
    private int queueSize = 0;
    private int restrictedNetworkType = -1;
    private String trackingId;
    private boolean useAnonymizeIp = false;
    private UserAgreement userAgreement;
    private String userId;
    private String version;

    public Configuration disableAutoDeviceId() {
        this.enableAutoDeviceId = false;
        return this;
    }

    public Configuration enableAutoDeviceId() {
        this.enableAutoDeviceId = true;
        return this;
    }

    public Configuration enableFastReady(boolean z) {
        this.enableFastReady = z;
        return this;
    }

    public Configuration enableUseInAppLogging(UserAgreement userAgreement) {
        setUserAgreement(userAgreement);
        this.enableUseInAppLogging = true;
        return this;
    }

    public int getAuidType() {
        return this.auidType;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public int getNetworkTimeoutInMilliSeconds() {
        return this.networkTimeoutInMilliSeconds;
    }

    public String getOverrideIp() {
        return this.overrideIp;
    }

    public int getQueueSize() {
        return this.queueSize;
    }

    public int getRestrictedNetworkType() {
        return this.restrictedNetworkType;
    }

    public String getTrackingId() {
        return this.trackingId;
    }

    public UserAgreement getUserAgreement() {
        return this.userAgreement;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isAlwaysRunningApp() {
        return this.isAlwaysRunningApp;
    }

    public boolean isEnableAutoDeviceId() {
        return this.enableAutoDeviceId;
    }

    public boolean isEnableFastReady() {
        return this.enableFastReady;
    }

    public boolean isEnableUseInAppLogging() {
        return this.enableUseInAppLogging;
    }

    public boolean isUseAnonymizeIp() {
        return this.useAnonymizeIp;
    }

    public Configuration setAlwaysRunningApp(boolean z) {
        this.isAlwaysRunningApp = z;
        return this;
    }

    public void setAuidType(int i) {
        this.auidType = i;
    }

    public Configuration setDeviceId(String str) {
        this.deviceId = str;
        return this;
    }

    public Configuration setNetworkTimeoutInMilliSeconds(int i) {
        this.networkTimeoutInMilliSeconds = i;
        return this;
    }

    public Configuration setOverrideIp(String str) {
        this.overrideIp = str;
        return this;
    }

    public Configuration setQueueSize(int i) {
        this.queueSize = i;
        return this;
    }

    protected void setRestrictedNetworkType(int i) {
        this.restrictedNetworkType = i;
    }

    public Configuration setTrackingId(String str) {
        this.trackingId = str;
        return this;
    }

    public Configuration setUseAnonymizeIp(boolean z) {
        this.useAnonymizeIp = z;
        return this;
    }

    public Configuration setUserAgreement(UserAgreement userAgreement) {
        this.userAgreement = userAgreement;
        return this;
    }

    public Configuration setUserId(String str) {
        this.userId = str;
        return this;
    }

    public Configuration setVersion(String str) {
        this.version = str;
        return this;
    }
}
