package com.yaoseetech.identity;

import net.openid.appauth.AuthState;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class servers as a communication media between TokenActivity and
 * CordovaIdentityPlugin. Usually CordovaIdentityPlugin sets the
 * callbackContext and TokenActivity sends out error or success message.
 *
 * To ensure the correctness, we declare all methods in this class to be
 * thread safe.
 */

public class AuthenticationResult {

    // A public singleton
    public static AuthenticationResult Instance = new AuthenticationResult();

    /**
     * Private field that stores cordova callback context which is used to send results back to JS
     */
    private CallbackContext mCallbackContext;
    /**
     * Default constructor
     */
    private AuthenticationResult(){
    }

    public synchronized void setCallbackContext(CallbackContext callbackContext) {
        this.mCallbackContext = callbackContext;
    }

    private JSONObject serializeTokens(AuthState authResult) throws JSONException {
        JSONObject json = new JSONObject();
        if (authResult.getAccessToken() != null) {
            json.put("accessToken", authResult.getAccessToken());
        }
        if (authResult.getAccessTokenExpirationTime() != null) {
            json.put("expiresOn", authResult.getAccessTokenExpirationTime());
        }
        if (authResult.getIdToken() != null) {
            json.put("idToken", authResult.getIdToken());
        }
        return json;
    }

    public synchronized void sendSuccessResult(AuthState authResult) {
        try {
            JSONObject result = serializeTokens(authResult);
            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
        }
        catch (Exception ex) {
            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, ex.getMessage()));
        }
        finally {
            mCallbackContext = null;
        }
    }

    public synchronized void sendErrorResult() {
        try {
            JSONObject cordovaError = new JSONObject();
            cordovaError.put("errorDescription", "todo");
            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, cordovaError));
        }
        catch(JSONException ex){
            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, ex.getMessage()));
        }
        finally {
            mCallbackContext = null;
        }
    }
}
