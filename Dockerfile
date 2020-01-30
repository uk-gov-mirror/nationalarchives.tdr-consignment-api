FROM openjdk:8-slim
WORKDIR play
COPY target/scala-2.13/consignmentapi.jar /
CMD  java -jar /consignmentapi.jar -Dconsignmentapi.user=$DB_USER \
            -Dconsignmentapi.password=$DB_PASSWORD \
            -Dconsignmentapi.url=jdbc:mysql://$DB_URL:3306/consignmentapi
