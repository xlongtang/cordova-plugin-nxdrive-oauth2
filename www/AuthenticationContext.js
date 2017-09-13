// Copyright (c) Infobeyond Technologies.  All rights reserved.
// Licensed under the Apache License, Version 2.0.  See License.txt in the project root for license information.

/*global module, require*/

var checkArgs = require('cordova/argscheck').checkArgs;

var bridge = require('./CordovaBridge');
var Deferred = require('./utility').Utility.Deferred;
var AuthenticationResult = require('./AuthenticationResult');
var TokenCache = require('./TokenCache');

/**
 * Constructs context to use with known authority to get the token. It reuses existing context
 * for this authority URL in native proxy or creates a new one if it doesn't exist.
 * Corresponding native context will be created at first time when it will be needed.
 *
 * @param   {String}  clientId          App client id
 * @param   {String}  clientSecret      App client secret
 * @param   {String}  redirectUrl       App redirect url
 * @param   {String}  scope             App authorization scope
 * @returns {Object}  Newly created authentication context.
 */
function AuthenticationContext(clientId, clientSecret, redirectUrl, scope) {

    checkArgs('ssss', 'AuthenticationContext', arguments);

    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUrl = redirectUrl;
    this.scope = scope;
    this.tokenCache = new TokenCache(this);
}

/**
 * Acquires token using interactive flow, by sign in or sign up.
 * It always shows UI and skips token from cache.
 *
 * @param   {String}  userId   Optional login hint. This will determine if we are directed to sign in or sign up.
 *
 * @returns {Promise} Promise either fulfilled with AuthenticationResult object or rejected with error
 */
AuthenticationContext.prototype.acquireTokenAsync = function (userId) {

    checkArgs('S', 'AuthenticationContext.acquireTokenAsync', arguments);

    var d = new Deferred();

    bridge.executeNativeMethod('acquireTokenAsync', [
        this.clientId,
        this.clientSecret,
        this.redirectUrl,
        this.scope, userId
    ]).then(function(authResult){
        d.resolve(new AuthenticationResult(authResult));
    }, function(err) {
        d.reject(err);
    });

    return d;
};

/**
 * Logouts a user
 * @param {String} policy
 * @returns {Promise} 
 */
AuthenticationContext.prototype.logoutAsync = function () {
    var d = new Deferred();

    bridge.executeNativeMethod('logoutAsync', [
        this.clientId,
        this.clientSecret,
        this.redirectUrl,
        this.scope
    ]).then(function(res){
        d.resolve(res);
    }, function(err) {
        d.reject(err);
    });

    return d;
    
};


module.exports = AuthenticationContext;
