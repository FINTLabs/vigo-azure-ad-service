FROM fintlabs.azurecr.io/vigo-azure-ad-frontend:latest as client

FROM gradle:6.3.0-jdk11-alpine as builder
USER root
COPY . .
COPY --from=client /src/build/ src/main/resources/static/
RUN gradle --no-daemon build

FROM gcr.io/distroless/java:11
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/vigo-azure-ad-service-*.jar /data/vigo-azure-ad-service.jar
CMD ["/data/vigo-azure-ad-service.jar"]