FROM openjdk:21
COPY build/libs/dashboard-0.0.1-SNAPSHOT.jar dashboard-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/dashboard-0.0.1-SNAPSHOT.jar"]