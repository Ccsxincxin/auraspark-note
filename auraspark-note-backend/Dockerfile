FROM eclipse-temurin:23-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY auraspark-common/pom.xml auraspark-common/
COPY auraspark-core/pom.xml auraspark-core/
COPY auraspark-ai/pom.xml auraspark-ai/
COPY auraspark-launcher/pom.xml auraspark-launcher/
COPY auraspark-test/pom.xml auraspark-test/
RUN ./mvnw dependency:go-offline -B -q
COPY . .
RUN ./mvnw clean package -DskipTests -q -pl auraspark-launcher -am

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /app/auraspark-launcher/target/auraspark-launcher-*-exec.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
