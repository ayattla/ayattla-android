# Media3 session service is referenced from the manifest.
-keep class com.ayattla.app.audio.PlaybackService { *; }

# WorkManager (used by Glance widgets) creates its Room database by reflection.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.** extends androidx.room.RoomDatabase { *; }
# Glance widget receiver and workers are referenced by name.
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

-keep class com.ayattla.app.crash.CrashActivity
