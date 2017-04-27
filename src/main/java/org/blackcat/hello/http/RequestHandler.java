package org.blackcat.hello.http;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.KeycloakHelper;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.TemplateEngine;
import org.blackcat.hello.conf.Configuration;

import java.text.MessageFormat;

public class RequestHandler implements Handler<HttpServerRequest> {

    private Router router;
    private TemplateEngine templateEngine;
    private Logger logger;

    public RequestHandler(final Vertx vertx, final TemplateEngine templateEngine,
                          final Logger logger, final Configuration configuration) {

        this.router = Router.router(vertx);
        this.templateEngine = templateEngine;
        this.logger = logger;

        // We need cookies, sessions and request bodies
        router.route()
                .handler(CookieHandler.create());

        router.route()
                .handler(BodyHandler.create());

        // avoid reading, sniffing hijacking or tampering your sessions.
        router.route()
                .handler(SessionHandler
                        .create(LocalSessionStore.create(vertx))
                        .setCookieHttpOnlyFlag(true)
                        .setCookieSecureFlag(true));

        // oauth2 setup
        setupOAuth2(vertx, router, configuration);

        /* public index */
        router
                .getWithRegex("/")
                .handler(this::publicIndexGetRequest);

        /* protected area */
        router
                    .getWithRegex("/protected/.*")
                    .handler(this::protectedGetRequest);

        /* extra handlers */
        router
                .getWithRegex("/static/.*")
                .handler(StaticHandler.create());

        /* invalid URL */
        router
                .getWithRegex(".*")
                .handler(this::notFound);

       /* invalid method */
        router
                .routeWithRegex(".*")
                .handler(this::notAllowed);
    }

    private void setupOAuth2(final Vertx vertx, final Router router, final Configuration configuration) {

        final String callbackURL = "/oauth2";

        OAuth2Auth authProvider = null;
        final String oauth2ProviderName = configuration.getOauth2Provider();
        if (oauth2ProviderName.equals("google")) {
            authProvider = GoogleAuth.create(vertx,
                    configuration.getOauth2ClientID(), configuration.getOauth2ClientSecret());
        }
        if (authProvider == null) {
            throw new RuntimeException(
                    MessageFormat.format("Unsupported OAuth2 provider: {}", oauth2ProviderName));
        }

        // create a oauth2 handler on our domain
        OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(authProvider,
                configuration.getOAuth2Domain());

        // these are the scopes
        authHandler.addAuthority("profile");
        authHandler.addAuthority("email");

        // We need a user session handler too to make sure the user
        // is stored in the session between requests
        router
                .route()
                .handler(UserSessionHandler.create(authProvider));

        // setup the callback handler for receiving the Google callback
        authHandler.setupCallback(router.get(callbackURL));

        router
                .route("/protected/*")
                .handler(authHandler);

        // additional login path
        router
                .route("/login")
                .handler(authHandler);

        // logout
        router
                .route("/logout")
                .handler(ctx -> {
                    ctx.clearUser();
                    found(ctx, "/");
                });

        logger.info("OAUTH2 setup complete");
    }

    @Override
    public void handle(HttpServerRequest request) {
        logger.info("Accepting HTTP Request: {} {} ...", request.method(), request.uri());
        router.accept(request);
    }

    /*** Route handlers ***********************************************************************************************/
    private void publicIndexGetRequest(RoutingContext ctx) {

        templateEngine.render(ctx, "templates/index", asyncResult -> {
            if (asyncResult.succeeded()) {
                final Buffer result = asyncResult.result();

                ctx.response()
                        .putHeader(Headers.CONTENT_TYPE_HEADER, "text/html; charset=utf-8")
                        .putHeader(Headers.CONTENT_LENGTH_HEADER, String.valueOf(result.length()))
                        .end(result);
            }
        });
    }

    private void protectedGetRequest(RoutingContext ctx) {

        final User user = ctx.user();
        AccessToken at = (AccessToken) user;

        JsonObject idToken = KeycloakHelper.idToken(at.principal());
        final String email = idToken.getString("email");

        /* populate ctx with template params */
        ctx.put("email", email);

        templateEngine.render(ctx, "templates/protected", asyncResult -> {
            if (asyncResult.succeeded()) {
                final Buffer result = asyncResult.result();

                ctx.response()
                        .putHeader(Headers.CONTENT_TYPE_HEADER, "text/html; charset=utf-8")
                        .putHeader(Headers.CONTENT_LENGTH_HEADER, String.valueOf(result.length()))
                        .end(result);
            }
        });
    }

