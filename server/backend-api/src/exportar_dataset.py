"""
Genera <storage>/dataset/ con las etiquetas y los videos anonimizados de los clips autorizados.

Uso (desde el contenedor de backend-api):
    python exportar_dataset.py               # anonimiza cada clip con backend-ia
    python exportar_dataset.py --sin-videos  # solo etiquetas, sin copiar ningún video

Cada clip se anonimiza con backend-ia (POST /anonimizar): puede tardar varios minutos por
clip. Un clip que falla se omite (nunca se copia el original). Si backend-ia no está
disponible o fallan todos, la exportación se aborta y el dataset anterior queda intacto.
"""
import argparse
import sys

from config.configDb import SessionLocal, init_db
from helpers.anonimizador_client import ClienteAnonimizador
from services.dataset_service import ExportacionAbortada, ExportacionEnCurso, exportar_dataset


def main():
    parser = argparse.ArgumentParser(description="Genera el dataset de clips autorizados.")
    parser.add_argument("--sin-videos", action="store_true",
                        help="no anonimiza ni copia videos; solo escribe las etiquetas")
    args = parser.parse_args()

    # Crea las tablas que falten (p. ej. las del dataset si la API aún no se reinició con el
    # código nuevo). Es idempotente: no toca las que ya existen.
    init_db()
    db = SessionLocal()
    anonimizador = None if args.sin_videos else ClienteAnonimizador()
    try:
        r = exportar_dataset(db, anonimizador=anonimizador)
    except ExportacionEnCurso as error:
        print(f"No se exportó: {error}")
        sys.exit(1)
    except ExportacionAbortada as error:
        print(f"Exportación abortada ({error.motivo}): {error}")
        print("El dataset anterior quedó intacto.")
        sys.exit(1)
    finally:
        if anonimizador is not None:
            anonimizador.close()
        db.close()

    print(f"Dataset en {r['destino']}")
    print(f"  clips: {r['clips']} · eventos: {r['eventos']} · conductores: {r['conductores']}")
    if r["sin_anonimizar"]:
        print(f"  sin video copiado (exportación --sin-videos): {r['sin_anonimizar']}")
    omitidos = r["omitidos"]
    if omitidos["total"]:
        print(f"  omitidos (no se pudieron anonimizar): {omitidos['total']}")
        for o in omitidos["clips"]:
            print(f"    {o['clip_id']}: {o['motivo']}")
    if r["clips_excluidos"]:
        print(f"  clips excluidos a mano: {r['clips_excluidos']}")
    e = r["excluidos"]
    print("  reportes excluidos -> "
          f"no aprobados: {e['no_aprobado']} · de prueba: {e['prueba']} · "
          f"sin consentimiento vigente: {e['sin_consentimiento']} · sin video en disco: {e['sin_video']}")


if __name__ == "__main__":
    main()
