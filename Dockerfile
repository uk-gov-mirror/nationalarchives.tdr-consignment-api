FROM openjdk:16-jdk-alpine
#For alpine versions need to create a group before adding a user to the image
RUN addgroup --system apigroup && adduser --system apiuser -G apigroup
RUN apk update
RUN apk upgrade
RUN apk add bash
WORKDIR api
COPY target/scala-2.13/consignmentapi.jar /api
RUN chown -R apiuser /api

USER apiuser
CMD java -Dconfig.resource=application.$ENVIRONMENT.conf \
            -jar /api/consignmentapi.jar \
            -Dconsignmentapi.db.user=$DB_USER \
            -Dconsignmentapi.db.password=$DB_PASSWORD \
            -Dconsignmentapi.db.url=jdbc:mysql://$DB_URL:3306/consignmentapi \
            -Dauth.url=$AUTH_URL
