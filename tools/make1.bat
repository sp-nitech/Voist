@echo off

if not exist portaudio (
    git clone https://github.com/PortAudio/portaudio.git
    pushd portaudio
    git checkout c1214824ab5b8e2229654aa737e7435782fd56d9
    popd
)

if not exist java (
    mkdir java
    pushd java
    curl -OL https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.exe
    curl -OL https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar
    curl -OL https://github.com/google/google-java-format/releases/download/v1.23.0/google-java-format-1.23.0-all-deps.jar
    popd
)

if not exist ant (
    mkdir ant
    pushd ant
    curl -OL https://dlcdn.apache.org//ant/binaries/apache-ant-1.10.15-bin.zip
    powershell -command "Expand-Archive -Path apache-ant-1.10.15-bin.zip" .
    popd
)

if not exist launch4j (
    mkdir launch4j
    pushd launch4j
    curl -OL https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-win32.exe
    popd
)

if not exist wixedit (
    mkdir wixedit
    pushd wixedit
    curl -OL https://github.com/WixEdit/WixEdit/releases/download/v0.8.2712.17/WixEdit-0.8.2712.17.msi
    popd
)

if not exist llvm (
    mkdir llvm
    pushd llvm
    curl -OL https://github.com/llvm/llvm-project/releases/download/llvmorg-19.1.0/LLVM-19.1.0-win64.exe
    popd
)

pause
