# Openshift 4.x Operator Catalog Utilities

This project contains an API service which provides some useful endpoints for managing tar.gz Operator package manifest archives, as described in [Openshift 4.x Operator Catalog Management project](https://github.com/ldojo/ocp4-operator-catalog-management).

To build/launch the api service with Podman or Docker:

```
podman built -t <image> .
podman run --rm -it -p 8080:8080 <image>
```

Launched on port 8080, you can go to http://host:8080/swagger-ui.html#/operator-catalog-util-apis and try out the apis via the Swagger interface
