FROM openjdk:8-alpine

COPY target/uberjar/realworld-clj.jar /realworld-clj/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/realworld-clj/app.jar"]
