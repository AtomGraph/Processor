FROM maven:3.5.3-jdk-8 as maven

### Build AtomGraph Processor

WORKDIR /usr/src/Processor

COPY . /usr/src/Processor

RUN mvn -Pstandalone clean install

### Deploy Processor webapp on Tomcat

FROM tomcat:8.0.52-jre8

WORKDIR $CATALINA_HOME/webapps

RUN rm -rf * # remove Tomcat's default webapps

# copy exploded WAR folder from the maven stage
COPY --from=maven /usr/src/Processor/target/ROOT/ ROOT/

WORKDIR $CATALINA_HOME

COPY src/main/webapp/META-INF/context.xml conf/Catalina/localhost/ROOT.xml

### Install XSLT processor and ps

RUN apt-get update && \
  apt-get -y install xsltproc && \
  apt-get -y install procps

### Copy entrypoint

COPY entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh

COPY context.xsl conf/Catalina/localhost/context.xsl

ENTRYPOINT ["/usr/local/tomcat/entrypoint.sh"]

EXPOSE 8080

# system location mapping
ENV LOCATION_MAPPING="/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/location-mapping.n3"

# user-defined location mapping
ENV CUSTOM_LOCATION_MAPPING="/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/custom-mapping.n3"