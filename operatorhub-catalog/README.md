## create a tar.gz operator catalog package manifest archive for the entire listing of OperatorHub

This folder contains a bash script to create a tar.gz archive of package manifests for the entire OperatorHub listing of Operators, as input to the 
[Operator Catalog Management Mechanism](https://github.com/ldojo/ocp4-operator-catalog-management). 

The script needs internet access, or access to quay.io to pull package manifests and run successfully. However, it is quite useful
for OCP4 disconnected environments. If in your disconnected environment, there is a bastion or some node that does have internet access,
the script in this folder could be set up to run periodically and push the result operatorHub catalog tar.gz to an internal repository
like Artifactory or Nexus. The Operator Catalog running in the disconnected environment just needs to point to that Artifactory/Nexus locaiton
to keep an updated OperatorHub listing.

This folder also contains a Jenkinsfile which "wraps" the bash script, and is ready for launch in Jenkins as a Pipeline

Here is the example Usage for the bash script:
```
$ ./create-catalog.sh -h
Usage:
#set variable $CATALOG_NAME
#the script will create a file named $CATALOG_NAME.tar.gz
#Example: this will create a file named mycatalog.tar.gz, containing package manifests for the operatorhub catalog
export CATALOG_NAME=mycatalog
./create-catalog.sh

Optionally, you can also set a $POST_CATALOG_CREATION_SCRIPT variable with its content being a series of commands to run after the tar.gz is created
This can be useful if you want the script to push the catalog tar.gz file to a repository like Artifactory.
Example:
export CATALOG_NAME=mycatalog
export POST_CATALOG_CREATION_SCRIPT='curl -T mycatalog.tar.gz "http://myrepo.com/mycatalog.tar.gz"'
./create-catalog.sh

$CATALOG_NAME is not set. Exiting.
```

## Run script as a Jenkins Job via a Jenkins Container

This project also contains code to build a Jenkins Image that will have a Jenkins Job that executes the script to build the catalog. 
This Jenkins Job can be useful to run periodically, so that there is a background job that builds the latest catalog, and pushes it to 
some repo, like Artifactory. With this in place, you can have a self updating Operator Catalog

To build the image:
```
sudo podman build -f jenkins.Dockerfile -t <image name:tag> .
```

Run the jenkins container with podman:
```
sudo podman run --rm -it -p 8080:8080  -e FILENAME=mycatalog.tar.gz \
  -e POST_CATALOG_CREATION_SCRIPT='curl -T mycatalog.tar.gz "http://myrepo.com/mycatalog.tar.gz"' \
  <image name:tag>
```
The Jenkins server will have a pre-defined job named "operator-catalog", which you can run to create the catalog tar.gz file

Run it in Openshift vi an Openshift Template:
```
oc process -f jenkins/openshift/jenkins-template.yml \
  -p POST_CATALOG_CREATION_SCRIPT='curl -T mycatalog.tar.gz "http://myrepo.com/mycatalog.tar.gz"' \
  -p IMAGE=<image name:tag> | oc create -f -
```
A Route will be created that you can access Jenkins UI from
