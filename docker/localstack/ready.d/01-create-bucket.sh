#!/bin/bash
set -euo pipefail

BUCKET="${S3_BUCKET:-vetsoftware-local}"
STREAM="${FIREHOSE_STREAM:-vetsoftware-audit-local}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"
ROLE_NAME="vetsoftware-local-firehose"
ROLE_ARN="arn:aws:iam::000000000000:role/${ROLE_NAME}"

if ! awslocal s3api head-bucket --bucket "${BUCKET}" >/dev/null 2>&1; then
  awslocal s3api create-bucket --bucket "${BUCKET}" --region "${REGION}"
fi

awslocal iam create-role \
  --role-name "${ROLE_NAME}" \
  --assume-role-policy-document \
  '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"firehose.amazonaws.com"},"Action":"sts:AssumeRole"}]}' \
  >/dev/null 2>&1 || true

if ! awslocal firehose describe-delivery-stream \
  --delivery-stream-name "${STREAM}" >/dev/null 2>&1; then
  awslocal firehose create-delivery-stream \
    --delivery-stream-name "${STREAM}" \
    --delivery-stream-type DirectPut \
    --extended-s3-destination-configuration \
    "RoleARN=${ROLE_ARN},BucketARN=arn:aws:s3:::${BUCKET},Prefix=audit/,ErrorOutputPrefix=errors/,BufferingHints={SizeInMBs=1,IntervalInSeconds=60},CompressionFormat=GZIP" \
    >/dev/null
fi

echo "LocalStack listo: s3://${BUCKET} y Firehose ${STREAM}"
