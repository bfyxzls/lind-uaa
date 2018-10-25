FROM fabric8/java-jboss-openjdk8-jdk:1.4
ENV JAVA_APP_JAR="app.jar"
ENV JAVA_MAX_MEM_RATIO=40
ENV AB_OFF ""
ADD  --chown=jboss:jboss ./target/gateway-0.0.1-SNAPSHOT.jar /deployments/app.jar