FROM openjdk:16-jdk-alpine
#For alpine versions need to create a group before adding a user to the image
RUN addgroup -S apigroup && adduser -S apiuser -G apigroup
WORKDIR play
COPY target/scala-2.13/consignmentapi.jar /
CMD java -Dconfig.resource=application.$ENVIRONMENT.conf \
            -jar /consignmentapi.jar \
            -Dconsignmentapi.db.user=$DB_USER \
            -Dconsignmentapi.db.password=$DB_PASSWORD \
            -Dconsignmentapi.db.url=jdbc:mysql://$DB_URL:3306/consignmentapi \
            -Dauth.url=$AUTH_URL
RUN chown -R apiuser /play
RUN chown apiuser /consignmentapi.jar

USER apiuser
