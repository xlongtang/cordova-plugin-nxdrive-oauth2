package com.yaoseetech.identity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.browser.BrowserDescriptor;
import net.openid.appauth.browser.BrowserSelector;

/**
 * Created by xlongtang on 5/31/2017.
 */

public class LogoutService {

    private final Context mContext;

    @NonNull
    private final AppAuthConfiguration mClientConfiguration;

    @Nullable
    private final BrowserDescriptor mBrowser;


    // ctor
    public LogoutService(Context context) {
        mContext = context;
        mClientConfiguration = AppAuthConfiguration.DEFAULT;
        mBrowser = BrowserSelector.select(
                context,
                mClientConfiguration.getBrowserMatcher());
    }

    public class LogoutRequest  {
        public final Uri endPoint;
        public final String clientId;

        public LogoutRequest(Uri endPoint, String clientId) {
            this.endPoint = endPoint;
            this.clientId = clientId;
        }
    }

    public class LogoutResponse {
    }

    public void performLogoutRequest(AuthorizationService authService,
                                      LogoutRequest request) {
        Intent intent;
        if (mBrowser.useCustomTab) {
            intent =  authService.createCustomTabsIntentBuilder().build().intent;
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
        }
        intent.setPackage(mBrowser.packageName);
        intent.setData(request.endPoint);

        intent.putExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.NO_TITLE);

        mContext.startActivity(intent);
    }
}
