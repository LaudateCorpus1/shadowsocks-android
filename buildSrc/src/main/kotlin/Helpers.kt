
import com.android.build.VariantOutput
import com.android.build.gradle.AbstractAppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import java.util.*

const val lifecycleVersion = "2.4.0-beta01"

private val Project.android get() = extensions.getByName<BaseExtension>("android")

private val flavorRegex = "(assemble|generate)\\w*(Release|Debug)".toRegex()
val Project.currentFlavor get() = gradle.startParameter.taskRequests.toString().let { task ->
    flavorRegex.find(task)?.groupValues?.get(2)?.toLowerCase(Locale.ROOT) ?: "debug".also {
        println("Warning: No match found for $task")
    }
}

fun Project.setupCommon() {
    android.apply {
        buildToolsVersion("31.0.0")
        compileSdkVersion(31)
        defaultConfig {
            minSdk = 23
            targetSdk = 31
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        val javaVersion = JavaVersion.VERSION_11
        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        lintOptions {
            warning("ExtraTranslation")
            warning("ImpliedQuantity")
            informational("MissingTranslation")
        }
        (this as ExtensionAware).extensions.getByName<KotlinJvmOptions>("kotlinOptions").jvmTarget =
                javaVersion.toString()
    }

    dependencies {
        add("testImplementation", "junit:junit:4.13.2")
        add("androidTestImplementation", "androidx.test:runner:1.4.0")
        add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.4.0")
    }
}

fun Project.setupCore() {
    setupCommon()
    android.apply {
        defaultConfig {
            versionCode = 5020650
            versionName = "5.2.6-nightly"
        }
        compileOptions.isCoreLibraryDesugaringEnabled = true
        lintOptions {
            disable("BadConfigurationProvider")
            warning("RestrictedApi")
            disable("UseAppTint")
        }
        ndkVersion = "21.4.7075529"
    }
    dependencies.add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs:1.1.5")
}

private val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)
fun Project.setupApp() {
    setupCore()

    android.apply {
        defaultConfig.resourceConfigurations.addAll(listOf(
            "ar",
            "es",
            "fa",
            "fr",
            "ja",
            "ko",
            "ru",
            "tr",
            "zh-rCN",
            "zh-rTW",
        ))
        buildTypes {
            getByName("debug") {
                isPseudoLocalesEnabled = true
            }
            getByName("release") {
                isShrinkResources = true
                isMinifyEnabled = true
                proguardFile(getDefaultProguardFile("proguard-android.txt"))
            }
        }
        lintOptions.disable("RemoveWorkManagerInitializer")
        packagingOptions {
            excludes.add("**/*.kotlin_*")
            jniLibs.useLegacyPackaging = true
        }
        splits.abi {
            isEnable = true
            isUniversalApk = true
        }
    }

    dependencies.add("implementation", project(":core"))

    if (currentFlavor == "release") (android as AbstractAppExtension).applicationVariants.all {
        for (output in outputs) {
            abiCodes[(output as ApkVariantOutputImpl).getFilter(VariantOutput.ABI)]?.let { offset ->
                output.versionCodeOverride = versionCode + offset
            }
        }
    }
}
