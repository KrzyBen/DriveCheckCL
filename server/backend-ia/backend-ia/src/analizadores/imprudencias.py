from analizadores.base import AnalizadorBase

UMBRAL_CONFIANZA = 0.4


class AnalizadorImprudencias(AnalizadorBase):
    """
    Capa de deteccion de elementos de control de transito (semaforos,
    senales, vehiculos - 23 clases).

    Importante: este modelo detecta elementos presentes en el frame, no
    determina por si solo una infraccion. La correlacion que se hace
    aca (mas abajo) es una primera version simple, pensada para poder
    probar el flujo completo ahora mismo, no la version final. Con el
    tiempo se puede reemplazar por logica mas precisa (por ejemplo,
    seguir la posicion del vehiculo respecto a la linea de detencion)
    sin tener que tocar el resto del pipeline.
    """

    nombre = "imprudencias"

    def analizar_frames(self, frames) -> dict:
        clases = self.metadata.get("clases", [])

        # Por cada clase, guarda la confianza maxima vista y en que
        # frames (por indice) aparecio, para poder razonar sobre
        # persistencia en el tiempo (ver "luz roja" mas abajo).
        detecciones_por_clase = {}

        for idx_frame, frame in enumerate(frames):
            resultado = self.modelo.predict(frame, verbose=False)[0]
            for caja in resultado.boxes:
                indice_clase = int(caja.cls[0])
                confianza = float(caja.conf[0])
                if confianza < UMBRAL_CONFIANZA:
                    continue

                nombre_clase = clases[indice_clase] if indice_clase < len(clases) else str(indice_clase)
                entrada = detecciones_por_clase.setdefault(nombre_clase, {"confianza_max": 0.0, "frames": []})
                entrada["frames"].append(idx_frame)
                entrada["confianza_max"] = max(entrada["confianza_max"], confianza)

        elementos_detectados = {
            clase: {"confianza": round(datos["confianza_max"], 4), "apariciones": len(datos["frames"])}
            for clase, datos in detecciones_por_clase.items()
        }

        # Heuristica v1: si "red light" aparece en frames que abarcan un
        # tramo real del clip (no solo un instante), es una senal de que
        # el vehiculo siguio avanzando con el semaforo en rojo en vez de
        # detenerse. Es una aproximacion, no una confirmacion definitiva:
        # el admin sigue siendo quien decide si es una infraccion real.
        resultado_final = "sin_evidencia"
        confianza_final = 0.0

        luz_roja = detecciones_por_clase.get("red light")
        if luz_roja and len(luz_roja["frames"]) >= 2:
            frames_de_aparicion = luz_roja["frames"]
            if max(frames_de_aparicion) - min(frames_de_aparicion) >= len(frames) * 0.2:
                resultado_final = "posible_no_respeto_luz_roja"
                confianza_final = luz_roja["confianza_max"]

        if resultado_final == "sin_evidencia" and elementos_detectados:
            resultado_final = "senales_detectadas"
            confianza_final = max(d["confianza"] for d in elementos_detectados.values())

        return {
            "resultado": resultado_final,
            "confianza": round(confianza_final, 4),
            "elementos_detectados": elementos_detectados,
        }
