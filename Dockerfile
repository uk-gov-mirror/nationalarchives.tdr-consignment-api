FROM openjdk:16-jdk-alpine
WORKDIR play
COPY target/scala-2.13/consignmentapi.jar /
CMD java -Dconfig.resource=application.$ENVIRONMENT.conf \
            -jar /consignmentapi.jar \
            -Dconsignmentapi.db.user=$DB_USER \
            -Dconsignmentapi.db.password=$DB_PASSWORD \
            -Dconsignmentapi.db.url=jdbc:mysql://$DB_URL:3306/consignmentapi \
            -Dauth.url=$AUTH_URL
