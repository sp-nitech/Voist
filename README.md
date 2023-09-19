# Voist

![screen](share/screen.jpg?raw=true)

- このソフトウェアは，円滑な音声収録を目的として，名古屋工業大学徳田研究室により開発されたものです．
- Windowsのみインストーラを用意していますが，Javaを用いているため，ビルド次第では他のOSで動作できる可能性があります．

## 収録手順
1. メニューの左上にある『ファイル』から『ユーザの変更』を選択して，任意のユーザ名を入力して『変更』ボタンを押します．
   1. PCを共有していない場合，ユーザ名を変える必要はありません．
   1. 音声ファイルは`C:/Users/（Windowsのユーザ名）/Documents/Voist/recording/（ユーザ名）/（プロンプト名）/`以下に保存されます．
1. メニューの『設定』→『プロンプト』→ 『フォントタイプ』から好みのフォントを選択します．
   1. F2, F3キーでも変更可能です．
1. メニューの『設定』→『プロンプト』→ 『フォントサイズ』からフォントの大きさを設定します．
   1. マウスホイールでも調整可能です．
1. 『サンプル再生』ボタンを押して，お手本の音声を聞きます．
   1. サンプル音声ファイルは`C:/Users/（Windowsのユーザ名）/Documents/Voist/sample/（プロンプト名）/`以下に配置されている必要があります．
   1. このとき，サンプル音声ファイルは録音ファイル名と同一である必要があります．
   1. サンプル音声が無い場合や，お手本に従う必要がない場合はスキップしてください．
1. 『収録開始』ボタンを押すとビープ音が再生されるので，**一拍置いてから**文を読み上げます．
1. 文を読み終わったら，同様に**一拍置いてから**『収録停止』ボタンを押します．
   1. 「収録に失敗しました」というメッセージが出た場合，メッセージに従って再収録してください．
1. 必要に応じて『収録音声再生』ボタンを押して，収録音声を確認します．
1. 『次の文へ』ボタンを押して，すべての文を収録するまで同様の操作を繰り返します．
   1. メニューの『表示』→『収録フォルダ』から収録した音声を確認できます．

## Q&A
- 収録の成功条件を厳しくしたいです．
  - メニューの『設定』→『音声収録』から所望の設定に変更してください．
- 漢字の読めない小さな子に読ませたいです．
  - メニューの 『設定』→『プロンプト』→『ルビタイプ』からBを選択してください．
  - F1キーでも変更可能です．
- 一文当たりの録音の最大時間は何秒ですか．
  - 20秒です．
- サンプリング周波数を変更したいです．
  - `res/properties/system.Voist.properties`の`SAMPLE_RATE`を変更してください．
- ビットレートを変更したいです．
  - `res/properties/system.Voist.properties`の`SAMPLE_SIZE`を変更してください．
  - 2 (16bit), 3 (24bit), 4 (32bit)のみ対応しています．
- サンプル再生ボタンを隠したいです．
  - `res/properties/system.Buttons.properties`の`USE_SAMPLE`を`false`にしてください．
- 音声合成器を作りたいです．
  - こちらのソフトウェアは音声収録のみを目的としています．
  - [HTS](https://hts.sp.nitech.ac.jp/)や[ESPnet](https://github.com/espnet/espnet)等のソフトウェアをご利用ください．

## 開発環境の構築（開発者向け）

### 要件
- [Visual Studio 2022](https://visualstudio.microsoft.com/ja/vs/)
- [.NET framework 2.0](https://www.microsoft.com/ja-jp/download/details.aspx?id=25150) (for WixEdit)

### 手順
1. 前準備
   1. `git clone https://github.com/sp-nitech/Voist.git`
   1. `CallPortAudio.dll`と`portaudio_x64.dll`がシンボリックリンクになっているか確認する．
   1. `tools`以下にある`make1.bat`を実行する．
   1. `tools/java/jdk-21_windows-x64_bin.exe`を実行してJDKをインストールする（インストール先はデフォルトのまま）．
   1. `tools`以下にある`make2.bat`を実行する．
1. ASIO SDKをダウンロード
   1. [Steinberg](https://www.steinberg.net/developers/)からASIO SDKをダウンロードする．
   1. ダウンロードしたzipファイルを解凍する．
   1. 解凍してできたディレクトリを`ASIOSDK`にリネームする．
   1. リネームした`ASIOSDK`を`tools/portaudio/src/hostapi/asio/`に配置する．
1. PortAudioをコンパイル
   1. `tools/portaudio/msvc/portaudio.sln`を開く．
   1. プロジェクトの変換について聞かれるのでOKを押す（警告が出るが無視する）．
   1. ソリューション構成をDebugからReleaseに変更する．
   1. プラットフォームがx64になっているか確認する．
   1. 『プロジェクト』→『プロパティ』→『構成プロパティ』→『C/C++』を選択する．
      1. 『最適化』→『フレームポイントなし』を『はい/(Oy)』にする．
      1. 『コード生成』→『ランタイムライブラリ』を『マルチスレッド(/MT)』にする．
      1. 『コード生成』→『浮動小数点モデル』を『Fast/(fp:fast)』にする．
   1. 『ビルド』→『ソリューションのビルド』を実行する．
1. CallPortAudioをコンパイル
   1. `extern/CallPortAudio.sln`を開く．
   1. ソリューション構成をDebugからReleaseに変更する．
   1. プラットフォームがx64になっているか確認する．
   1. 『ビルド』→『ソリューションのビルド』を実行する．
1. Voistをコンパイル
   1. [Eclipse Foundation](https://www.eclipse.org/downloads/)からEclipseのインストーラをダウンロードする．
   1. インストーラを実行してEclipse IDE for Java Developersを選択する．
   1. 『File』→『Open Projects from File System...』から，このリポジトリのルートディレクトリを指定する．
   1. 『Run』→『Run』で動作確認する．
1. インストーラを作成
   1. `tools/launch4j/launch4j-3.50-win32.exe`を実行してLaunch4jをインストールする．
   1. `tools/wixedit/WixEdit-0.8.1417.11.msi`を実行してWixEditをインストールする．
   1. 『File』→『Export』→『Java』→『Runnable JAR file』からJARファイルを出力する．
      1. Launch configuration: Voist
      1. Export destination: このリポジトリの`package/bin`
      1. Package required libraries into generated JAR: チェック
   1. Launch4j.exeを起動してJARファイルをEXEファイルに変換する．
      1. `package/res/launch4j.config.xml`を開く．
      1. Build wrapper（歯車のボタン）を実行する．
   1. WixEditを起動してインストーラを作成する．
      1. `package/res/wixedit.config.wxs`を開く．
      1. 『Build』→『Build MSI setup package』を実行する．
   1. インストーラ`package/bin/Voist.msi`を実行して動作確認する．

## 参考文献
```bibtex
@InProceedings{sp-nitech2016voist,
  author = {吉村建慶 and 橋本佳 and 大浦圭一郎 and 南角吉彦 and 徳田恵一},
  title = {クラウドソーシングによる音声収集のための収録ソフトウェアの設計},
  booktitle = {日本音響学会2016年春季研究発表会},
  pages = {307--308},
  year = {2016},
}
```
