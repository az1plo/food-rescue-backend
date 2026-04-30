FROM maven:3.9-eclipse-temurin-25 as builder
LABEL Miroslav Rucka <miroslav.rucka@posam.sk>

WORKDIR /application

COPY pom.xml .

# TODO: ak nedefinujes child module build nezbehne:
# 14.15 [ERROR] [ERROR] Some problems were encountered while processing the POMs:
# 14.15 [ERROR] Child module /application/application/domain of /application/pom.xml does not exist
COPY application application

RUN mvn package
RUN mkdir build && cd build && java -Djarmode=layertools -jar ../application/springboot/target/*.jar extract

FROM eclipse-temurin:25-jdk-alpine
WORKDIR /application

# TODO: na --from=builder pls review
COPY --from=builder application/build/dependencies/ ./
COPY --from=builder application/build/spring-boot-loader/ ./
COPY --from=builder application/build/snapshot-dependencies/ ./
COPY --from=builder application/build/application/ ./

RUN addgroup -S boot && adduser -S boot -G boot
USER boot

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher"]
