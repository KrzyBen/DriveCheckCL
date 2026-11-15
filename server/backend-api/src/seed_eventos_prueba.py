import argparse
import random
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from config.configDb import SessionLocal
from entity.user_entity import User
from entity.reporte_entity import Reporte, VideoReporte, EstadoReporte, Infraccion

PREFIJO = "[PRUEBA]"
CHILE = ZoneInfo("America/Santiago")

# (zona, lat, lon, peso, dispersión en grados, perfil horario)
ZONAS = [
    ("Concepción centro", -36.8270, -73.0503, 18, 0.004, "urbano"),
    ("Talcahuano", -36.7167, -73.1167, 8, 0.005, "urbano"),
    ("San Pedro de la Paz", -36.8400, -73.1000, 8, 0.005, "urbano"),
    ("Chiguayante", -36.9167, -73.0167, 5, 0.004, "urbano"),
    ("Coronel", -37.0167, -73.1500, 5, 0.004, "urbano"),
    ("Lota", -37.0833, -73.1583, 3, 0.003, "urbano"),
    ("Los Ángeles", -37.4697, -72.3537, 8, 0.006, "ruta"),
    ("Arauco", -37.2500, -73.3167, 3, 0.004, "urbano"),
    ("Cañete", -37.8000, -73.4167, 2, 0.003, "ruta"),
]
# peso por hora del día (0 a 23)
PERFIL = {
    "urbano": [1, 1, 1, 1, 1, 2, 5, 12, 14, 8, 6, 6, 8, 8, 6, 7, 11, 18, 26, 30, 24, 14, 7, 3],
    "ruta":   [1, 1, 1, 1, 1, 2, 4, 7, 9, 10, 11, 12, 12, 12, 11, 11, 11, 12, 12, 9, 6, 4, 2, 1],
}
SEVERIDADES = (["leve"] * 40) + (["moderada"] * 38) + (["grave"] * 22)


def generar(cantidad: int, rng: random.Random):
    pesos = [z[3] for z in ZONAS]
    hoy = datetime.now(CHILE).date()
    for _ in range(cantidad):
        zona, lat, lon, _p, sigma, perfil = rng.choices(ZONAS, weights=pesos)[0]
        while True:  # día de las últimas 6 semanas; los fines de semana salen la mitad
            dia = hoy - timedelta(days=rng.randint(0, 44))
            if dia.isoweekday() <= 5 or rng.random() < 0.5:
                break
        hora = rng.choices(range(24), weights=PERFIL[perfil])[0]
        momento = datetime(dia.year, dia.month, dia.day, hora, rng.randint(0, 59), tzinfo=CHILE)
        yield {
            "zona": zona,
            "lat": round(rng.gauss(lat, sigma), 6),
            "lon": round(rng.gauss(lon, sigma), 6),
            "momento": momento,
            "severidad": rng.choice(SEVERIDADES),
        }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cantidad", nargs="?", type=int, default=60)
    ap.add_argument("--borrar", action="store_true")
    ap.add_argument("--usuario", type=int, default=None, help="id del usuario dueño de los reportes")
    args = ap.parse_args()

    db = SessionLocal()
    try:
        if args.borrar:
            reportes = db.query(Reporte).filter(Reporte.titulo.like(f"{PREFIJO}%")).all()
            for r in reportes:
                db.delete(r)
            db.commit()
            print(f"Borrados {len(reportes)} reportes de prueba.")
            return

        q = db.query(User)
        usuario = q.filter(User.id == args.usuario).first() if args.usuario else q.order_by(User.id).first()
        if not usuario:
            print("No hay usuarios: crea uno primero (o pasa --usuario ID).")
            return
        admin = db.query(User).filter(User.rol == "administrador").order_by(User.id).first()

        catalogo = db.query(Infraccion).filter(Infraccion.articulo != "Manual").order_by(Infraccion.id).all()
        rng = random.Random(7)
        eventos = list(generar(args.cantidad, random.Random(42)))
        for ev in eventos:
            r = Reporte(
                usuario_id=usuario.id,
                titulo=f"{PREFIJO} {ev['zona']}",
                comentario="Dato de prueba para el mapa de calor",
                estado=EstadoReporte.aprobado,
                severidad_validada=ev["severidad"],
                validado_por_id=admin.id if admin else None,
                finalizado_at=ev["momento"],
                created_at=ev["momento"],
                updated_at=ev["momento"],
            )
            r.videos.append(VideoReporte(
                video_path="prueba/sin_video.mp4", orden=1,
                lat=ev["lat"], lon=ev["lon"], grabado_en=ev["momento"],
            ))
            if catalogo:  # 1 o 2 infracciones del catálogo, para poder probar el filtro por tipo
                r.infracciones = rng.sample(catalogo, k=min(len(catalogo), rng.choice([1, 1, 2])))
            db.add(r)
        db.commit()
        print(f"Creados {len(eventos)} reportes aprobados de prueba (usuario id={usuario.id}).")
        print("Para quitarlos: python seed_eventos_prueba.py --borrar")
    finally:
        db.close()


if __name__ == "__main__":
    main()
