FROM openshift/base-centos7

MAINTAINER Jacob Borella <jborella@redhat.com>

ENV BUILDER_VERSION x.x
ENV PATH $PATH:/opt/app-root/src/SoapUI-5.4.0/bin

LABEL io.k8s.description="Running 'SOAPUI simulator in a container" \
      io.k8s.display-name="builder x.y.z" \
      io.openshift.expose-services="8088:http"

RUN wget https://s3.amazonaws.com/downloads.eviware/soapuios/5.4.0/SoapUI-5.4.0-linux-bin.tar.gz
RUN tar -xvzf SoapUI-5.4.0-linux-bin.tar.gz
RUN rm SoapUI-5.4.0-linux-bin.tar.gz
RUN yum install -y java-1.8.0-openjdk devel
RUN yum clean all -y

COPY ./EMPL-18080-2-Simulator-soapui-project.xml .

USER 1001

EXPOSE 8080

CMD ["mockservicerunner.sh", "-p", "8080", "-m", "PersonEJBPortBinding MockService", "/opt/app-root/src/EMPL-18080-2-Simulator-soapui-project.xml"]
