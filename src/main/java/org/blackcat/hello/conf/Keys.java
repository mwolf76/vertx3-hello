package org.blackcat.hello.conf;

public class Keys {
    /* SERVER */
    static String SERVER_SECTION = "server";

    static String SERVER_HTTP_PORT = "port";
    static int DEFAULT_SERVER_HTTP_PORT = 8080;

    static String SERVER_USE_SSL = "useSSL";
    static boolean DEFAULT_SERVER_USE_SSL = false;

    static String SERVER_KEYSTORE_FILENAME = "keystoreFilename";
    static String DEFAULT_SERVER_KEYSTORE_FILENAME = "server-keystore.jks";

    static String SERVER_KEYSTORE_PASSWORD = "keystorePassword";
    static String DEFAULT_SERVER_KEYSTORE_PASSWORD = "password";

    /* OAUTH2 */
    static String OAUTH2_SECTION = "oauth2";

    static String OAUTH2_PROVIDER = "provider";
    static String OAUTH2_PROVIDER_GOOGLE = "google";
    // TODO: add more providers

    static String OAUTH2_CLIENT_ID = "clientID";
    static String OAUTH2_CLIENT_SECRET = "clientSecret";

    static String OAUTH2_DOMAIN = "domain";

    private Keys()
    {}
}
