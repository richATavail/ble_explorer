plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
}

android {
	namespace = "com.bitwisearts.android.ble"
	compileSdk = 36

	defaultConfig {
		minSdk = 28

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//		consumerProguardFiles = "consumer-rules.pro"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlin {
		jvmToolchain(17)
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.jetbrains.kotlin.reflect)
	implementation(libs.jetbrains.kotlin.coroutines)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
}