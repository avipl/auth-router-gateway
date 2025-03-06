# the base image
FROM maven:3.9.9-amazoncorretto-17-alpine

RUN mkdir /app
COPY . /app
WORKDIR /app

RUN mvn clean package -DskipTests

ENTRYPOINT ["java", "-jar", "target/auth.jar"]