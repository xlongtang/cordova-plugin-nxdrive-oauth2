package com.infobeyondtech.identity;

import android.support.annotation.NonNull;

import com.yaoseetech.identity.DefaultIdentityProvider;
import com.yaoseetech.identity.IGlobalR;
import com.yaoseetech.identity.IIdentityProvider;

/**
 * Created by xlongtang on 5/15/2017.
 */

public class IdentityProviderFactory {

    public static final int NOT_SPECIFIED = -1;

    // Singleton
    public static IdentityProviderFactory Instance = new IdentityProviderFactory();

    // Preventing from any construction from outside.
    private IdentityProviderFactory() {
    }

    private static IIdentityProvider SignUpIn = null;

    synchronized IIdentityProvider getSignUpIn(@NonNull String clientId,
                                               @NonNull String redirectUri,
                                               @NonNull String scope,
                                               IGlobalR globalRStrings) {

        if (SignUpIn == null) {
            SignUpIn = new DefaultIdentityProvider(
                    "NXdrive Sign Up/In",
                    true,
                    DefaultIdentityProvider.NOT_SPECIFIED,
                    globalRStrings.getRStringId("nxdrive_authorize_uri"),
                    globalRStrings.getRStringId("nxdrive_token_uri"),
                    NOT_SPECIFIED, // dynamic registration not supported
                    globalRStrings.getRStringId("nxdrive_logout_uri"),
                    "", // on tenant
                    clientId,
                    redirectUri,
                    "",
                    scope,
                    0,
                    globalRStrings.getRStringId("nxdrive_name"),
                    android.R.color.white);
        }
        return SignUpIn;
    }
}
