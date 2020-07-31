FROM docker.io/jenkins/jenkins:latest

ENV JAVA_OPTS=-Djenkins.install.runSetupWizard=false

COPY jenkins/users "$JENKINS_HOME"/users/
COPY jenkins/config.xml "$JENKINS_HOME"/config.xml
COPY jenkins/jobs "$JENKINS_HOME"/jobs
COPY create-catalog.sh /usr/local/bin/
COPY oc.tar.gz /tmp/

USER root
RUN tar zxf /tmp/oc.tar.gz -C /usr/local/bin/
USER jenkins

COPY jenkins/plugins/plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt
