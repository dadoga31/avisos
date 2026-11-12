package io.github.dadoga31.avisos.datos

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class Conversores {
    @TypeConverter
    fun notasATexto(notas: List<Nota>): String {
        val a = JSONArray()
        notas.forEach { a.put(JSONObject().put("id", it.id).put("ts", it.ts).put("texto", it.texto)) }
        return a.toString()
    }

    @TypeConverter
    fun textoANotas(texto: String?): List<Nota> {
        if (texto.isNullOrBlank()) return emptyList()
        val a = JSONArray(texto)
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Nota(o.optString("id"), o.optString("ts"), o.optString("texto"))
        }
    }
}

@Dao
interface AvisosDao {
    @Query("SELECT * FROM avisos")
    fun todos(): Flow<List<Aviso>>

    @Query("SELECT * FROM avisos")
    suspend fun todosAhora(): List<Aviso>

    @Query("SELECT * FROM avisos WHERE id = :id")
    suspend fun porId(id: String): Aviso?

    @Query("SELECT COUNT(*) FROM avisos WHERE correoId = :correoId AND correoId != ''")
    suspend fun cuantosConCorreo(correoId: String): Int

    @Upsert
    suspend fun guardar(aviso: Aviso)

    @Upsert
    suspend fun guardarVarios(avisos: List<Aviso>)

    @Delete
    suspend fun borrar(aviso: Aviso)

    @Query("DELETE FROM avisos")
    suspend fun borrarTodos()

    @Query("SELECT * FROM tecnicos ORDER BY CASE WHEN id = 'yo' THEN 0 ELSE 1 END, nombre COLLATE NOCASE")
    fun tecnicos(): Flow<List<Tecnico>>

    @Query("SELECT * FROM tecnicos")
    suspend fun tecnicosAhora(): List<Tecnico>

    @Upsert
    suspend fun guardarTecnico(tecnico: Tecnico)

    @Delete
    suspend fun borrarTecnico(tecnico: Tecnico)

    @Query("UPDATE avisos SET asignadoA = '' WHERE asignadoA = :id")
    suspend fun desasignar(id: String)
}

@Database(entities = [Aviso::class, Tecnico::class], version = 1, exportSchema = true)
@TypeConverters(Conversores::class)
abstract class BaseDatos : RoomDatabase() {
    abstract fun dao(): AvisosDao

    companion object {
        @Volatile private var instancia: BaseDatos? = null

        fun de(contexto: Context): BaseDatos = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(contexto.applicationContext, BaseDatos::class.java, "avisos.db")
                .build().also { instancia = it }
        }

        /** Para las pruebas: base en memoria. */
        fun enMemoria(contexto: Context): BaseDatos =
            Room.inMemoryDatabaseBuilder(contexto, BaseDatos::class.java).allowMainThreadQueries().build()
    }
}
