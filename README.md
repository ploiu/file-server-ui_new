# New! File Server UI

A rewrite of the original frontend to [ploiu/file_server](https://github.com/ploiu/file_server), written in Kotlin
Compose Multiplatform.

## But Why?

The [original](https://github.com/ploiu/file-server-ui) was written in openjfx.

I didn't like how openjfx made it hard to make a ui that feel good. The styling was a weird adaptation of css but was
poorly documented, didn't fully adapt the css spec, and adapted other parts of the spec differently (using variables for
example).

On top of that, Compose Multiplatform allows both the desktop and android apps share a lot of logic, with mostly only
the ui pages being different

## Installing

### Linux

```shell
./gradlew clean packageDeb
sudo apt install ./composeApp/build/compose/binaries/main/deb/ploiu-file-server_1.0.1*.deb
```

## Building

### Dependencies:

- java21
- rust 1.88.0 (for desktop, building native code to store and retrieve the password set by you when logging into the
  server)

### Setup

due to a bug(?) in intellij with running apps in debug mode, gradle `Exec` tasks don't run _at all_ when debug is
selected.
Because of this, the ffi bindings for desktop credential manager aren't generated when running the `main [desktop]` run
config.
Run this to generate them

```shell
./gradlew :composeApp:buildCredsFfi
```

This will generate the bindings for your current platform and will need to be re-run if you ever delete the bindings
from `./composeApp/src/desktopMain/resources`

## Running

A few things are needed before running the application:

- a cert for the local server you're testing against
- a cert for the production server you're deploying to
- a configuration file

### Configuration file

in `./src/commonMain/composeResources/files`, there is an example config file named `app-example.properties`. Copy that
and rename the copy to `app.properties`. Fill out the specified fields.

### Certificates

Instead of dealing with java's keystore and android's certificate system, this app
uses [certificate hash pinning](https://www.cyberark.com/what-is/certificate-pinning/) to make the application trust the
server (and _only_ the server). Instructions for generating these certificates are located in `app-example.properties`,
but here it is again for convenience:

```sh
openssl x509 -in ./cert.pem
```

both certificates need to be stored alongside `app.properties`, with the dev server's cert being named `cert_local.x509`
and the production server certificate being named `cert_production.x509`

Certificate pinning is obsolete for websites and anything accessed via a web browser,
but for a small self-hosted application like this, the extra burden of updating the app when the certificate changes is
fine.

--- 

I do not consent to AI scraping of this project.
