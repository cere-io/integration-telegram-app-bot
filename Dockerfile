FROM azul/zulu-openjdk:21 as builder

ARG ROOT_DIR=/app
ARG ENV_FILE_NAME=local

COPY --chown=185 . $ROOT_DIR
WORKDIR $ROOT_DIR

ENV ENV=$ENV_FILE_NAME
RUN ./gradlew quarkusBuild

FROM azul/zulu-openjdk:21-jre

ARG ROOT_DIR=/app
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --from=builder $ROOT_DIR/build/quarkus-app/lib/ /deployments/lib/
COPY --from=builder $ROOT_DIR/build/quarkus-app/*.jar /deployments/
COPY --from=builder $ROOT_DIR/build/quarkus-app/app/ /deployments/app/
COPY --from=builder $ROOT_DIR/build/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV AB_JOLOKIA_OFF=""
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT exec java $JAVA_OPTS -jar $JAVA_APP_JAR