FROM openjdk:8
RUN mkdir -p /app/app-server-migration
COPY ./target/app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar /app/app-server-migration
WORKDIR /app/app-server-migration
ENTRYPOINT ["java", "-jar", "app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar"]
