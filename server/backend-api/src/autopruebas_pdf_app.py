"""
Autopruebas de GET /api/app/reporte/{id}/pdf (PDF del informe para la app, con Bearer).

Usa la app real con TestClient y la autenticación real (JWT firmados para usuarios de
prueba). Igual que autopruebas_usuarios.py, todo corre dentro de UNA transacción externa que
se deshace al final, con datos propios y un storage temporal: no se tocan los usuarios 1, 2, 3
ni los reportes 67, 68, 69, ni el storage real.

Uso (desde el contenedor de backend-api, en src/):
    python autopruebas_pdf_app.py
"""
import os
import sys
import tempfile
from datetime import datetime, timedelta, timezone

from fastapi.testclient import TestClient
from jose import jwt

from config.configDb import engine, get_db
from config.configEnv import ACCESS_TOKEN_SECRET, ACCESS_TOKEN_ALGORITHM
from entity.reporte_entity import Reporte, EstadoReporte
from helpers import storage_helper
from main import app
from autopruebas_usuarios import (
    REPORTES_PROTEGIDOS, crear_usuario, foto_protegida, nueva_sesion, proteger,
)

PDF = b"%PDF-1.4\n% PDF de autoprueba\n" + b"x" * 2000 + b"\n%%EOF\n"
FALLOS = []


def comprobar(condicion, nombre):
    print(f"  [{'OK' if condicion else 'FALLA'}] {nombre}")
    if not condicion:
        FALLOS.append(nombre)


def token_de(email, minutos=5):
    """JWT igual al que emite el login (verify_token solo mira el email y la expiración)."""
    vence = datetime.now(timezone.utc) + timedelta(minutes=minutos)
    return jwt.encode({"email": email, "exp": vence}, ACCESS_TOKEN_SECRET, algorithm=ACCESS_TOKEN_ALGORITHM)


def con_token(email, minutos=5):
    return {"Authorization": f"Bearer {token_de(email, minutos)}"}


def crear_reporte_con_pdf(db, storage, usuario, con_pdf=True, archivo=True):
    """Reporte aprobado; con_pdf=pdf_path en la base, archivo=el PDF existe en el storage."""
    reporte = Reporte(usuario_id=usuario.id, titulo="Reporte de autoprueba",
                      estado=EstadoReporte.aprobado if con_pdf else EstadoReporte.recibido)
    db.add(reporte)
    db.flush()
    proteger(reporte.id, REPORTES_PROTEGIDOS, "reporte")
    if con_pdf:
        reporte.pdf_path = f"usuario_{usuario.id}/reporte_{reporte.id}/informe.pdf"
        if archivo:
            absoluta = os.path.join(storage, reporte.pdf_path)
            os.makedirs(os.path.dirname(absoluta), exist_ok=True)
            with open(absoluta, "wb") as salida:
                salida.write(PDF)
    db.flush()
    return reporte.id


def pruebas(conn, storage):
    with nueva_sesion(conn) as db:
        dueno = crear_usuario(db)
        otro = crear_usuario(db)
        con_pdf = crear_reporte_con_pdf(db, storage, dueno)
        sin_pdf = crear_reporte_con_pdf(db, storage, dueno, con_pdf=False)
        pdf_perdido = crear_reporte_con_pdf(db, storage, dueno, archivo=False)
        email_dueno, email_otro = dueno.email, otro.email
        inexistente = con_pdf + 100_000
        db.commit()

    cliente = TestClient(app)  # sin "with": no corre el lifespan (no siembra la base)
    url = f"/api/app/reporte/{con_pdf}/pdf"

    print("== 1. El dueño descarga su PDF ==")
    r = cliente.get(url, headers=con_token(email_dueno))
    comprobar(r.status_code == 200 and r.content == PDF, "200 con el contenido exacto del PDF")
    comprobar(r.headers.get("content-type") == "application/pdf"
              and f'filename="informe_{con_pdf}.pdf"' in r.headers.get("content-disposition", ""),
              "Content-Type application/pdf y nombre informe_<id>.pdf")
    r = cliente.get(url, headers={**con_token(email_dueno), "Range": "bytes=0-7"})
    comprobar(r.status_code == 206 and r.content == PDF[:8], "también acepta Range (206)")

    print("== 2. Quien no es el dueño o no tiene sesión ==")
    r = cliente.get(url, headers=con_token(email_otro))
    comprobar(r.status_code == 404 and r.content != PDF, "otro conductor -> 404 (no se revela que existe)")
    r = cliente.get(url)
    # HTTPBearer de FastAPI 0.115 responde 403 si falta el header: es igual en todos los endpoints
    comprobar(r.status_code == 403, f"sin header Authorization -> 403 (comportamiento actual de toda la API): {r.status_code}")
    r = cliente.get(url, headers={"Authorization": "Bearer x.y.z"})
    comprobar(r.status_code == 401, "token inválido -> 401")
    r = cliente.get(url, headers=con_token(email_dueno, minutos=-1))
    comprobar(r.status_code == 401, "token vencido -> 401")

    print("== 3. Reportes sin PDF ==")
    r = cliente.get(f"/api/app/reporte/{sin_pdf}/pdf", headers=con_token(email_dueno))
    comprobar(r.status_code == 404, "reporte propio sin PDF -> 404")
    r = cliente.get(f"/api/app/reporte/{pdf_perdido}/pdf", headers=con_token(email_dueno))
    comprobar(r.status_code == 404, "pdf_path en la base pero sin archivo -> 404")
    r = cliente.get(f"/api/app/reporte/{inexistente}/pdf", headers=con_token(email_dueno))
    comprobar(r.status_code == 404, "reporte inexistente -> 404")

    print("== 4. Orden de rutas ==")
    r = cliente.get(f"/api/app/reporte/{con_pdf}", headers=con_token(email_dueno))
    comprobar(r.status_code == 200 and r.json()["data"]["id"] == con_pdf, "GET /{reporte_id} sigue devolviendo el reporte")
    r = cliente.get("/api/app/reporte/abc/pdf", headers=con_token(email_dueno))
    comprobar(r.status_code == 422, "id no numérico -> 422")


def main():
    antes = foto_protegida()
    raiz_original = storage_helper.STORAGE_ROOT
    conn = engine.connect()
    trans = conn.begin()

    def db_de_prueba():
        # Cada petición usa una sesión sobre la misma conexión (dentro de la transacción externa)
        db = nueva_sesion(conn)
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = db_de_prueba
    try:
        with tempfile.TemporaryDirectory() as storage:
            storage_helper.STORAGE_ROOT = storage
            pruebas(conn, storage)
    finally:
        # Siempre: deshace todo lo que hizo la prueba, aunque haya fallado a la mitad
        app.dependency_overrides.clear()
        trans.rollback()
        conn.close()
        storage_helper.STORAGE_ROOT = raiz_original

    print("== Limpieza y datos protegidos ==")
    comprobar(foto_protegida() == antes, "usuarios 1-3, reportes 67-69 y conteos de tablas sin cambios")

    print("\nTODO OK" if not FALLOS else f"\nFALLARON {len(FALLOS)}: {FALLOS}")
    sys.exit(1 if FALLOS else 0)


if __name__ == "__main__":
    main()