    /*** Responders ***************************************************************************************************/
    private void badRequest(RoutingContext ctx, String message) {
        HttpServerRequest request = ctx.request();
        logger.debug("Bad Request: {}", request.uri());

        request.response()
                .setStatusCode(StatusCode.BAD_REQUEST.getStatusCode())
                .setStatusMessage(StatusCode.BAD_REQUEST.getStatusMessage())
                .end(message != null ? message : StatusCode.BAD_REQUEST.getStatusMessage());
    }

    private void conflict(RoutingContext ctx, String message) {
        HttpServerRequest request = ctx.request();
        logger.debug("Conflict: {}", message);

        request.response()
                .setStatusCode(StatusCode.CONFLICT.getStatusCode())
                .setStatusMessage(StatusCode.CONFLICT.getStatusMessage())
                .end(message);
    }

    private void done(RoutingContext ctx) {
        ctx.response()
                .end();
    }

    private void notAllowed(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        logger.debug("Not allowed: {}", request.uri());

        request.response()
                .setStatusCode(StatusCode.METHOD_NOT_ALLOWED.getStatusCode())
                .setStatusMessage(StatusCode.METHOD_NOT_ALLOWED.getStatusMessage())
                .end(StatusCode.METHOD_NOT_ALLOWED.toString());
    }

    private void notAcceptable(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        logger.debug("Not acceptable: {}", request.uri());

        request.response()
                .setStatusCode(StatusCode.NOT_ACCEPTABLE.getStatusCode())
                .setStatusMessage(StatusCode.NOT_ACCEPTABLE.getStatusMessage())
                .end(StatusCode.NOT_ACCEPTABLE.toString());
    }

    private void notFound(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        final MultiMap headers = request.headers();
        String accept = headers.get(Headers.ACCEPT_HEADER);
        boolean html = (accept != null && accept.contains("text/html"));
        boolean json = (accept != null && accept.contains("application/json"));

        logger.debug("Resource not found: {}", request.uri());
        HttpServerResponse response = ctx.response();
        response
                .setStatusCode(StatusCode.NOT_FOUND.getStatusCode())
                .setStatusMessage(StatusCode.NOT_FOUND.getStatusMessage());

        if (html) {
            templateEngine.render(ctx, "templates/notfound", asyncResult -> {
                if (asyncResult.succeeded()) {
                    response
                            .putHeader(Headers.CONTENT_TYPE_HEADER, "text/html; charset=utf-8")
                            .end(asyncResult.result());
                }
            });
        } else if (json) {
            response
                    .end(new JsonObject()
                            .put("status", "error")
                            .put("message", "Not Found")
                            .encodePrettily());
        } else /* assume: text/plain */ {
            response
                    .end(StatusCode.NOT_FOUND.toString());
        }
    }

    private void notModified(RoutingContext ctx, String etag) {
        ctx.response()
                .setStatusCode(StatusCode.NOT_MODIFIED.getStatusCode())
                .setStatusMessage(StatusCode.NOT_MODIFIED.getStatusMessage())
                .putHeader(Headers.ETAG_HEADER, etag)
                .putHeader(Headers.CONTENT_LENGTH_HEADER, "0")
                .end();
    }

    private void ok(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        logger.debug("Ok: {}", request.uri());

        ctx.response()
                .setStatusCode(StatusCode.OK.getStatusCode())
                .setStatusMessage(StatusCode.OK.getStatusMessage())
                .end(StatusCode.OK.toString());
    }

    private void internalServerError(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        logger.debug("Internal Server Error: {}", request.uri());

        ctx.response()
                .setStatusCode(StatusCode.INTERNAL_SERVER_ERROR.getStatusCode())
                .setStatusMessage(StatusCode.INTERNAL_SERVER_ERROR.getStatusMessage())
                .end(StatusCode.INTERNAL_SERVER_ERROR.toString());
    }

    private void found(RoutingContext ctx, String targetURI) {
        ctx.response()
                .setStatusCode(StatusCode.FOUND.getStatusCode())
                .setStatusMessage(StatusCode.FOUND.getStatusMessage())
                .putHeader(Headers.LOCATION_HEADER, targetURI)
                .end();
    }

    /*** Helpers ******************************************************************************************************/

}
