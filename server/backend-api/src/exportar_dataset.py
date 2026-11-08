"""
Genera <storage>/dataset/ con las etiquetas de los clips autorizados.

Uso (desde el contenedor de backend-api):
    python exportar_dataset.py

Por ahora NO copia videos: falta el paso de anonimización. Ver README.md generado.
"""
from config.configDb import SessionLocal, init_db
from services.dataset_service import exportar_dataset


def main():
    # Crea las tablas que falten (p. ej. las del dataset si la API aún no se reinició con el
    # código nuevo). Es idempotente: no toca las que ya existen.
    init_db()
    db = SessionLocal()
    try:
        r = exportar_dataset(db)
    finally:
        db.close()

    print(f"Dataset en {r['destino']}")
    print(f"  clips: {r['clips']} · eventos: {r['eventos']} · conductores: {r['conductores']}")
    if r["sin_anonimizar"]:
        print(f"  sin video copiado (falta anonimizar): {r['sin_anonimizar']}")
    if r["clips_excluidos"]:
        print(f"  clips excluidos a mano: {r['clips_excluidos']}")
    e = r["excluidos"]
    print("  reportes excluidos -> "
          f"no aprobados: {e['no_aprobado']} · de prueba: {e['prueba']} · "
          f"sin consentimiento vigente: {e['sin_consentimiento']} · sin video en disco: {e['sin_video']}")


if __name__ == "__main__":
    main()