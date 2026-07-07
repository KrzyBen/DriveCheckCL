from weasyprint import HTML
from datetime import datetime
from entity.reporte_entity import Reporte
from helpers.storage_helper import get_reporte_dir

SEVERIDAD_LABELS = {"leve": "Leve", "moderada": "Moderada", "grave": "Grave"}


def _construir_html(reporte: Reporte) -> str:
    infracciones_html = "".join(
        f'<div class="infraccion"><span>{i.articulo} — {i.descripcion}</span></div>'
        for i in reporte.infracciones
    ) or '<p class="sin-infracciones">Sin infracciones registradas</p>'

    fecha_emision = datetime.now().strftime("%d %b %Y")
    usuario_nombre = reporte.usuario.nombre_completo if reporte.usuario else "—"
    validador_nombre = reporte.validado_por.nombre_completo if reporte.validado_por else "—"

    return f"""
    <html>
    <head>
    <style>
        @page {{ size: A4; margin: 2.2cm; }}
        body {{ font-family: Arial, sans-serif; font-size: 11px; color: #1a1a1a; }}
        .header {{ display: flex; justify-content: space-between; align-items: center;
                   border-bottom: 2px solid #0039A6; padding-bottom: 10px; margin-bottom: 14px; }}
        .brand {{ font-size: 12px; font-weight: 600; }}
        .flag span {{ display: inline-block; width: 5px; height: 16px; margin-right: 2px; }}
        .flag .b {{ background: #0039A6; }}
        .flag .w {{ background: #fff; border: 0.5px solid #ccc; }}
        .flag .r {{ background: #D52B1E; }}
        .meta {{ text-align: right; color: #666; font-size: 9.5px; }}
        h1 {{ font-size: 14px; margin: 0 0 2px; }}
        .subtitle {{ color: #666; font-size: 9.5px; margin: 0 0 14px; }}
        .datos {{ display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px;
                  margin-bottom: 14px; padding: 10px 12px; background: #FAFAFA; border-radius: 4px; }}
        .datos span.label {{ color: #888; }}
        .seccion-titulo {{ font-weight: 600; margin: 0 0 6px; font-size: 11px; }}
        .descripcion {{ color: #333; margin: 0 0 14px; line-height: 1.5; }}
        .infraccion {{ border-left: 2px solid #D52B1E; padding: 4px 10px; margin-bottom: 6px; background: #FCF8F8; font-size: 10.5px; }}
        .sin-infracciones {{ color: #888; font-size: 10.5px; }}
        .footer {{ margin-top: 24px; padding-top: 14px; border-top: 0.5px solid #ddd;
                   display: flex; justify-content: space-between; font-size: 9px; color: #888; }}
    </style>
    </head>
    <body>
        <div class="header">
            <div class="brand flag">
                <span class="b"></span><span class="w"></span><span class="r"></span>
                DriveCheckCL
            </div>
            <div class="meta">
                <div>Informe N° {reporte.id}</div>
                <div>Emitido el {fecha_emision}</div>
            </div>
        </div>

        <h1>Informe de Análisis de Accidente de Tránsito</h1>
        <p class="subtitle">Documento generado y validado conforme a la Ley 18.290 de Tránsito</p>

        <div class="datos">
            <div><span class="label">Usuario:</span> {usuario_nombre}</div>
            <div><span class="label">Fecha del reporte:</span> {reporte.created_at.strftime('%d %b %Y')}</div>
            <div><span class="label">Severidad:</span> <strong>{SEVERIDAD_LABELS.get(reporte.severidad_validada, '—')}</strong></div>
            <div><span class="label">Validado por:</span> {validador_nombre}</div>
        </div>

        <p class="seccion-titulo">Descripción del incidente</p>
        <p class="descripcion">{reporte.comentario or 'Sin comentario del usuario.'}</p>

        <p class="seccion-titulo">Infracciones detectadas</p>
        {infracciones_html}

        <p class="seccion-titulo">Notas del validador</p>
        <p class="descripcion">{reporte.notas_admin or 'Sin notas adicionales.'}</p>

        <div class="footer">
            <span>Validado por: {validador_nombre}</span>
            <span>Página 1 de 1</span>
        </div>
    </body>
    </html>
    """


def generar_pdf_informe(reporte: Reporte) -> str:
    html_content = _construir_html(reporte)
    dir_path = get_reporte_dir(reporte.usuario_id, reporte.id)
    nombre_archivo = "informe.pdf"
    ruta_absoluta = f"{dir_path}/{nombre_archivo}"

    HTML(string=html_content).write_pdf(ruta_absoluta)

    return f"usuario_{reporte.usuario_id}/reporte_{reporte.id}/{nombre_archivo}"