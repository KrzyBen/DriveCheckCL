"""
Autopruebas de delete_user_service contra la base de datos real (PostgreSQL).

Todo corre dentro de UNA transacción externa que se deshace al final (trans.rollback() en el
finally): los commit/rollback del servicio solo afectan a un SAVEPOINT. Los datos son propios
(correos autoprueba_...@drivecheck.test) y nunca se tocan los usuarios 1, 2, 3 ni los
reportes 67, 68, 69: se fotografían antes y se comparan después. Los archivos van a un
storage temporal, así que el storage real (/storage) tampoco se toca.

Uso (desde el contenedor de backend-api, en src/):
    python autopruebas_usuarios.py
"""
import asyncio
import os
import sys
import tempfile
import uuid
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace

from sqlalchemy import text
from sqlalchemy.orm import Session

from config.configDb import engine
from entity.user_entity import User
from entity.reporte_entity import Reporte, VideoReporte, Infraccion, EstadoReporte
from entity.reporte_validacion_entity import ReporteValidacion, EstadoAnalisis
from entity.password_reset_entity import PasswordResetCode
from entity.notificacion_entity import Notificacion
from entity.dataset_clip_entity import DatasetClip
from helpers import storage_helper
from services import user_service as us

USUARIOS_PROTEGIDOS = {1, 2, 3}
REPORTES_PROTEGIDOS = {67, 68, 69}
DOMINIO = "@drivecheck.test"
CONTENIDO_VIDEO = b"VIDEO-DE-AUTOPRUEBA"
FALLOS = []


def comprobar(condicion, nombre):
    print(f"  [{'OK' if condicion else 'FALLA'}] {nombre}")
    if not condicion:
        FALLOS.append(nombre)


def borrar(db, admin, **criterios):
    """Llama al servicio tal como lo hace el controlador."""
    return asyncio.run(us.delete_user_service(db, admin, **criterios))


def nueva_sesion(conn):
    # create_savepoint: el commit/rollback del servicio no cierra la transacción externa
    return Session(bind=conn, join_transaction_mode="create_savepoint", autoflush=False)


def contar(conn, sql, **params):
    """Verificación con SQL directo, para no leer lo que la sesión tenga en caché."""
    return conn.execute(text(sql), params).scalar()


def proteger(id_, protegidos, que):
    # Red de seguridad: si la secuencia entregara un id protegido, se aborta todo
    if id_ in protegidos:
        raise RuntimeError(f"Abortado: el {que} de prueba recibió el id protegido {id_}")


# ── datos propios ──────────────────────────────────────────────────────────

def crear_usuario(db, rol="conductor"):
    marca = uuid.uuid4().hex[:11]
    usuario = User(
        nombre_completo=f"Autoprueba {marca}", rut=f"T{marca}",  # rut: máximo 12 caracteres
        email=f"autoprueba_{marca}{DOMINIO}", rol=rol, password="no-se-usa",
    )
    db.add(usuario)
    db.flush()
    proteger(usuario.id, USUARIOS_PROTEGIDOS, "usuario")
    return usuario


def crear_reporte(db, storage, usuario, estado=EstadoReporte.aprobado, n_videos=2):
    """Reporte con videos en la base y sus archivos reales en el storage temporal."""
    reporte = Reporte(usuario_id=usuario.id, titulo="Reporte de autoprueba", estado=estado)
    db.add(reporte)
    db.flush()
    proteger(reporte.id, REPORTES_PROTEGIDOS, "reporte")
    for orden in range(1, n_videos + 1):
        ruta = f"usuario_{usuario.id}/reporte_{reporte.id}/video_{orden}.mp4"
        db.add(VideoReporte(reporte_id=reporte.id, video_path=ruta, orden=orden))
        absoluta = os.path.join(storage, ruta)
        os.makedirs(os.path.dirname(absoluta), exist_ok=True)
        with open(absoluta, "wb") as archivo:
            archivo.write(CONTENIDO_VIDEO)
    db.flush()
    return reporte


def crear_codigo(db, email):
    db.add(PasswordResetCode(
        email=email, code_hash="no-se-usa",
        expires_at=datetime.now(timezone.utc) + timedelta(minutes=15),
    ))


def carpeta_reporte(storage, usuario_id, reporte_id):
    return os.path.join(storage, f"usuario_{usuario_id}", f"reporte_{reporte_id}")


def carpeta_usuario(storage, usuario_id):
    return os.path.join(storage, f"usuario_{usuario_id}")


# ── foto de lo que no se puede tocar ───────────────────────────────────────

