import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from config.configEnv import SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD, SMTP_FROM_NAME


async def send_email(to: str, subject: str, body: str) -> None:
    """
    Envío simple por SMTP (pensado para Gmail con una "contraseña de
    aplicación", ver .env.example). smtplib es síncrono: para el volumen de
    este proyecto (correos de recuperación de contraseña, no notificaciones
    masivas) esto es suficiente y evita sumar una dependencia nueva. Si más
    adelante se necesita enviar correo con más frecuencia, vale la pena
    moverlo a un threadpool (run_in_executor) para no bloquear el event loop.
    """
    message = MIMEMultipart()
    message["From"] = f"{SMTP_FROM_NAME} <{SMTP_USER}>"
    message["To"] = to
    message["Subject"] = subject
    message.attach(MIMEText(body, "plain"))

    with smtplib.SMTP(SMTP_HOST, SMTP_PORT) as server:
        server.starttls()
        server.login(SMTP_USER, SMTP_PASSWORD)
        server.sendmail(SMTP_USER, to, message.as_string())
