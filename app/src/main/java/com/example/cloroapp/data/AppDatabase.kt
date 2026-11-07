package com.example.cloroapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Usuario::class,
        UsuarioGranja::class,
        Granja::class,
        PuntoMedicion::class,
        NivelCloro::class,
        Alerta::class,
        AccionCorrectiva::class
    ],
    // Versión 4: rutas de archivos de fotos (fotoRuta) en SQLite para persistencia nativa con FileProvider.
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun granjaDao(): GranjaDao
    abstract fun puntoMedicionDao(): PuntoMedicionDao
    abstract fun nivelCloroDao(): NivelCloroDao
    abstract fun alertaDao(): AlertaDao
    abstract fun accionCorrectivaDao(): AccionCorrectivaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cloro_database"
                )
                    // La base solo contiene datos ficticios: se recrea en vez de migrar desde la v1.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
