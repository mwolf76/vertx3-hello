#!/bin/sh
mvn clean package -DskipTests && \
    java -jar -Dvertx.logger-delegate-factory-class-name="io.vertx.core.logging.SLF4JLogDelegateFactory" target/hello-1.0-SNAPSHOT-fat.jar -conf conf/hello-config.json run org.blackcat.hello.service.HelloVerticle



