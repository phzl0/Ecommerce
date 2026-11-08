"""First local configuration. Does not overwrite an existing environment."""

import getpass
import os
from pathlib import Path
import secrets

path = Path(__file__).resolve().parent.parent / ".env"
if path.exists():
    print(".env já existe. Nenhuma configuração foi alterada.")
    raise SystemExit(0)
email = input("E-mail do administrador: ").strip()
if "@" not in email or any(c in email for c in "\r\n"):
    raise SystemExit("Informe um e-mail válido.")
password = getpass.getpass("Senha do administrador (12 a 72 caracteres): ")
if (
    not 12 <= len(password) <= 72
    or len(password.encode("utf-8")) > 72
    or "\n" in password
    or "\r" in password
):
    raise SystemExit("Use uma senha entre 12 e 72 caracteres e até 72 bytes em UTF-8.")
if password != getpass.getpass("Confirme a senha: "):
    raise SystemExit("As senhas não coincidem.")


def quote(value):
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


try:
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    print(".env já existe. Nenhuma configuração foi alterada.")
    raise SystemExit(0)

with os.fdopen(fd, "w", encoding="utf-8") as out:
    out.write(
        "POSTGRES_PASSWORD="
        + secrets.token_urlsafe(32)
        + "\nADMIN_EMAIL="
        + quote(email)
        + "\nADMIN_PASSWORD="
        + quote(password)
        + "\nDEMO_DATA_ENABLED=true\nAPP_PAYMENTS_ENABLED=false\nAPP_PAYMENTS_LIVE_MODE=false\n"
    )
print(
    "Configuração local criada. As senhas não serão exibidas. Agora execute docker compose up --build -d."
)