TABLAS = ["usuarios", "reportes", "reporte_videos", "reporte_validacion", "reporte_infracciones",
          "notificaciones", "dataset_clips", "dataset_clip_infracciones", "password_reset_codes",
          "infracciones_catalogo"]


def foto_protegida():
    """Filas protegidas y conteos por tabla, leídos en una conexión aparte."""
    with engine.connect() as c:
        return {
            "usuarios": [tuple(f) for f in c.execute(
                text("SELECT * FROM usuarios WHERE id = ANY(:ids) ORDER BY id"), {"ids": list(USUARIOS_PROTEGIDOS)})],
            "reportes": [tuple(f) for f in c.execute(
                text("SELECT * FROM reportes WHERE id = ANY(:ids) ORDER BY id"), {"ids": list(REPORTES_PROTEGIDOS)})],
            "conteos": {t: c.execute(text(f"SELECT count(*) FROM {t}")).scalar() for t in TABLAS},
        }


# ── casos ──────────────────────────────────────────────────────────────────

def caso_a(conn, storage, admin):
    print("== a. Conductor con 2 reportes: se borra todo lo suyo ==")
    with nueva_sesion(conn) as db:
        conductor = crear_usuario(db)
        otro = crear_usuario(db)
        infraccion = Infraccion(articulo="AUTOPRUEBA", descripcion="Infracción de autoprueba")
        db.add(infraccion)
        db.flush()

        r1 = crear_reporte(db, storage, conductor, EstadoReporte.aprobado)
        r2 = crear_reporte(db, storage, conductor, EstadoReporte.analizando)
        r1.infracciones.append(infraccion)
        db.add(ReporteValidacion(reporte_id=r1.id, estado_analisis=EstadoAnalisis.completado))
        db.add(ReporteValidacion(reporte_id=r2.id, estado_analisis=EstadoAnalisis.analizando))
        db.flush()
        clip = DatasetClip(video_id=r1.videos[0].id, etiquetas_propias=True, severidad="leve")
        clip.infracciones.append(infraccion)
        db.add(clip)
        db.add(Notificacion(usuario_id=conductor.id, reporte_id=r1.id, mensaje="autoprueba"))
        db.add(Notificacion(usuario_id=conductor.id, reporte_id=None, mensaje="autoprueba"))
        # Notificación de OTRO usuario que apunta a un reporte del conductor
        db.add(Notificacion(usuario_id=otro.id, reporte_id=r2.id, mensaje="autoprueba"))
        crear_codigo(db, conductor.email)
        crear_codigo(db, conductor.email)
        crear_codigo(db, otro.email)
        db.commit()

        uid, email, otro_id, otro_email = conductor.id, conductor.email, otro.id, otro.email
        reportes = [r1.id, r2.id]
        clip_id, infraccion_id = clip.id, infraccion.id

        data, error = borrar(db, admin, id=uid)

    comprobar(error is None and data["id"] == uid, "el servicio responde sin error con los datos del usuario")
    comprobar(data and data.get("archivos_no_eliminados") == [], "archivos_no_eliminados vacío")
    comprobar(contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=uid) == 0, "se borró el usuario")
    comprobar(contar(conn, "SELECT count(*) FROM reportes WHERE id = ANY(:r)", r=reportes) == 0, "se borraron sus 2 reportes")
    comprobar(contar(conn, "SELECT count(*) FROM reporte_videos WHERE reporte_id = ANY(:r)", r=reportes) == 0,
              "se borraron sus videos")
    comprobar(contar(conn, "SELECT count(*) FROM reporte_validacion WHERE reporte_id = ANY(:r)", r=reportes) == 0,
              "se borraron sus validaciones")
    comprobar(contar(conn, "SELECT count(*) FROM reporte_infracciones WHERE reporte_id = ANY(:r)", r=reportes) == 0,
              "se borraron sus infracciones (reporte_infracciones)")
    comprobar(contar(conn, "SELECT count(*) FROM notificaciones WHERE usuario_id = :u OR reporte_id = ANY(:r)",
                     u=uid, r=reportes) == 0,
              "se borraron sus notificaciones y la de otro usuario sobre sus reportes")
    comprobar(contar(conn, "SELECT count(*) FROM dataset_clips WHERE id = :c", c=clip_id) == 0
              and contar(conn, "SELECT count(*) FROM dataset_clip_infracciones WHERE clip_id = :c", c=clip_id) == 0,
              "se borró su clip del dataset con sus etiquetas")
    comprobar(contar(conn, "SELECT count(*) FROM password_reset_codes WHERE email = :e", e=email) == 0,
              "se borraron sus códigos de recuperación")
    comprobar(contar(conn, "SELECT count(*) FROM password_reset_codes WHERE email = :e", e=otro_email) == 1
              and contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=otro_id) == 1,
              "el otro usuario y su código siguen existiendo")
    comprobar(contar(conn, "SELECT count(*) FROM infracciones_catalogo WHERE id = :i", i=infraccion_id) == 1,
              "la infracción del catálogo sigue existiendo")
    comprobar(not os.path.exists(carpeta_usuario(storage, uid)),
              "se borraron las carpetas de sus reportes y usuario_N (quedó vacía)")

    print("== a2. Si usuario_N tiene otros archivos, la carpeta se conserva ==")
    with nueva_sesion(conn) as db:
        conductor = crear_usuario(db)
        reporte = crear_reporte(db, storage, conductor, n_videos=1)
        db.commit()
        uid, rid = conductor.id, reporte.id
        suelto = os.path.join(carpeta_usuario(storage, uid), "archivo_suelto.txt")
        with open(suelto, "w", encoding="utf-8") as archivo:
            archivo.write("no es de ningún reporte")
        data, error = borrar(db, admin, id=uid)
    comprobar(error is None and data["archivos_no_eliminados"] == [], "se borra sin error")
    comprobar(not os.path.exists(carpeta_reporte(storage, uid, rid)), "se borró la carpeta del reporte")
    comprobar(os.path.isfile(suelto), "usuario_N se conserva con el archivo suelto")


