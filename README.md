### 添加到Android studio<br>
##### Step1: 在根build.gradle中添加仓库：<br>

```
allprojects {
    repositories {
        mavenCentral()
        maven { url "https://jitpack.io" }
        maven() {
            url 'https://maven.aliyun.com/repository/releases'
        }

    }
}
```
##### 注意:maven { url "https://jitpack.io" }一定要放到 allprojects 里面不然更新不下来
#### Step2: 在工程中添加依赖：<br>
```
dependencies {
    implementation 'com.github.bigSnck:TVideoShowLibrary:1.0.0'
}
```
#### Step4: 在项目的主文件夹里面main文件夹下新建一个jniLibs文件夹：<br>
  main-->jniLibs-->armeabi-v7a-->添加libalivcffmpeg.so
  
#### Step4: 在工程中添加依赖：<br>
```
defaultConfig {
   ndk {abiFilters "armeabi-v7a"}
}
```

### 具体使用Demo<br>
#### 通常普通用法<br>
##### 代码：<br>
```
 String mUrl = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
 TPlayer.player(this, new UrlSourceData(mUrl, "哈哈哈哈"));
 
```
##### 效果图：<br>
<img src="https://github.com/bigSnck/TVideoShowLibrary/blob/master/image/video_2.jpg" width="500" height="300"/> 
<img src="https://github.com/bigSnck/TVideoShowLibrary/blob/master/image/video_1.jpg" width="300" height="500"/> 

#### 更多用法可以查看源码 谢谢！！ <br>
