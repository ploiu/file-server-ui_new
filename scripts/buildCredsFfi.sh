#!/usr/bin/env bash

if [ -f ./gradlew ]
then
    projectPath=$(pwd)
elif [ -f ../gradlew ]
then
    projectPath=$(pwd)/..
else
    echo "could not find project root from $(pwd)"
    exit 1
fi

source "$projectPath"/scripts/colors.sh

# we only support linux, so make sure we're on linux
os=$(uname -s | tr '[:upper:]' '[:lower:]')
# arch will be x86_64 if it's x86, but we will replace the _ with a - to help later on
arch=$(uname -m | sed 's/_/-/')
cyanFg "detected machine as $os-$arch"
if [[ "$os" != "linux" || "$arch" != *64* ]]
then
    redFg "only 64 bit linux and windows are supported at this time (and only linux is supported for this script)"
    exit 1
fi

# need to make sure cargo exists before building
grayFg "checking dependencies before building..."
if ! command -v cargo &> /dev/null
then
    redFg 'cargo could not be found, please install it from https://docs.rust-lang.org'
    exit
fi

cyanFg "all dependencies found! building creds-ffi..."

cd "$projectPath"/creds-ffi || exit

grayFg "building creds-ffi..."

if ! cargo build --release
then
    redFg "build failed! Check above logs for more details"
    exit 1
fi

# create the dir for jna to load our library from
libOutputPath=$projectPath/composeApp/src/desktopMain/resources/$os-$arch
rm -rf "$libOutputPath"
mkdir -p "$libOutputPath"
magentaFg "copying built library to $libOutputPath"
cp ./target/release/librust_credential_manager.so "$libOutputPath"
greenFg "build complete!"
