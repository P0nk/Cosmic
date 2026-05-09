# Helm Chart for Cosmic

## Quick Start

1.  Build the image for Cosmic and push it to your own image repository.

    ```bash
    docker build -t <image repository>:<image tag> ../../
    docker image push <image repository>:<image tag>
    ```

2.  Fetch dependencies.

    ```bash
    helm dependency build
    ```

3.  Create `values-override.yaml`.

    ```bash
    cat <<EOF > values-override.yaml
    image:
      repository: <image repository>
      tag: <image tag>
    EOF
    ```

4.  Install the Helm chart into your cluster.

    ```
    helm install cosmic-release . -f values-override.yaml
    ```

## Custom Config

1.  Create a copy of `config.yaml` and edit it.

    ```bash
    cp ../../config.yaml ./config.yaml
    vim config.yaml
    ```

2.  Install the Helm chart with your custom config file.

    ```bash
    helm install cosmic-release ../cosmic -f values-override.yaml \
      --set-file cosmic.configuration=./config.yaml
    ```