def caso_b(conn, storage, admin):
    print("== b. Validador que validó reportes de otros ==")
    with nueva_sesion(conn) as db:
        validador = crear_usuario(db, rol="validador")
        conductor = crear_usuario(db)
        reporte = crear_reporte(db, storage, conductor)
        reporte.validado_por_id = validador.id
        db.add(ReporteValidacion(reporte_id=reporte.id, estado_analisis=EstadoAnalisis.completado,
                                 actualizado_por_id=validador.id))
        db.flush()
        clip = DatasetClip(video_id=reporte.videos[0].id, etiquetas_propias=True, editado_por_id=validador.id)
        db.add(clip)
        db.commit()
        vid, cid, rid, clip_id = validador.id, conductor.id, reporte.id, clip.id

        data, error = borrar(db, admin, id=vid)

    comprobar(error is None and data["archivos_no_eliminados"] == [], "el servicio responde sin error")
    comprobar(contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=vid) == 0, "se borró el validador")
    comprobar(contar(conn, "SELECT count(*) FROM reportes WHERE id = :r AND validado_por_id IS NULL", r=rid) == 1,
              "el reporte del otro sigue existiendo con validado_por_id NULL")
    comprobar(contar(conn, "SELECT count(*) FROM reporte_validacion WHERE reporte_id = :r "
                           "AND actualizado_por_id IS NULL", r=rid) == 1,
              "reporte_validacion.actualizado_por_id quedó NULL")
    comprobar(contar(conn, "SELECT count(*) FROM dataset_clips WHERE id = :c AND editado_por_id IS NULL",
                     c=clip_id) == 1,
              "dataset_clips.editado_por_id quedó NULL (FK con SET NULL)")
    comprobar(os.path.isfile(os.path.join(carpeta_reporte(storage, cid, rid), "video_1.mp4")),
              "los archivos del otro conductor siguen intactos")


def caso_c(conn, admin):
    print("== c. Usuario sin reportes ni carpetas ==")
    with nueva_sesion(conn) as db:
        usuario = crear_usuario(db)
        db.commit()
        uid = usuario.id
        data, error = borrar(db, admin, id=uid)
    comprobar(error is None and data["archivos_no_eliminados"] == [], "se borra sin error")
    comprobar(contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=uid) == 0, "el usuario ya no existe")


class ConsultaFalsa:
    def __init__(self, sesion):
        self.sesion = sesion

    def filter(self, *condiciones):
        return self

    def first(self):
        self.sesion.consultas += 1
        return self.sesion.usuario

    def count(self):
        return self.sesion.total_admins


class SesionFalsa:
    """Imita lo mínimo de una Session: registra si el servicio intentó borrar o guardar algo."""

    def __init__(self, usuario, total_admins):
        self.usuario = usuario
        self.total_admins = total_admins
        self.consultas = 0
        self.cambios = []

    def query(self, *modelos):
        return ConsultaFalsa(self)

    def delete(self, objeto):
        self.cambios.append("delete")

    def flush(self):
        self.cambios.append("flush")

    def commit(self):
        self.cambios.append("commit")

    def rollback(self):
        pass


