package com.yaoseetech.identity;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.openid.appauth.AuthorizationServiceConfiguration;

/**
 * Created by xlongtang on 5/15/2017.
 */

public interface IIdentityProvider {

    String getName();

    boolean isEnabled();

    @Nullable
    Uri getDiscoveryEndpoint();

    @Nullable
    Uri getAuthEndpoint();

    @Nullable
    Uri getTokenEndpoint();

    @Nullable
    Uri getLogoutEndPoint();

    @NonNull
    String getTenant();

    String getClientId();

    void setClientId(String clientId);

    @NonNull
    Uri getRedirectUri();

    @NonNull
    String getPolicy();

    @NonNull
    String getScope();

    void retrieveConfig(Context context,
                        AuthorizationServiceConfiguration.RetrieveConfigurationCallback callback);
}
