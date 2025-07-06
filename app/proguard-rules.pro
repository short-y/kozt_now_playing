# Add project specific ProGuard rules here.
# By default, the flags in this file are applied to all build types.

# ProGuard rules for kotlinx.serialization
-keepclassmembers class kotlinx.serialization.internal.* {
    *;
}
-keep class *$$serializer {
    *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class ** {
    @kotlinx.serialization.Transient <fields>;
}

# ProGuard rules for Coil
-dontwarn okio.**
-if class coil.disk.DiskLruCache
-keep class coil.disk.DiskLruCache { *; }
-if class coil.RealImageLoader
-keep class coil.RealImageLoader { *; }
-if class coil.memory.MemoryCache
-keep class coil.memory.MemoryCache { *; }
-if class coil.request.RequestService
-keep class coil.request.RequestService { *; }
-if class coil.target.ViewTarget
-keep class coil.target.ViewTarget { *; }
-if class coil.transition.TransitionTarget
-keep class coil.transition.TransitionTarget { *; }
-if class coil.util.CoilUtils
-keep class coil.util.CoilUtils { *; }
-if class coil.util.Lifecycles
-keep class coil.util.Lifecycles { *; }
-if class coil.util.Requests
-keep class coil.util.Requests { *; }
-if class coil.util.SystemCallbacks
-keep class coil.util.SystemCallbacks { *; }
-if class coil.util.Utils
-keep class coil.util.Utils { *; }
