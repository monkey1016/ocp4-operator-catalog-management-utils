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

