import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tucolectivo.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tucolectivo.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1020
        versionName = "1.1.10"
    }
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/tucolectivo20sanluis-release.jks")
            storePassword = "android"
            keyAlias = "tucolectivo20sanluis"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false
            if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release").apply {
                    storeFile = file(System.getenv("KEYSTORE_FILE")!!)
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEY_PASSWORD")
                }
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true; viewBinding = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.activity)
}

/*
 * Icono: el JPG entregado contiene un fondo gris rectangular. Generamos recursos
 * PNG con alfa y recorte circular antes de compilar, sin alterar la ilustración.
 * El launcher adaptativo aplica su propia máscara; el círculo queda en zona segura.
 */
val generatedIconRes = layout.buildDirectory.dir("generated/cleanLauncherIcon/res")
android.sourceSets.getByName("main").res.srcDir(generatedIconRes)

val generateCleanLauncherIcon = tasks.register("generateCleanLauncherIcon") {
    val source = layout.projectDirectory.file("src/main/res/drawable-nodpi/icon_new.jpg")
    inputs.file(source)
    outputs.dir(generatedIconRes)
    doLast {
        val original = ImageIO.read(source.asFile)
        val side = minOf(original.width, original.height)
        val sx = (original.width - side) / 2
        val sy = (original.height - side) / 2
        val destination = generatedIconRes.get().asFile.resolve("drawable-nodpi")
        destination.mkdirs()
        listOf("icon_clean_foreground" to 0.64, "icon_clean_legacy" to 0.94).forEach { (name, fraction) ->
            val size = 512
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val diameter = (size * fraction).toInt()
            val offset = (size - diameter) / 2
            graphics.clip = Ellipse2D.Double(offset.toDouble(), offset.toDouble(), diameter.toDouble(), diameter.toDouble())
            // El círculo original termina antes del margen gris del JPG.
            val crop = (side * 0.80).toInt()
            val cropOffset = (side - crop) / 2
            graphics.drawImage(original, offset, offset, offset + diameter, offset + diameter,
                sx + cropOffset, sy + cropOffset, sx + cropOffset + crop, sy + cropOffset + crop, null)
            graphics.dispose()
            ImageIO.write(image, "png", destination.resolve("$name.png"))
        }
    }
}
tasks.named("preBuild") { dependsOn(generateCleanLauncherIcon) }
