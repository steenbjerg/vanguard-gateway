# vanguard-gateway

This project implements a simple api/http gateway that handles any http request going into my home installation:
* It has some security checks to avoid intruders from breaking my current setup.
* It handles any all http methods including SSE requests.
* It is not designed to handle a million requests per seconds. It is tailored for small home setups.

It is currently under developement.

## Testing keycloak

setup keycloak and the hosts file

bin/kc.[sh|bat] start --proxy=reencrypt --https-port=8543 --hostname-url=https://my-keycloak.org:8443 --hostname-admin-url=https://admin.my-keycloak.org:8443

proxy=reencrypt
https-port=8543
hostname-url=https://my-keycloak.org:8443
hostname-admin-url=https://admin.my-keycloak.org:8443
#hostname-strict=false



## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/vanguard-gateway-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

## Test
### SSE

```shell script
/usr/lib/jvm/java-21-openjdk-amd64/bin/java -classpath build/classes/java/main dk.stonemountain.vanguard.Sse
```
