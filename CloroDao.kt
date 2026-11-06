package com.example.cloroapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun obtenerPorCorreo(correo: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(usuario: Usuario): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun asignarGranja(asignacion: UsuarioGranja)

    @Query(
        """SELECT g.* FROM granjas g
           INNER JOIN usuario_granja ug ON ug.granjaId = g.id
           WHERE ug.usuarioId = :usuarioId
           ORDER BY g.nombre"""
    )
    suspend fun obtenerGranjasAsignadas(usuarioId: Long): List<Granja>

    @Query("SELECT COUNT(*) FROM usuario_granja WHERE usuarioId = :usuarioId AND granjaId = :granjaId")
    suspend fun contarAsignacion(usuarioId: Long, granjaId: Long): Int
}

@Dao
interface GranjaDao {
    @Query("SELECT * FROM granjas ORDER BY nombre")
    suspend fun obtenerTodas(): List<Granja>

    @Query("SELECT * FROM granjas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Granja?

    @Query("SELECT * FROM granjas WHERE nombre = :nombre LIMIT 1")
    suspend fun obtenerPorNombre(nombre: String): Granja?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(granja: Granja): Long
}

@Dao
interface PuntoMedicionDao {
    @Query("SELECT * FROM puntos_medicion WHERE id = :id")
    suspend fun obtenerPorId(id: Long): PuntoMedicion?

    @Query("SELECT * FROM puntos_medicion WHERE codigo = :codigo LIMIT 1")
    suspend fun obtenerPorCodigo(codigo: String): PuntoMedicion?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(punto: PuntoMedicion): Long

    /** Puntos visibles para el usuario (todos para la jefatura; solo los de sus granjas para los demás). */
    @Query(
        """SELECT p.id AS puntoId, p.codigo AS codigo, p.galpon AS galpon, p.tipo AS tipo,
                  p.ubicacion AS ubicacion, g.id AS granjaId, g.nombre AS granjaNombre,
                  n.valorPpm AS valorPpm, n.estado AS estado, n.fechaHoraMillis AS fechaHoraMillis
           FROM puntos_medicion p
           INNER JOIN granjas g ON g.id = p.granjaId
           LEFT JOIN niveles_cloro n ON n.id = (
               SELECT id FROM niveles_cloro
               WHERE puntoMedicionId = p.id
               ORDER BY fechaHoraMillis DESC, id DESC LIMIT 1)
           WHERE (:todas = 1 OR p.granjaId IN (SELECT granjaId FROM usuario_granja WHERE usuarioId = :usuarioId))
           ORDER BY g.nombre, p.codigo"""
    )
    fun observarResumen(todas: Boolean, usuarioId: Long): Flow<List<PuntoResumen>>

