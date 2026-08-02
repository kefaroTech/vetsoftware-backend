# Gestión de secretos

Este repositorio no admite credenciales, tokens, claves privadas ni material criptográfico funcional en Git.

## Desarrollo local

1. Copie `deploy/env/local.env.example` como `.env.local`.
2. Genere valores únicos fuera de Git. Para `DIAN_ENC_KEY`, use 32 bytes aleatorios codificados en base64.
3. Nunca reutilice valores locales en `dev` o `prod`.
4. Compruebe con `git check-ignore -v .env.local` que el archivo continúa ignorado.

El backend falla al iniciar si `DIAN_ENC_KEY` no está definida. No existe una clave predeterminada en el código.

## Controles obligatorios

- `.husky/pre-commit` ejecuta Gitleaks sobre el contenido staged y bloquea el commit ante cualquier hallazgo.
- `Secret history scan` analiza el historial disponible, contenido codificado y archivos incluidos en cada push o pull request.
- Gitleaks se ejecuta desde una imagen oficial fijada por digest; Docker ausente o detenido bloquea el control local.
- En GitHub, `Secret history scan` debe configurarse como required check y Secret scanning/Push protection deben habilitarse en la organización.

Si una credencial llega a Git, debe revocarse o rotarse primero. Borrar el archivo en un commit posterior no elimina el secreto de los commits anteriores.
