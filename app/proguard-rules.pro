# Apache POI and XML libraries use reflection for a few optional integrations.
# Keep the app-facing Excel import/export paths stable while R8 removes unused code.
-keep class com.example.flightlog.data.export.ExcelExporter { *; }
-keep class com.example.flightlog.data.db.ExcelImporter { *; }
-dontwarn org.apache.**
-dontwarn org.openxmlformats.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn schemasMicrosoftComOfficeExcel.**
-dontwarn com.github.virtuald.**
