package eu.nioc.tumblrbrowse.utils;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1RequestToken;

/**
 * Extending Oauth1.0a API for Tumblr (including specific endpoints)
 */
public class OauthTumblrApi extends DefaultApi10a {
    private static final String AUTHORIZE_URL = "https://www.tumblr.com/oauth/authorize?oauth_token=%s";
    private static final String REQUEST_TOKEN_RESOURCE = "https://www.tumblr.com/oauth/request_token";
    private static final String ACCESS_TOKEN_RESOURCE = "https://www.tumblr.com/oauth/access_token";

    private OauthTumblrApi() {
    }

    private static class InstanceHolder {
        private static final OauthTumblrApi INSTANCE = new OauthTumblrApi();
    }

    public static OauthTumblrApi instance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_RESOURCE;
    }

    @Override
    public String getRequestTokenEndpoint() {
        return REQUEST_TOKEN_RESOURCE;
    }

    @Override
    public String getAuthorizationUrl(OAuth1RequestToken requestToken) {
        return String.format(AUTHORIZE_URL, requestToken.getToken());
    }
}
