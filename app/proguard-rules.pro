# Release R8 keep rules (BDK JNI, Web3j crypto, Retrofit, Kotlinx Serialization, Hilt).
# AGP 9.3 optimization DSL includes this file via src/main/keepRules/proguard-rules.keep.

-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-renamesourcefileattribute SourceFile

# JNI / UniFFI (BDK)
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.bitcoindevkit.** { *; }
-keep class uniffi.** { *; }

# Web3j crypto (mnemonic wordlist, BIP-32, EIP-1559 signing)
-keep class org.web3j.** { *; }
-keepclassmembers class org.web3j.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.web3j.**

# Retrofit service methods (reflection on annotations)
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# Kotlinx Serialization (JSON DTOs + Compose Navigation @Serializable routes)
-keepattributes *Annotation*
-keep,includedescriptorclasses class network.bahn.androidcryptowallet.**$$serializer { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Hilt / Dagger (consumer rules usually suffice; keep generated components)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn com.google.errorprone.annotations.**

# EncryptedSharedPreferences / Tink (optional KeysDownloader deps are not on the classpath)
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
