-keep class com.lokalpos.app.data.entity.** { *; }
-keepclassmembers class com.lokalpos.app.data.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn java.lang.invoke.StringConcatFactory

-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class javax.activation.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn javax.activation.**

-keep class com.lokalpos.app.data.dao.AggregatedSalesItem { *; }
-keep class com.lokalpos.app.data.dao.DailySales { *; }
-keep class com.lokalpos.app.data.dao.PaymentMethodSummary { *; }
-keep class com.lokalpos.app.data.dao.CategorySales { *; }
