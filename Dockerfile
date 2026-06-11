FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/ libs/

RUN mv "$(find libs -maxdepth 1 -name '*.jar' ! -name '*plain*' | head -1)" app.jar \
    && rm -rf libs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]