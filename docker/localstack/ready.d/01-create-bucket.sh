#!/bin/bash
set -euo pipefail

BUCKET="${S3_BUCKET:-vetsoftware-local}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"

if ! awslocal s3api head-bucket --bucket "${BUCKET}" >/dev/null 2>&1; then
  awslocal s3api create-bucket --bucket "${BUCKET}" --region "${REGION}"
fi

echo "LocalStack listo: s3://${BUCKET}"
