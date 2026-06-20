package com.drivecheckcl.data.repository

import android.content.Context
import com.drivecheckcl.data.local.LocalStorageManager
import com.drivecheckcl.data.model.EstadoInforme
import com.drivecheckcl.data.model.InformeLocal
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InformeRepository {

    private const val PREFS_NAME  = "drivecheckcl_informes"
    private const val KEY_INFORMES = "informes_list"

    // ── Crear informe local ───────────────────────────────────────────────────

    fun crearInformeLocal(
        context:     Context,
        videoPaths:  List<String>,
        comentario:  String
    ): InformeLocal {
        val id            = LocalStorageManager.generarIdReporte()
        val sdfOut        = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL"))
        val titulo        = "Reporte ${sdfOut.format(Date())}"
        val informesActuales = listarInformes(context)
        val numeracion    = informesActuales.size + 1

        // Mover videos a la carpeta del reporte
        val rutasDestino = videoPaths.mapNotNull { path ->
            LocalStorageManager.moverVideoAReporte(context, path, id)
        }

        val informe = InformeLocal(
            id            = id,
            titulo        = titulo,
            comentario    = comentario,
            videoPaths    = rutasDestino,
            fechaCreacion = System.currentTimeMillis(),
            estado        = EstadoInforme.ENVIADO,
            numeracion    = numeracion,
            pdfDisponible = false
        )

        guardarInforme(context, informe)
        return informe
    }

    // ── CRUD en SharedPreferences ─────────────────────────────────────────────

    fun listarInformes(context: Context): List<InformeLocal> {
        val prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json   = prefs.getString(KEY_INFORMES, "[]") ?: "[]"
        return parsearInformes(json).sortedByDescending { it.fechaCreacion }
    }

    fun actualizarEstado(
        context:      Context,
        informeId:    String,
        nuevoEstado:  EstadoInforme,
        pdfDisponible: Boolean = false
    ) {
        val informes = listarInformes(context).toMutableList()
        val idx = informes.indexOfFirst { it.id == informeId }
        if (idx == -1) return
        informes[idx] = informes[idx].copy(
            estado        = nuevoEstado,
            pdfDisponible = pdfDisponible
        )
        guardarTodos(context, informes)
    }

    fun eliminarInforme(context: Context, informeId: String) {
        val informes = listarInformes(context).filter { it.id != informeId }
        guardarTodos(context, informes)
    }

    // ── Serialización manual (sin Gson en el repo) ────────────────────────────

    private fun guardarInforme(context: Context, informe: InformeLocal) {
        val informes = listarInformes(context).toMutableList()
        informes.add(0, informe)
        guardarTodos(context, informes)
    }

    private fun guardarTodos(context: Context, informes: List<InformeLocal>) {
        val array = JSONArray()
        informes.forEach { array.put(serializarInforme(it)) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_INFORMES, array.toString()).apply()
    }

    private fun serializarInforme(informe: InformeLocal): JSONObject {
        val paths = JSONArray()
        informe.videoPaths.forEach { paths.put(it) }
        return JSONObject().apply {
            put("id",             informe.id)
            put("titulo",         informe.titulo)
            put("comentario",     informe.comentario)
            put("videoPaths",     paths)
            put("fechaCreacion",  informe.fechaCreacion)
            put("estado",         informe.estado.name)
            put("numeracion",     informe.numeracion)
            put("pdfDisponible",  informe.pdfDisponible)
        }
    }

    private fun parsearInformes(json: String): List<InformeLocal> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj   = array.getJSONObject(i)
                val paths = obj.getJSONArray("videoPaths")
                InformeLocal(
                    id            = obj.getString("id"),
                    titulo        = obj.getString("titulo"),
                    comentario    = obj.getString("comentario"),
                    videoPaths    = (0 until paths.length()).map { paths.getString(it) },
                    fechaCreacion = obj.getLong("fechaCreacion"),
                    estado        = EstadoInforme.valueOf(obj.getString("estado")),
                    numeracion    = obj.getInt("numeracion"),
                    pdfDisponible = obj.getBoolean("pdfDisponible")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}