    @Query(
        """SELECT p.id AS puntoId, p.codigo AS codigo, p.galpon AS galpon, p.tipo AS tipo,
                  p.ubicacion AS ubicacion, g.id AS granjaId, g.nombre AS granjaNombre,
                  n.valorPpm AS valorPpm, n.estado AS estado, n.fechaHoraMillis AS fechaHoraMillis
           FROM puntos_medicion p
           INNER JOIN granjas g ON g.id = p.granjaId
           LEFT JOIN niveles_cloro n ON n.id = (
               SELECT id FROM niveles_cloro
               WHERE puntoMedicionId = p.id
               ORDER BY fechaHoraMillis DESC, id DESC LIMIT 1)
           WHERE p.id = :puntoId"""
    )
    fun observarResumenPunto(puntoId: Long): Flow<PuntoResumen?>
}

@Dao
interface NivelCloroDao {
    /** Las mediciones no se modifican ni se eliminan: solo se insertan. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(medicion: NivelCloro): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodas(mediciones: List<NivelCloro>): List<Long>

    @Query("SELECT * FROM niveles_cloro WHERE id = :id")
    suspend fun obtenerPorId(id: Long): NivelCloro?

    @Query("SELECT COUNT(*) FROM niveles_cloro")
    suspend fun contar(): Int

    @Query(
        """SELECT * FROM niveles_cloro WHERE puntoMedicionId = :puntoId
           ORDER BY fechaHoraMillis DESC, id DESC LIMIT 1"""
    )
    suspend fun obtenerUltima(puntoId: Long): NivelCloro?

    @Query(
        """SELECT * FROM niveles_cloro
           WHERE puntoMedicionId = :puntoId AND fechaHoraMillis >= :desde
           ORDER BY fechaHoraMillis DESC"""
    )
    fun observarHistorial(puntoId: Long, desde: Long): Flow<List<NivelCloro>>

    @Query("SELECT * FROM niveles_cloro WHERE sincronizado = 0 ORDER BY fechaHoraMillis")
    suspend fun obtenerPendientes(): List<NivelCloro>

    @Query("SELECT COUNT(*) FROM niveles_cloro WHERE sincronizado = 0")
    fun observarCantidadPendientes(): Flow<Int>

    @Query("UPDATE niveles_cloro SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizada(id: Long)

    @Query(
        """SELECT COUNT(*) FROM niveles_cloro
           WHERE origen = 'MANUAL' AND registradoPor = :nombre AND fechaHoraMillis >= :desde"""
    )
    fun observarControlesManuales(nombre: String, desde: Long): Flow<Int>

    @Query(
        """SELECT p.id AS puntoId, p.codigo AS codigo, p.galpon AS galpon,
                  g.id AS granjaId, g.nombre AS granjaNombre,
                  AVG(n.valorPpm) AS promedio, MIN(n.valorPpm) AS minimo, MAX(n.valorPpm) AS maximo,
                  COUNT(n.id) AS total,
                  SUM(CASE WHEN n.estado <> 'NORMAL' THEN 1 ELSE 0 END) AS desviaciones
           FROM puntos_medicion p
           INNER JOIN granjas g ON g.id = p.granjaId
           INNER JOIN niveles_cloro n ON n.puntoMedicionId = p.id
           WHERE n.fechaHoraMillis >= :desde
             AND (:todas = 1 OR p.granjaId IN (SELECT granjaId FROM usuario_granja WHERE usuarioId = :usuarioId))
           GROUP BY p.id
           ORDER BY g.nombre, p.codigo"""
    )
    fun observarEstadisticas(desde: Long, todas: Boolean, usuarioId: Long): Flow<List<EstadisticaPunto>>
}

@Dao
interface AlertaDao {
    @Insert
    suspend fun insertar(alerta: Alerta): Long

    @Update
    suspend fun actualizar(alerta: Alerta)

    @Query("SELECT * FROM alertas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Alerta?

    /** Solo hay una alerta pendiente por punto: las nuevas desviaciones se suman a ella. */
    @Query(
        """SELECT * FROM alertas
           WHERE puntoMedicionId = :puntoId AND estado <> 'CERRADA'
           ORDER BY fechaHoraMillis DESC LIMIT 1"""
    )
    suspend fun obtenerPendienteDelPunto(puntoId: Long): Alerta?

    @Query(
        """SELECT a.*, p.codigo AS codigoPunto, p.galpon AS galpon, g.id AS granjaId, g.nombre AS granjaNombre
           FROM alertas a
           INNER JOIN puntos_medicion p ON p.id = a.puntoMedicionId
           INNER JOIN granjas g ON g.id = p.granjaId
           WHERE (:soloPendientes = 0 OR a.estado <> 'CERRADA')
             AND (:todas = 1 OR p.granjaId IN (SELECT granjaId FROM usuario_granja WHERE usuarioId = :usuarioId))
           ORDER BY a.fechaHoraMillis DESC"""
    )
    fun observarAlertas(soloPendientes: Boolean, todas: Boolean, usuarioId: Long): Flow<List<AlertaDetalle>>

    @Query(
        """SELECT a.*, p.codigo AS codigoPunto, p.galpon AS galpon, g.id AS granjaId, g.nombre AS granjaNombre
           FROM alertas a
           INNER JOIN puntos_medicion p ON p.id = a.puntoMedicionId
           INNER JOIN granjas g ON g.id = p.granjaId
           WHERE a.id = :alertaId"""
    )
    fun observarAlerta(alertaId: Long): Flow<AlertaDetalle?>

    @Query(
        """SELECT COUNT(*) FROM alertas a
           INNER JOIN puntos_medicion p ON p.id = a.puntoMedicionId
           WHERE a.estado <> 'CERRADA'
             AND (:todas = 1 OR p.granjaId IN (SELECT granjaId FROM usuario_granja WHERE usuarioId = :usuarioId))"""
    )
    fun observarCantidadPendientes(todas: Boolean, usuarioId: Long): Flow<Int>
}

@Dao
interface AccionCorrectivaDao {
    @Insert
    suspend fun insertar(accion: AccionCorrectiva): Long

    @Query("SELECT * FROM acciones_correctivas WHERE alertaId = :alertaId ORDER BY fechaHoraMillis DESC")
    fun observarPorAlerta(alertaId: Long): Flow<List<AccionCorrectiva>>

    @Query("SELECT MAX(fechaHoraMillis) FROM acciones_correctivas WHERE alertaId = :alertaId")
    suspend fun obtenerFechaUltimaAccion(alertaId: Long): Long?

    @Query("UPDATE acciones_correctivas SET estadoResolucion = 'VALIDADA' WHERE alertaId = :alertaId")
    suspend fun validarTodas(alertaId: Long)
}
