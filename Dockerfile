FROM openjdk:8-jdk-slim

ADD ./target/simulatiesysteem-1.0-SNAPSHOT.jar simulatiesysteem-1.0-SNAPSHOT.jar

CMD ["java", "-jar", "simulatiesysteem-1.0-SNAPSHOT.jar"]
