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
        classpath 'cn.enaium:ml4g:1.0.4'
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

### Use Mixin (Only support @Mixin,@Inject and @Accessor)

```groovy
minecraft {
    mainClass = "net.minecraft.launchwrapper.Launch"
    tweakClass = "cn.enaium.example.launch.Tweaker"
    mixinRefMap = "mixins.temp.refmap.json"
}
```

#### Example

```java
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(at = @At("HEAD"), method = "run()V")//✔
    public void run(CallbackInfo ci) {
        System.out.println("Hello ML4G");
    }
}
```

```java
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(at = @At("HEAD"), method = "run")//❌
    public void run(CallbackInfo ci) {
        System.out.println("Hello ML4G");
    }
}
```