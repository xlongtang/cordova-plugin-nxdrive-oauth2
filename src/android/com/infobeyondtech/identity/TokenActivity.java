package com.infobeyondtech.identity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.yaoseetech.identity.AuthenticationResult;
import com.yaoseetech.identity.DefaultGlobalR;
import com.yaoseetech.identity.IGlobalR;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretPost;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by xlongtang on 5/11/2017.
 */

public class TokenActivity extends AppCompatActivity {
    private static final String TAG = "TokenActivity";

    private static final String KEY_AUTH_STATE = "authState";
    private static final String KEY_USER_INFO = "userInfo";

    private static final String EXTRA_AUTH_SERVICE_DISCOVERY = "authServiceDiscovery";
    private static final String EXTRA_AUTH_STATE = "authState";

    private static final int BUFFER_SIZE = 1024;

    private AuthState mAuthState;
    private AuthorizationService mAuthService;
    private JSONObject mUserInfoJson;

    // onCreate will created
    private IGlobalR mGlobalRStrings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get resource string
        this.mGlobalRStrings = new DefaultGlobalR(this);
        setContentView(this.mGlobalRStrings.getRLayoutId("nxdrive_auth_token_activity"));

        mAuthService = new AuthorizationService(this);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_AUTH_STATE)) {
                try {
                    mAuthState = AuthState.jsonDeserialize(
                            savedInstanceState.getString(KEY_AUTH_STATE));
                } catch (JSONException ex) {
                    Log.e(TAG, "Malformed authorization JSON saved", ex);
                }
            }

            if (savedInstanceState.containsKey(KEY_USER_INFO)) {
                try {
                    mUserInfoJson = new JSONObject(savedInstanceState.getString(KEY_USER_INFO));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed to parse saved user info JSON", ex);
                }
            }
        }

        if (mAuthState == null) {
            mAuthState = getAuthStateFromIntent(getIntent());
            AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
            AuthorizationException ex = AuthorizationException.fromIntent(getIntent());
            mAuthState.update(response, ex);

            if (response != null) {
                Log.d(TAG, "Received AuthorizationResponse.");
                showSnackbar(mGlobalRStrings.getRStringId("nxdrive_exchange_notification"));
                exchangeAuthorizationCode(response);
            } else {
                Log.i(TAG, "Authorization failed: " + ex);
                showSnackbar(mGlobalRStrings.getRStringId("nxdrive_authorization_failed"));
            }
        }

        refreshUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (mAuthState != null) {
            state.putString(KEY_AUTH_STATE, mAuthState.jsonSerializeString());
        }

        if (mUserInfoJson != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuthService.dispose();
    }

    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        Log.d(TAG, "Token request complete");
        mAuthState.update(tokenResponse, authException);

        // If we get an access token, we refresh ui and return.
        if (mAuthState.isAuthorized() && mAuthState.getAccessToken() != null) {
            showSnackbar((tokenResponse != null)
                    ? mGlobalRStrings.getRStringId("nxdrive_exchange_complete")
                    : mGlobalRStrings.getRStringId("nxdrive_refresh_failed"));
            refreshUi();
            return;
        }
        // If we get refresh token, we request for an access token.
        if (mAuthState.getRefreshToken() != null) {
            showSnackbar(mGlobalRStrings.getRStringId("nxdrive_refresh_token"));
            refreshAccessToken();
            return;
        }
    }

    private void refreshUi() {

        final Button confirmButton =
                (Button) findViewById(this.mGlobalRStrings.getRId("id", "nxdrive_auth_confirm"));
        if (mAuthState.getRefreshToken() == null) {
            confirmButton.setVisibility(View.GONE);
            return;
        }
        // Otherwise, show the confirm button
        confirmButton.setVisibility(View.VISIBLE);
        // Depending on the authorization result, we handle the above confirm button differently.
        if (mAuthState != null && mAuthState.isAuthorized()) {
            // Start a timer and close
            final CountDownTimer timer = new CountDownTimer(10000, 1000) {

                public void onTick(long millisUntilFinished) {
                    confirmButton.setText(String.format("Close in %d seconds", millisUntilFinished / 1000));
                }

                public void onFinish() {
                    AuthenticationResult.Instance.sendSuccessResult(mAuthState);
                    finish();
                }
            }.start();

            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    timer.cancel();
                    AuthenticationResult.Instance.sendSuccessResult(mAuthState);
                    finish();
                }
            });
            return;
        }

        // Otherwise
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthenticationResult.Instance.sendErrorResult();
                finish();
            }
        });
    }

    private void refreshAccessToken() {
        performTokenRequest(mAuthState.createTokenRefreshRequest());
    }

    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(authorizationResponse.createTokenExchangeRequest());
    }

    private void performTokenRequest(TokenRequest request) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mAuthState.getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
            return;
        }

        clientAuthentication = new ClientSecretPost(mGlobalRStrings.getExtraString("client_secret"));

        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(
                            @Nullable TokenResponse tokenResponse,
                            @Nullable AuthorizationException ex) {
                        receivedTokenResponse(tokenResponse, ex);
                    }
                });
    }

    private void fetchUserInfo() {
        if (mAuthState.getAuthorizationServiceConfiguration() == null) {
            Log.e(TAG, "Cannot make userInfo request without service configuration");
        }

        mAuthState.performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
            @Override
            public void execute(String accessToken, String idToken, AuthorizationException ex) {
                if (ex != null) {
                    Log.e(TAG, "Token refresh failed when fetching user info");
                    return;
                }

                AuthorizationServiceDiscovery discoveryDoc = getDiscoveryDocFromIntent(getIntent());
                if (discoveryDoc == null) {
                    throw new IllegalStateException("no available discovery doc");
                }

                URL userInfoEndpoint;
                try {
                    userInfoEndpoint = new URL(discoveryDoc.getUserinfoEndpoint().toString());
                } catch (MalformedURLException urlEx) {
                    Log.e(TAG, "Failed to construct user info endpoint URL", urlEx);
                    return;
                }

                InputStream userInfoResponse = null;
                try {
                    HttpURLConnection conn = (HttpURLConnection) userInfoEndpoint.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                    conn.setInstanceFollowRedirects(false);
                    userInfoResponse = conn.getInputStream();
                    String response = readStream(userInfoResponse);
                    updateUserInfo(new JSONObject(response));
                } catch (IOException ioEx) {
                    Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
                } catch (JSONException jsonEx) {
                    Log.e(TAG, "Failed to parse userinfo response");
                } finally {
                    if (userInfoResponse != null) {
                        try {
                            userInfoResponse.close();
                        } catch (IOException ioEx) {
                            Log.e(TAG, "Failed to close userinfo response stream", ioEx);
                        }
                    }
                }
            }
        });
    }

    private void updateUserInfo(final JSONObject jsonObject) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mUserInfoJson = jsonObject;
                refreshUi();
            }
        });
    }

    @MainThread
    private void showSnackbar(int messageId) {
        Snackbar.make(findViewById(mGlobalRStrings.getRId("id", "coordinator")),
                getResources().getString(messageId),
                Snackbar.LENGTH_SHORT)
                .show();
    }

    private static String readStream(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder sb = new StringBuilder();
        int readCount;
        while ((readCount = br.read(buffer)) != -1) {
            sb.append(buffer, 0, readCount);
        }
        return sb.toString();
    }

    static PendingIntent createPostAuthorizationIntent(
            @NonNull Context context,
            @NonNull AuthorizationRequest request,
            @Nullable AuthorizationServiceDiscovery discoveryDoc,
            @NonNull AuthState authState) {
        Intent intent = new Intent(context, TokenActivity.class);
        intent.putExtra(EXTRA_AUTH_STATE, authState.jsonSerializeString());
        /*
        if (discoveryDoc != null) {
            intent.putExtra(EXTRA_AUTH_SERVICE_DISCOVERY, discoveryDoc.docJson.toString());
        } */

        return PendingIntent.getActivity(context, request.hashCode(), intent, 0);
    }

    static AuthorizationServiceDiscovery getDiscoveryDocFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_AUTH_SERVICE_DISCOVERY)) {
            return null;
        }
        String discoveryJson = intent.getStringExtra(EXTRA_AUTH_SERVICE_DISCOVERY);
        try {
            return new AuthorizationServiceDiscovery(new JSONObject(discoveryJson));
        } catch (JSONException ex) {
            throw new IllegalStateException("Malformed JSON in discovery doc");
        } catch (AuthorizationServiceDiscovery.MissingArgumentException  ex) {
            throw new IllegalStateException("Malformed JSON in discovery doc");
        }
    }

    static AuthState getAuthStateFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_AUTH_STATE)) {
            throw new IllegalArgumentException("The AuthState instance is missing in the intent.");
        }
        try {
            return AuthState.jsonDeserialize(intent.getStringExtra(EXTRA_AUTH_STATE));
        } catch (JSONException ex) {
            Log.e(TAG, "Malformed AuthState JSON saved", ex);
            throw new IllegalArgumentException("The AuthState instance is missing in the intent.");
        }
    }
}
