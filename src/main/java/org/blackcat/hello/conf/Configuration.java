package org.blackcat.hello.conf;

import io.vertx.core.json.JsonObject;
import org.blackcat.hello.conf.exceptions.ConfigurationException;

import java.text.MessageFormat;

import static org.blackcat.hello.conf.Keys.*;

/**
 * Configuration parser
 */
final public class Configuration {

    /* Configuration data */
    private int httpPort;
    private boolean useSSL;
    private String keystoreFilename;
    private String keystorePassword;

    private String oauth2Provider;
    private String oauth2ClientID;

    private String oauth2ClientSecret;
    private String oauth2Domain;

    public int getHttpPort() {
        return httpPort;
    }

    public boolean sslEnabled() {
        return useSSL;
    }

    public String getKeystoreFilename() {
        return keystoreFilename;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    void parseServerSection(JsonObject jsonObject) {
        final JsonObject serverSection = jsonObject.getJsonObject(SERVER_SECTION);

        this.httpPort = serverSection.getInteger(SERVER_HTTP_PORT, DEFAULT_SERVER_HTTP_PORT);
        this.useSSL = serverSection.getBoolean(SERVER_USE_SSL, DEFAULT_SERVER_USE_SSL);
        if (useSSL) {
            this.keystoreFilename = serverSection.getString(SERVER_KEYSTORE_FILENAME, DEFAULT_SERVER_KEYSTORE_FILENAME);
            this.keystorePassword = serverSection.getString(SERVER_KEYSTORE_PASSWORD, DEFAULT_SERVER_KEYSTORE_PASSWORD);
        }
    }

    public String getOauth2Provider() {
        return oauth2Provider;
    }

    public String getOauth2ClientID() {
        return oauth2ClientID;
    }

    public String getOauth2ClientSecret() {
        return oauth2ClientSecret;
    }

    public String getOAuth2Domain() {
        return oauth2Domain;
    }

    void parseOAuth2Section(JsonObject jsonObject) {
        final JsonObject oauth2Section = jsonObject.getJsonObject(OAUTH2_SECTION);


        this.oauth2Provider = oauth2Section.getString(OAUTH2_PROVIDER);

        // TODO: add more supported providers
        if (! oauth2Provider.equals(OAUTH2_PROVIDER_GOOGLE)) {
            throw new ConfigurationException( MessageFormat.format(
                    "Unsupported oauth2 provider: {}", oauth2Provider));
        }

        this.oauth2ClientID = oauth2Section.getString(OAUTH2_CLIENT_ID);
        this.oauth2ClientSecret = oauth2Section.getString(OAUTH2_CLIENT_SECRET);

        this.oauth2Domain = oauth2Section.getString(OAUTH2_DOMAIN);
    }

    public Configuration(JsonObject jsonObject) {
        parseServerSection(jsonObject);
        parseOAuth2Section(jsonObject);
    }

}