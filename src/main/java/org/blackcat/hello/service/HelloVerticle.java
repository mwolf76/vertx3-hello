package org.blackcat.hello.service;

/**
 * A sample web application using Google oauth2 provider
 * @author (c) 2017 marco DOT pensallorto AT gmail DOT com
 */

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.templ.PebbleTemplateEngine;
import org.blackcat.hello.conf.Configuration;
import org.blackcat.hello.http.RequestHandler;

public class HelloVerticle extends AbstractVerticle {

    private Logger logger;

    @Override
    public void start(Future<Void> startFuture) {

        logger = LoggerFactory.getLogger(HelloVerticle.class);

        /* retrieve configuration object from vert.x ctx */
        final Configuration configuration = new Configuration(vertx.getOrCreateContext().config());
        logger.info("Configuration: {}", configuration.toString());

        /* configure Pebble template engine */
        final PebbleTemplateEngine pebbleEngine = PebbleTemplateEngine.create(vertx);

        /* configure request handler */
        Handler<HttpServerRequest> handler =
                new RequestHandler(vertx, pebbleEngine, logger, configuration);

        HttpServerOptions httpServerOptions = new HttpServerOptions()
                // in Vert.x 2x 100-continues was activated per default, in vert.x 3x it is off per default.
                .setHandle100ContinueAutomatically(true);

        if (configuration.sslEnabled()) {
            httpServerOptions
                    .setSsl(true)
                    .setKeyStoreOptions(
                            new JksOptions()
                                    .setPath(configuration.getKeystoreFilename())
                                    .setPassword(configuration.getKeystorePassword()));
        }

        vertx.createHttpServer(httpServerOptions)
                .requestHandler(handler)
                .listen(configuration.getHttpPort(), result -> {

                    if (result.succeeded()) {
                        logger.info("Ready to accept requests on port {}.",
                                String.valueOf(configuration.getHttpPort()));

                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }
}
