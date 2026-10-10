#!/usr/bin/env bash
set -euo pipefail

host="couchbase"
admin_user="${COUCHBASE_ADMINISTRATOR_USERNAME:-Administrator}"
admin_password="${COUCHBASE_ADMINISTRATOR_PASSWORD:-password}"
bucket="${COUCHBASE_BUCKET:-inventory}"
scope="${COUCHBASE_SCOPE:-Inventory}"
collection="${COUCHBASE_COLLECTION:-InventoryList}"

until curl -fsS "http://${host}:8091/ui/index.html" >/dev/null; do
  echo "Waiting for Couchbase HTTP service..."
  sleep 5
done

if ! /opt/couchbase/bin/couchbase-cli bucket-list \
  -c "$host" -u "$admin_user" -p "$admin_password" >/dev/null 2>&1; then
  echo "Initializing Couchbase cluster..."
  /opt/couchbase/bin/couchbase-cli cluster-init \
    -c "$host" \
    --cluster-username "$admin_user" \
    --cluster-password "$admin_password" \
    --services data,index,query,fts \
    --cluster-ramsize 1024 \
    --cluster-index-ramsize 256 \
    --cluster-fts-ramsize 256 \
    --index-storage-setting default
fi

until /opt/couchbase/bin/couchbase-cli bucket-list \
  -c "$host" -u "$admin_user" -p "$admin_password" >/dev/null 2>&1; do
  echo "Waiting for Couchbase cluster initialization..."
  sleep 5
done

if ! curl -fsS -u "$admin_user:$admin_password" \
  "http://${host}:8091/pools/default/buckets/${bucket}" >/dev/null 2>&1; then
  echo "Creating Couchbase bucket ${bucket}..."
  /opt/couchbase/bin/couchbase-cli bucket-create \
    -c "$host" \
    -u "$admin_user" \
    -p "$admin_password" \
    --bucket "$bucket" \
    --bucket-type couchbase \
    --bucket-ramsize 256 \
    --enable-flush 1 \
    --wait
fi

if ! curl -fsS -u "$admin_user:$admin_password" \
  "http://${host}:8091/pools/default/buckets/${bucket}/scopes" | grep -q '"name":"'"${scope}"'"'; then
  echo "Creating Couchbase scope ${scope}..."
  curl -fsS -u "$admin_user:$admin_password" \
    -X POST "http://${host}:8091/pools/default/buckets/${bucket}/scopes" \
    --data-urlencode "name=${scope}" >/dev/null
fi

if ! curl -fsS -u "$admin_user:$admin_password" \
  "http://${host}:8091/pools/default/buckets/${bucket}/scopes" | grep -q '"name":"'"${collection}"'"'; then
  echo "Creating Couchbase collection ${scope}.${collection}..."
  curl -fsS -u "$admin_user:$admin_password" \
    -X POST "http://${host}:8091/pools/default/buckets/${bucket}/scopes/${scope}/collections" \
    --data-urlencode "name=${collection}" >/dev/null
fi

query="CREATE PRIMARY INDEX IF NOT EXISTS ON \`${bucket}\`.\`${scope}\`.\`${collection}\`"
curl -fsS -u "$admin_user:$admin_password" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -X POST "http://${host}:8093/query/service" \
  --data-urlencode "statement=${query}" >/dev/null

echo "Couchbase is ready: ${bucket}.${scope}.${collection}"
