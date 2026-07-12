from analizadores.base import AnalizadorBase


class AnalizadorAccidentes(AnalizadorBase):
    """
    Capa de clasificacion de severidad de accidente (YOLOv8s-cls, 3 clases).

    Si backend-ia/modelos/accidentes/metadata.json no existe todavia
    (el modelo de accidentes se entreno antes de que definieramos esta
    convencion), se usa un orden de respaldo. Ese orden de respaldo
    puede estar mal si el modelo real no quedo entrenado en ese mismo
    orden alfabetico exacto: conviene generar el metadata.json real
    para este modelo tambien, no solo confiar en este respaldo.
    """

    nombre = "accidentes"
    CLASES_RESPALDO = ["grave", "leve", "moderada"]  # orden alfabetico, solo como respaldo

    def analizar_frames(self, frames) -> dict:
        clases = self.metadata.get("clases") or self.CLASES_RESPALDO

        # Se guarda, por cada clase, la confianza mas alta vista en cualquiera
        # de los frames muestreados (un accidente puede aparecer en un solo
        # frame puntual, promediar con el resto lo diluiria).
        mejor_confianza_por_clase = {c: 0.0 for c in clases}

        for frame in frames:
            resultado = self.modelo.predict(frame, verbose=False)[0]
            probs = resultado.probs.data.tolist()
            for clase, prob in zip(clases, probs):
                if prob > mejor_confianza_por_clase[clase]:
                    mejor_confianza_por_clase[clase] = prob

        clase_top = max(mejor_confianza_por_clase, key=mejor_confianza_por_clase.get)
        confianza_top = mejor_confianza_por_clase[clase_top]

        return {
            "resultado": clase_top,
            "confianza": round(confianza_top, 4),
        }
