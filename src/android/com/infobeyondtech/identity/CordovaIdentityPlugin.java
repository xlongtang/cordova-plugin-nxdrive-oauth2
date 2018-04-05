package com.infobeyondtech.identity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yaoseetech.identity.AuthenticationResult;
import com.yaoseetech.identity.DefaultGlobalR;
import com.yaoseetech.identity.IGlobalR;
import com.yaoseetech.identity.IIdentityProvider;
import com.infobeyondtech.identity.IdentityProviderFactory;
import com.yaoseetech.identity.LogoutService;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.RegistrationRequest;
import net.openid.appauth.RegistrationResponse;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.browser.BrowserDescriptor;
import net.openid.appauth.browser.ExactBrowserMatcher;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;


/**
 * Created by xlongtang on 5/11/2017.
 */

public class CordovaIdentityPlugin extends CordovaPlugin {

    // Members that are initialized once and destroyed.
    private AuthorizationService mAuthService;
    private IGlobalR mGlobalRStrings;
    // Callback context is a shared member among methods
    private CallbackContext callbackContext;

    private static final String LOG_TAG = "NXdrive.OAuth2";

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        // mGlobalRString may escape from this class.
        // Therefore, its release will be handled by the garbage collector.
        this.mGlobalRStrings = new DefaultGlobalR(cordova.getActivity());
    }

    @Override
    public void onDestroy() {
        // Release resources
        if (this.mAuthService != null) {
            this.mAuthService.dispose();
            // Detach to help release
            this.mAuthService = null;
        }
        this.mGlobalRStrings = null;
        super.onDestroy();
    }

    @Override
    public boolean execute(final String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        // TODO: Do we need to set the result callback for every call.
        this.cordova.setActivityResultCallback(this);

        this.callbackContext = callbackContext;

        if (!action.equals("logoutAsync") && !action.equals("acquireTokenAsync")) {
            return false;
        }
        // Create a fresh one each time
        this.mAuthService = new AuthorizationService(this.getContext());

        // Set up callbackContext in the shared authentication result.
        // Note that we must do so every time and it must be thread safe.
        // TODO: Is there any resource leaking???? When shall we set the callbackcontext to be null.
        AuthenticationResult.Instance.setCallbackContext(callbackContext);

        String clientId = args.getString(0);
        String clientSecret = args.getString(1);
        String redirectUrl = args.getString(2);
        String scope = args.getString(4);
        // UserId is an optional argument
        final String userId = args.length() > 4 ? args.getString(4) : "";

        // Setup client secret
        this.mGlobalRStrings.setExtraString("client_secret", clientSecret);
        final IIdentityProvider idp =
                IdentityProviderFactory.Instance.getSignUpIn(clientId,
                        redirectUrl, scope, this.mGlobalRStrings);

        final AuthorizationServiceConfiguration.RetrieveConfigurationCallback retrieveCallback =
                new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {

                    @Override
                    public void onFetchConfigurationCompleted(
                            @Nullable AuthorizationServiceConfiguration serviceConfiguration,
                            @Nullable AuthorizationException ex) {
                        if (ex != null) {
                            Log.w(LOG_TAG, "Failed to retrieve configuration for " + idp.getName(), ex);
                            // Send out plugin
                            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, ex.getMessage()));
                        } else {
                            Log.d(LOG_TAG, "configuration retrieved for " + idp.getName()
                                    + ", proceeding");

                            if (action.equals("logoutAsync")) {
                                makeLogoutRequest(idp);
                            } else if (idp.getClientId() == null) {
                                // Do dynamic client registration if no client_id
                                makeRegistrationRequest(serviceConfiguration, idp);
                            } else {
                                makeAuthRequest(serviceConfiguration, idp, new AuthState(), userId);
                            }
                        }
                    }
                };

        idp.retrieveConfig(this.getContext(), retrieveCallback);
        return true;
    }


    private AppAuthConfiguration createConfiguration(
            @Nullable BrowserDescriptor browser) {
        AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();

        if (browser != null) {
            builder.setBrowserMatcher(new ExactBrowserMatcher(browser));
        }

        return builder.build();
    }

    /**
     * A convenient method for retrieving application context.
     *
     * @return
     */
    private Context getContext() {
        return this.cordova.getActivity().getApplicationContext();
    }

    private void makeAuthRequest(
            @NonNull AuthorizationServiceConfiguration serviceConfig,
            @NonNull IIdentityProvider idp,
            @NonNull AuthState authState,
            String loginHint) {

        if (loginHint.isEmpty()) {
            loginHint = null;
        }

        AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
                serviceConfig,
                idp.getClientId(),
                ResponseTypeValues.CODE,
                idp.getRedirectUri())
                .setScope(idp.getScope())
                .setLoginHint(loginHint)
                .build();

        Log.d(LOG_TAG, "Making auth request to " + serviceConfig.authorizationEndpoint);

        mAuthService.performAuthorizationRequest(
                authRequest,
                TokenActivity.createPostAuthorizationIntent(
                        this.getContext(),
                        authRequest,
                        serviceConfig.discoveryDoc,
                        authState),
                mAuthService.createCustomTabsIntentBuilder()
                        // .setToolbarColor(getColorCompat(R.color.colorAccent))
                        .build());
    }

    private void makeRegistrationRequest(
            @NonNull AuthorizationServiceConfiguration serviceConfig,
            @NonNull final IIdentityProvider idp) {

        final RegistrationRequest registrationRequest = new RegistrationRequest.Builder(
                serviceConfig,
                Arrays.asList(idp.getRedirectUri()))
                .setTokenEndpointAuthenticationMethod(ClientSecretBasic.NAME)
                .build();

        Log.d(LOG_TAG, "Making registration request to " + serviceConfig.registrationEndpoint);

        mAuthService.performRegistrationRequest(
                registrationRequest,
                new AuthorizationService.RegistrationResponseCallback() {
                    @Override
                    public void onRegistrationRequestCompleted(
                            @Nullable RegistrationResponse registrationResponse,
                            @Nullable AuthorizationException ex) {
                        Log.d(LOG_TAG, "Registration request complete");
                        if (registrationResponse != null) {
                            idp.setClientId(registrationResponse.clientId);
                            Log.d(LOG_TAG, "Registration request complete successfully");
                            // Continue with the authentication
                            makeAuthRequest(registrationResponse.request.configuration, idp,
                                    new AuthState((registrationResponse)), null);
                        }
                    }
                });
    }

    private void makeLogoutRequest(@NonNull final IIdentityProvider idp) {
        LogoutService service = new LogoutService(this.getContext());
        final LogoutService.LogoutRequest request =
                service.new LogoutRequest(idp.getLogoutEndPoint(), idp.getClientId());

        service.performLogoutRequest(this.mAuthService, request);
        // Send back result ...
        // Though the moment we send back any result does not gurantee that we
        // have successfully logged out.
        this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
    }
}
