# API Gateway router

This is an authentication service built using Spring Boot that allows users to sign in using email/password or by using Google OAuth. It utilizes JWT (JSON Web Token) for secure authentication and authorization, and Redis for session management and rate limiting to ensure security and scalability.

`Note`: This project in its current state not meant to be an out-of-the-box API gateway. It needs code changes to configure your backend services.

## Features

* Email and password based authentication
* Google OAuth based authentication
* JWT authentication and authorization (You can tweak project to use JWT or session)
* Session mgmt using Redis
* Redis based rate limiting
* Phone number verification using Twilio
* Routing to backend services

## Table of Contents

*   [Getting Started](#getting-started)
    *   [Prerequisites](#Prerequisites)
    *   [Instructions](#instructions-to-setup)
*   [Configuration](#configuration)
*   [Running the Application](#running-the-application)
*   [Docker Compose](#docker-compose)
*   [Project Structure](#project-structure)

## Getting started

### Prerequisites

1.  Java 17
2.  Maven
3.  Docker

### Instructions to setup

1.  Clone the repository

```shell
git clone git@github.com:avipl/auth-router-gateway.git
```

1.  Navigate to the project directory

```shell
cd auth-router-gateway
```

1.  Build the project

mvn clean install

1.  Create a copy of .env.example file and rename it to .env.
2.  Update the environment variable in the .env file.

## Configuration:

Application uses following environment variables. 

1. Create a `.env` file in the root directory.

```properties
PGUSER=postgres
PGPASSWORD= # replace with postgres password
PGHOST=pgServer # Docker container hostname
PGPORT=5432
PGDATABASE=auth
```

2. Create a `application.properties` file in the root directory

```properties
spring.security.oauth2.client.registration.google.client-id=
spring.security.oauth2.client.registration.google.client-secret=
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google

# Postgres
spring.datasource.url=jdbc:postgresql://pgserver:5432/auth
spring.datasource.username=postgres
spring.datasource.password= # replace with postgres password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.generate-ddl=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# redis
spring.data.redis.host=redis
spring.data.redis.password=
spring.data.redis.port=6379

#Twilio
twilio.ACCOUNT_SID= # replace with twilio account SID
twilio.AUTH_TOKEN= # replace with twilio account auth token

```

## Running the application:

1.  Using maven

```shell
mvn spring-boot:run
```

1.  Using docker compose

```shell
docker compose build
docker compose up -d
```

`-d` starts the containers in detached mode.

## Docker Compose:

The compose.yml file at the root of the project defines the services (containers) that make up the application.  It handles the networking, volumes, and dependencies between the containers.

```
services:
  auth: # app service
    build: .
    hostname: auth # service accessible with this hostname over docker network
    depends_on:
      pgserver:
          condition: service_healthy # wait for Postgres service to respond
      redis:
          condition: service_healthy # wait for Redis service to respond
    ports:
      - "8080:8080" # Map host port 8080 to container port 8080
    networks:
      - avi-portfolio # Creates an overlay network to communicate with other services (i.e. cotainer)
  
  pgserver:
    image: postgres:17.2
    hostname: pgserver
    env_file:# set the environment variable of the container using the following file
      - .env # dot env located at project root 
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: ${PGPASSWORD}
    restart: unless-stopped
    networks:
      - avi-portfolio
    volumes:
      - type: bind
        source: postgres-data
        target: /var/lib/postgresql
    healthcheck: # Auth service uses this healthcheck to make sure postgres server is ready
      test: [ "CMD-SHELL", "pg_isready -U $PGUSER" ]
      interval: 5s
      timeout: 5s
      retries: 5

  pgadmin-ui:
    image: dpage/pgadmin4
    env_file:
      - .env
    environment:
      PGADMIN_DEFAULT_EMAIL: test@test.com
      PGADMIN_DEFAULT_PASSWORD: password@3578
    ports:
      - "5000:80"
    networks:
      - avi-portfolio
  
  pgserver:
    image: postgres:17.2
    hostname: pgres
    env_file:
      - .env
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: ${PGPASSWORD}
    restart: unless-stopped
    networks:
      - avi-portfolio
    volumes:
      - type: bind # persists database data
        source: postgres-data
        target: /var/lib/postgresql 
    healthcheck: 
      test: ["CMD-SHELL", "pg_isready -U $PGUSER"]
      interval: 5s
      timeout: 5s
      retries: 5

networks:
  avi-portfolio: # Overlay network for service containers
    name: avi-portfolio

volumes:
  postgres-data: # Named volume for database data
```

## Project Structure:

Following is the project structure,

```
.
|   .env
|   .env.example # example environment variable
|   .gitattributes
|   .gitignore
|   application.properties
|   application.properties.example
|   compose.yaml # used by docker compose command see [Docker compose](###Docker-Compose)
|   Dockerfile # build instruction for auth service image
|   HELP.md
|   LICENSE
|   mvnw
|   mvnw.cmd
|   pom.xml # project dependencies
|   Readme.md # this file!
|   
+---.mvn
|   \---wrapper
|           maven-wrapper.properties
|           
+---doc
+---postgres-data
|   \---data
+---src
|   +---main
|   |   +---java
|   |   |   \---com
|   |   |       \---authdemo
|   |   |           \---auth
|   |   |               |   AuthApplication.java # main file
|   |   |               |   
|   |   |               +---component
|   |   |               |       AuthTokenResolver.java
|   |   |               |       
|   |   |               +---config
|   |   |               |       GatewayRouterConfig.java # Configure your backend service routes here
|   |   |               |       JpaConfig.java
|   |   |               |       JwtConfig.java
|   |   |               |       RateLimiterConfig.java
|   |   |               |       SecurityConfig.java
|   |   |               |       
|   |   |               +---controller
|   |   |               |       AuthController.java # APIs that handles sign in and signup
|   |   |               |       PhoneVerificationController.java # Business logic for phone verification
|   |   |               |       
|   |   |               +---converter
|   |   |               +---dto
|   |   |               |       ExceptionResponseDto.java
|   |   |               |       RegistrationRequestDto.java
|   |   |               |       SignInRequestDto.java
|   |   |               |       SignInResponseDto.java
|   |   |               |       
|   |   |               +---entity
|   |   |               |       RefreshToken.java
|   |   |               |       Role.java
|   |   |               |       User.java
|   |   |               |       
|   |   |               +---filter
|   |   |               |       RateLimitFilter.java
|   |   |               |       
|   |   |               +---handler
|   |   |               |       OAuthSuccessHandler.java
|   |   |               |       
|   |   |               +---model
|   |   |               |       AuthTokens.java
|   |   |               |       CustomOidcUser.java
|   |   |               |       
|   |   |               +---repository
|   |   |               |       RefreshTokenRepository.java
|   |   |               |       UserRepository.java
|   |   |               |       
|   |   |               +---service
|   |   |               |       CustomOidcUserService.java
|   |   |               |       JpaUserDetailsService.java
|   |   |               |       JwtService.java
|   |   |               |       SignInService.java
|   |   |               |       
|   |   |               \---util
|   |   |                       CookieUtil.java
|   |   |                       
|   |   \---resources
|   |       |   application.properties
|   |       |   
|   |       +---db
|   |       |   \---migration
|   |       +---jwt
|   |       |       app.key
|   |       |       app.pub
|   |       |       
|   |       +---static
|   |       |       index.html
|   |       |       
|   |       \---templates
|   \---test
       \---java
           \---com
               +---authdemo
                  \---auth
                          AuthApplicationTests.java                      

```