def caso_d(conn, admin):
    print("== d. Protecciones ==")
    with nueva_sesion(conn) as db:
        data, error = borrar(db, admin, id=admin.id)
    comprobar(data is None and "propia cuenta" in (error or ""), "no puede eliminarse a sí mismo")
    comprobar(contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=admin.id) == 1,
              "y su usuario sigue existiendo")

    # Último administrador: sesión falsa para no tocar al único admin real (usuario 1)
    unico = SimpleNamespace(id=10**9, rol="administrador", nombre_completo="x", rut="x", email="x")
    falsa = SesionFalsa(unico, total_admins=1)
    data, error = borrar(falsa, admin, id=unico.id)
    comprobar(data is None and "al menos un administrador" in (error or ""),
              "no se puede eliminar al último administrador")
    comprobar(falsa.cambios == [], "y no se intentó borrar ni guardar nada")

    # Con otro administrador además del real, sí se puede eliminar
    with nueva_sesion(conn) as db:
        otro_admin = crear_usuario(db, rol="administrador")
        db.commit()
        oid = otro_admin.id
        data, error = borrar(db, admin, id=oid)
    comprobar(error is None and contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=oid) == 0,
              "un administrador se puede eliminar si quedan otros")

    with nueva_sesion(conn) as db:
        inexistente = contar(conn, "SELECT coalesce(max(id), 0) FROM usuarios") + 1000
        data, error = borrar(db, admin, id=inexistente)
    comprobar(data is None and error == "Usuario no encontrado", "usuario inexistente -> 'Usuario no encontrado'")

    # Sin criterios: primero con sesión falsa (si el servicio consultara, se vería sin riesgo)
    falsa = SesionFalsa(SimpleNamespace(id=1, rol="conductor", nombre_completo="x", rut="x", email="x"), 2)
    data, error = borrar(falsa, admin)
    comprobar(data is None and error == "Debes indicar id, rut o email",
              "sin id/rut/email -> error (sesión falsa)")
    comprobar(falsa.consultas == 0 and falsa.cambios == [], "ni siquiera busca un usuario")
    if falsa.consultas == 0 and not falsa.cambios:
        # Solo si la protección está, se repite contra la base real
        antes = contar(conn, "SELECT count(*) FROM usuarios")
        with nueva_sesion(conn) as db:
            data, error = borrar(db, admin)
        comprobar(data is None and error == "Debes indicar id, rut o email"
                  and contar(conn, "SELECT count(*) FROM usuarios") == antes,
                  "sin id/rut/email contra la base real: error y no borra nada")


def caso_e(conn, storage, admin):
    print("== e. Falla al borrar una carpeta ==")
    with nueva_sesion(conn) as db:
        conductor = crear_usuario(db)
        r1 = crear_reporte(db, storage, conductor)
        r2 = crear_reporte(db, storage, conductor)
        db.commit()
        uid, rid1, rid2 = conductor.id, r1.id, r2.id

        original = us.eliminar_carpeta_reporte

        def eliminar_con_falla(usuario_id, reporte_id):
            if reporte_id == rid2:
                raise OSError("permiso denegado (simulado)")
            return original(usuario_id, reporte_id)

        us.eliminar_carpeta_reporte = eliminar_con_falla
        try:
            data, error = borrar(db, admin, id=uid)
        finally:
            us.eliminar_carpeta_reporte = original

    comprobar(error is None, "el servicio no devuelve error")
    comprobar(contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=uid) == 0
              and contar(conn, "SELECT count(*) FROM reportes WHERE id = ANY(:r)", r=[rid1, rid2]) == 0,
              "el usuario y sus reportes quedaron eliminados")
    comprobar(data and data["archivos_no_eliminados"] == [rid2], "archivos_no_eliminados trae el id del reporte")
    comprobar(not os.path.exists(carpeta_reporte(storage, uid, rid1)), "la otra carpeta sí se borró")
    comprobar(os.path.isdir(carpeta_reporte(storage, uid, rid2)), "la carpeta que falló sigue ahí")
    comprobar(os.path.isdir(carpeta_usuario(storage, uid)), "usuario_N se conserva porque no quedó vacía")


