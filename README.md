# ML4G

**Waraing: This plugin is under development and there are still many bugs**

## Install

```groovy
buildscript {
    repositories {
        mavenCentral()
        maven { url 'https://maven.enaium.cn/' }
    }

    dependencies {
        classpath 'cn.enaium:ml4g:1.0.6'
    }
}

apply plugin: 'ml4g'
```

```
gradlew idea
```

## Configuration

### Version

```groovy
minecraft {
    gameVersion = "1.16.5"//Default
}
```

### Use Mixin

```groovy
minecraft {
    mainClass = "net.minecraft.launchwrapper.Launch"
    tweakClass = "cn.enaium.example.launch.Tweaker"
    mixinRefMap = "mixins.temp.refmap.json"
}
```