package com.yaoseetech.identity;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import net.openid.appauth.AuthorizationServiceConfiguration;

/**
 * Created by xlongtang on 5/15/2017.
 */

public class DefaultIdentityProvider implements IIdentityProvider {
    /**
     * Value used to indicate that a configured property is not specified or required.
     */
    public static final int NOT_SPECIFIED = -1;

    @NonNull
    public final String name;

    @DrawableRes
    public final int buttonImageRes;

    @StringRes
    public final int buttonContentDescriptionRes;

    public final int buttonTextColorRes;

    @StringRes
    private final int mDiscoveryEndpointRes;

    @StringRes
    private final int mAuthEndpointRes;

    @StringRes
    private final int mTokenEndpointRes;

    @StringRes
    private final int mRegistrationEndpointRes;

    @StringRes
    private final int mLogoutEndpointRes;

    private boolean mConfigurationRead = false;
    private boolean mDiscoveryUriFormatted = false;
    private boolean mEnabled;
    private Uri mDiscoveryEndpoint;
    private Uri mAuthEndpoint;
    private Uri mTokenEndpoint;
    private Uri mRegistrationEndpoint;

    private boolean mLogoutUriFormatted = false;
    private Uri mLogoutEndPoint;

    private String mTenant;
    private String mClientId;
    private Uri mRedirectUri;
    private String mPolicy;
    private String mScope;

    public DefaultIdentityProvider(
            @NonNull String name,
            Boolean enabled,
            @StringRes int discoveryEndpointRes,
            @StringRes int authEndpointRes,
            @StringRes int tokenEndpointRes,
            @StringRes int registrationEndpointRes,
            @StringRes int logoutEndpointRes,

            @NonNull String tenant,
            @NonNull String clientId,
            @NonNull String redirectUri,
            @NonNull String policy,
            @NonNull String scope,

            @DrawableRes int buttonImageRes,
            @StringRes int buttonContentDescriptionRes,
            @ColorRes int buttonTextColorRes) {

        if (!isSpecified(discoveryEndpointRes)
                && !isSpecified(authEndpointRes)
                && !isSpecified(tokenEndpointRes)) {
            throw new IllegalArgumentException(
                    "the discovery endpoint or the auth and token endpoints must be specified");
        }

        this.name = name;
        this.mEnabled = enabled;
        this.mDiscoveryEndpointRes = NOT_SPECIFIED;
        this.mAuthEndpointRes = authEndpointRes;
        this.mTokenEndpointRes = tokenEndpointRes;
        this.mRegistrationEndpointRes = NOT_SPECIFIED;
        this.mLogoutEndpointRes = logoutEndpointRes;

        this.mTenant = tenant;
        this.mClientId = clientId;
        this.mRedirectUri = Uri.parse(redirectUri);
        this.mPolicy = policy;
        this.mScope = scope;

        this.buttonImageRes = checkSpecified(buttonImageRes, "buttonImageRes");
        this.buttonContentDescriptionRes =
                checkSpecified(buttonContentDescriptionRes, "buttonContentDescriptionRes");
        this.buttonTextColorRes = checkSpecified(buttonTextColorRes, "buttonTextColorRes");
    }

    /**
     * This must be called before any of the getters will function.
     */
    public void readConfiguration(Context context) {
        if (mConfigurationRead) {
            return;
        }

        Resources res = context.getResources();

        mDiscoveryEndpoint = isSpecified(mDiscoveryEndpointRes)
                ? getUriResource(res, mDiscoveryEndpointRes, "discoveryEndpointRes")
                : null;
        mAuthEndpoint = isSpecified(mAuthEndpointRes)
                ? getUriResource(res, mAuthEndpointRes, "authEndpointRes")
                : null;
        mTokenEndpoint = isSpecified(mTokenEndpointRes)
                ? getUriResource(res, mTokenEndpointRes, "tokenEndpointRes")
                : null;
        mRegistrationEndpoint = isSpecified(mRegistrationEndpointRes)
                ? getUriResource(res, mRegistrationEndpointRes, "registrationEndpointRes")
                : null;

        mLogoutEndPoint = isSpecified(mLogoutEndpointRes)
                ? getUriResource(res, mLogoutEndpointRes, "logoutEndpointRes")
                : null;

        mConfigurationRead = true;
    }

    private void checkConfigurationRead() {
        if (!mConfigurationRead) {
            throw new IllegalStateException("Configuration not read");
        }
    }

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    @Nullable
    public Uri getDiscoveryEndpoint() {
        checkConfigurationRead();
        if (mDiscoveryEndpoint != null && !mDiscoveryUriFormatted) {
            String test = mDiscoveryEndpoint.toString();
            String discoveryEndpointString = String.format(mDiscoveryEndpoint.toString(), getTenant(), getPolicy());
            mDiscoveryEndpoint = Uri.parse(discoveryEndpointString);
        }
        return mDiscoveryEndpoint;
    }

    @Nullable
    public Uri getAuthEndpoint() {
        checkConfigurationRead();
        return mAuthEndpoint;
    }

    @Nullable
    public Uri getTokenEndpoint() {
        checkConfigurationRead();
        return mTokenEndpoint;
    }

    @Nullable
    public Uri getLogoutEndPoint() {
        checkConfigurationRead();
        if (mLogoutEndPoint != null && !mLogoutUriFormatted) {
            String tmp = String.format(mLogoutEndPoint.toString(), getTenant(), getPolicy(), getRedirectUri());
            mLogoutEndPoint = Uri.parse(tmp);
        }
        return mLogoutEndPoint;
    }

    @NonNull
    public String getTenant() {
        return mTenant;
    }

    public String getClientId() {
        return mClientId;
    }


    public void setClientId(String clientId) {
        mClientId = clientId;
    }

    @NonNull
    public Uri getRedirectUri() {
        return mRedirectUri;
    }

    @NonNull
    public String getPolicy() {
        return mPolicy;
    }

    @NonNull
    public String getScope() {
        checkConfigurationRead();
        return mScope;
    }

    public void retrieveConfig(Context context,
                               AuthorizationServiceConfiguration.RetrieveConfigurationCallback callback) {
        readConfiguration(context);
        if (getDiscoveryEndpoint() != null) {
            AuthorizationServiceConfiguration.fetchFromUrl(mDiscoveryEndpoint, callback);
        } else {
            AuthorizationServiceConfiguration config =
                    new AuthorizationServiceConfiguration(mAuthEndpoint, mTokenEndpoint,
                            mRegistrationEndpoint);
            callback.onFetchConfigurationCompleted(config, null);
        }
    }

    private static boolean isSpecified(int value) {
        return value != NOT_SPECIFIED;
    }

    private static int checkSpecified(int value, String valueName) {
        if (value == NOT_SPECIFIED) {
            throw new IllegalArgumentException(valueName + " must be specified");
        }
        return value;
    }

    private static Uri getUriResource(Resources res, @StringRes int resId, String resName) {
        return Uri.parse(res.getString(resId));
    }
}


