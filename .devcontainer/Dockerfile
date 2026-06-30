FROM mcr.microsoft.com/devcontainers/base:alpine-3.20


# Setzen der Umgebungsvariablen
ENV CLOUD_SDK_VERSION=464.0.0
ENV FIRELY_TERMINAL_VERSION=3.3.2
ENV JAVA_VALIDATOR_VERSION=6.3.25
ENV SUSHI_VERSION=3.16.4

# Google Cloud SDK herunterladen und installieren
RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
  tar -xf google-cloud-cli-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
  ./google-cloud-sdk/install.sh --quiet && \
  rm google-cloud-cli-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz

# Installieren der notwendige Tools
# Add Microsoft's .NET SDK repository and install .NET SDK
RUN apk add --no-cache dotnet8-sdk

# Install ICU libraries
RUN apk add --no-cache icu-libs

# Install Node.js and npm
RUN apk add --no-cache nodejs npm

# Install Java (OpenJDK)
RUN apk add --no-cache openjdk17

# Install jekyll
RUN apk add --no-cache jekyll

# Install py-pip
RUN apk add --no-cache py-pip py3-yaml pipx

# Install Firely Terminal as vscode user
# Switch to the vscode user
USER vscode

RUN pipx install gsutil
ENV PATH="/google-cloud-sdk/bin:${PATH}"

# Install Firely Terminal
RUN dotnet tool install --global Firely.Terminal --version $FIRELY_TERMINAL_VERSION

# Add .NET tools to PATH for vscode user
ENV PATH="/home/vscode/.dotnet/tools:${PATH}"

# Add fhirscripts
RUN pipx install -f git+https://github.com/gematik/fhir-scripts.git

# Switch back to root user to perform remaining installations
USER root

RUN npm install -g fsh-sushi@$SUSHI_VERSION

# Static webserver for local development
RUN npm install -g http-server

# Installieren der zusätzliche Abhängigkeiten
RUN apk update && apk add --no-cache jq findutils curl ca-certificates

# Setup for FHIR Validator
RUN mkdir -p /home/vscode/.fhir/validators/
RUN wget -q https://github.com/hapifhir/org.hl7.fhir.core/releases/download/$JAVA_VALIDATOR_VERSION/validator_cli.jar -O /home/vscode/.fhir/validators/validator_cli.jar

# Set ownership
RUN mkdir -p /home/vscode/.fhir/packages && \
  chown -R vscode:vscode /home/vscode/.fhir/packages

RUN mkdir -p /home/vscode/.fhir/settings/
COPY codfsh-config.yaml /home/vscode/.fhir/settings/codfsh-config.yaml

# Installing PlantUML...
RUN apk add curl graphviz fontconfig

# Setup scripts
RUN mkdir /scripts
RUN curl -L https://raw.githubusercontent.com/gematik/fhir-scripts/refs/heads/main/scripts/fhir_scripts.sh -o /scripts/fhir_scripts.sh && chmod +x /scripts/fhir_scripts.sh
RUN /scripts/fhir_scripts.sh pytools

# Setzen des Arbeitsverzeichnis
WORKDIR /workspace

COPY . /workspace

CMD [ "/bin/bash" ]
