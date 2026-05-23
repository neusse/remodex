-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# sshj pulls in net.i2p EdDSA code that references a JDK-internal verifier type.
# Android does not provide that class, and the optional path is not used here.
-dontwarn sun.security.x509.X509Key
