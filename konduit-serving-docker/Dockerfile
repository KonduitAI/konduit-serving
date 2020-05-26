ARG BACKEND
FROM BACKEND
MAINTAINER Shams Ul Azeem <shams@konduit.ai>

# Install "software-properties-common" (for the "add-apt-repository")
RUN apt-get update && apt-get install -y \
    software-properties-common

# Add the "JAVA" ppa
RUN add-apt-repository -y \
    ppa:webupd8team/java

# Install OpenJDK-8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME

# Add the service itself
ARG JAR_FILE
ADD ../konduit-serving-uberjar/target/${JAR_FILE} /srv/konduit.jar

EXPOSE 9008

CMD ["java", "-jar", "/srv/konduit.jar", "serve", "-c", "/srv/assets/config"]