def caso_f(conn, storage, admin):
    print("== f. Error de base de datos a mitad: rollback ==")
    with nueva_sesion(conn) as db:
        conductor = crear_usuario(db)
        reporte = crear_reporte(db, storage, conductor)
        db.add(ReporteValidacion(reporte_id=reporte.id, estado_analisis=EstadoAnalisis.completado))
        crear_codigo(db, conductor.email)
        # Se leen antes del commit: leerlos después recarga el objeto y abre un SAVEPOINT
        # nuevo, y la tabla de abajo quedaría dentro de él (el rollback del servicio la borraría)
        uid, rid, email = conductor.id, reporte.id, conductor.email
        db.commit()

        # Tabla normal (no TEMP: Postgres no admite FK de una tabla temporal a una permanente).
        # Se crea dentro de la transacción externa, así que desaparece con el rollback final.
        # Su FK sin ON DELETE hace fallar el DELETE del usuario en el flush.
        conn.execute(text("CREATE TABLE autoprueba_bloqueo (usuario_id integer REFERENCES usuarios(id))"))
        conn.execute(text("INSERT INTO autoprueba_bloqueo VALUES (:u)"), {"u": uid})

        data, error = borrar(db, admin, id=uid)

    comprobar(data is None and error == "Error interno del servidor", "el servicio devuelve error interno")
    comprobar(contar(conn, "SELECT count(*) FROM usuarios WHERE id = :u", u=uid) == 1, "el usuario sigue existiendo")
    comprobar(contar(conn, "SELECT count(*) FROM reportes WHERE id = :r", r=rid) == 1
              and contar(conn, "SELECT count(*) FROM reporte_videos WHERE reporte_id = :r", r=rid) == 2
              and contar(conn, "SELECT count(*) FROM reporte_validacion WHERE reporte_id = :r", r=rid) == 1,
              "su reporte, videos y validación siguen existiendo")
    comprobar(contar(conn, "SELECT count(*) FROM password_reset_codes WHERE email = :e", e=email) == 1,
              "su código de recuperación sigue existiendo")
    comprobar(all(os.path.isfile(os.path.join(carpeta_reporte(storage, uid, rid), f"video_{n}.mp4")) for n in (1, 2)),
              "no se borró ningún archivo")
    comprobar(contar(conn, "SELECT count(*) FROM autoprueba_bloqueo") == 1,
              "autoprueba_bloqueo vive en la transacción externa (el rollback del servicio no la tocó)")
    conn.execute(text("DROP TABLE IF EXISTS autoprueba_bloqueo"))


def main():
    antes = foto_protegida()
    raiz_original = storage_helper.STORAGE_ROOT
    conn = engine.connect()
    trans = conn.begin()
    try:
        with tempfile.TemporaryDirectory() as storage:
            # get_storage_root y eliminar_carpeta_reporte leen esta variable en cada llamada
            storage_helper.STORAGE_ROOT = storage

            with nueva_sesion(conn) as db:
                admin = crear_usuario(db, rol="administrador")
                db.commit()
                # El servicio solo usa current_admin.id
                admin = SimpleNamespace(id=admin.id)

            caso_a(conn, storage, admin)
            caso_b(conn, storage, admin)
            caso_c(conn, admin)
            caso_d(conn, admin)
            caso_e(conn, storage, admin)
            caso_f(conn, storage, admin)
    finally:
        # Siempre: deshace todo lo que hizo la prueba, aunque haya fallado a la mitad
        trans.rollback()
        conn.close()
        storage_helper.STORAGE_ROOT = raiz_original

    print("== Limpieza y datos protegidos ==")
    despues = foto_protegida()
    comprobar(despues["usuarios"] == antes["usuarios"] and len(antes["usuarios"]) == len(USUARIOS_PROTEGIDOS),
              "usuarios 1, 2 y 3 sin cambios")
    comprobar(despues["reportes"] == antes["reportes"] and len(antes["reportes"]) == len(REPORTES_PROTEGIDOS),
              "reportes 67, 68 y 69 sin cambios")
    comprobar(despues["conteos"] == antes["conteos"], "mismos conteos en todas las tablas que antes de la prueba")
    with engine.connect() as c:
        restos = c.execute(text("SELECT count(*) FROM usuarios WHERE email LIKE :p"),
                           {"p": f"autoprueba\\_%{DOMINIO}"}).scalar()
        tabla = c.execute(text("SELECT to_regclass('autoprueba_bloqueo')")).scalar()
    comprobar(restos == 0 and tabla is None, "no quedan usuarios de prueba ni la tabla autoprueba_bloqueo")

    print("\nTODO OK" if not FALLOS else f"\nFALLARON {len(FALLOS)}: {FALLOS}")
    sys.exit(1 if FALLOS else 0)


if __name__ == "__main__":
    main()
