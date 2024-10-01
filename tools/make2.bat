@echo off

if not exist igo (
    mkdir igo
    pushd igo
    curl -OL https://ftp.iij.ad.jp/pub/osdn.jp/igo/55029/igo-0.4.5-src.tar.gz
    tar xzvf igo-0.4.5-src.tar.gz
    pushd igo-0.4.5-src
    start /wait cmd /c ..\..\ant\apache-ant-1.10.15\bin\ant
    move igo-0.4.5.jar ..\..\java\
    popd
    popd
)

if not exist mecab-ipadic (
    rmdir /s /q ..\res\ipadic
    mkdir mecab-ipadic
    pushd mecab-ipadic
    curl -OL https://sourceforge.net/projects/mecab/files/mecab-ipadic/2.7.0-20070801/mecab-ipadic-2.7.0-20070801.tar.gz
    tar xzvf mecab-ipadic-2.7.0-20070801.tar.gz
    java -cp ..\java\igo-0.4.5.jar net.reduls.igo.bin.BuildDic ipadic mecab-ipadic-2.7.0-20070801 EUC-JP
    move ipadic ..\..\res\
    popd
)

if not exist ..\jre (
    xcopy /e /i "C:\Program Files\Java\jdk-21" ..\jre
)

javac -h ..\extern\CallPortAudio\src -sourcepath ..\src -d ..\bin ..\src\jp\ac\nitech\sp\voist\CallPortAudio.java

pause
