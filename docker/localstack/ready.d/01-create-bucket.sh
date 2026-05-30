#!/bin/bash
# Crea el bucket por defecto en LocalStack cuando el servicio S3 está listo.
# El nombre debe coincidir con vetsoftware.storage.s3.bucket del perfil dev (S3_BUCKET).
BUCKET="${S3_BUCKET:-vetsoftware}"
awslocal s3 mb "s3://${BUCKET}" || true
