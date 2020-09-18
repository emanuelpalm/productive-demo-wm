#!/bin/bash

cd "$(dirname "$0")" || exit
source "lib_certs.sh"
cd ..

# Root Certificate

create_root_keystore \
  "crypto/arrowhead.p12" "arrowhead.eu"

# Buyer Cloud

create_cloud_keystore \
  "crypto/arrowhead.p12" "arrowhead.eu" \
  "crypto/buyer/cloud.p12" "demo.buyer.arrowhead.eu"

create_buyer_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_SAN=$2

  create_system_keystore \
    "crypto/arrowhead.p12" "arrowhead.eu" \
    "crypto/buyer/cloud.p12" "demo.buyer.arrowhead.eu" \
    "crypto/buyer/system.${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.demo.buyer.arrowhead.eu" \
    "${SYSTEM_SAN}"
}

create_buyer_system_keystore "authorization"     "ip:172.1.1.10,dns:authorization.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "event_handler"     "ip:172.1.1.11,dns:event-handler.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "gatekeeper"        "ip:172.1.1.12,dns:gatekeeper.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "gateway"           "ip:172.1.1.13,dns:gateway.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "orchestrator"      "ip:172.1.1.14,dns:orchestrator.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "service_registry"  "ip:172.1.1.15,dns:service-registry.buyer,dns:localhost,ip:127.0.0.1"

create_buyer_system_keystore "contract_proxy"    "ip:172.1.1.16,dns:contract-proxy.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "buyer"             "ip:172.1.1.19,dns:buyer.buyer,dns:localhost,ip:127.0.0.1"

create_sysop_keystore \
  "crypto/arrowhead.p12" "arrowhead.eu" \
  "crypto/buyer/cloud.p12" "demo.buyer.arrowhead.eu" \
  "crypto/buyer/sysop.p12" "sysop.demo.buyer.arrowhead.eu"

create_truststore \
  "crypto/buyer/truststore.p12" \
  "crypto/arrowhead.crt" "arrowhead.eu" \
  "crypto/buyer/demo.buyer.arrowhead.eu.crt" "demo.buyer.arrowhead.eu"

# Seller Cloud

create_cloud_keystore \
  "crypto/arrowhead.p12" "arrowhead.eu" \
  "crypto/seller/cloud.p12" "demo.seller.arrowhead.eu"

create_seller_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_SAN=$2

  create_system_keystore \
    "crypto/arrowhead.p12" "arrowhead.eu" \
    "crypto/seller/cloud.p12" "demo.seller.arrowhead.eu" \
    "crypto/seller/system.${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.demo.seller.arrowhead.eu" \
    "${SYSTEM_SAN}"
}

create_seller_system_keystore "authorization"     "ip:172.2.1.10,dns:authorization.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "event_handler"     "ip:172.2.1.11,dns:event-handler.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "gatekeeper"        "ip:172.2.1.12,dns:gatekeeper.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "gateway"           "ip:172.2.1.13,dns:gateway.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "orchestrator"      "ip:172.2.1.14,dns:orchestrator.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "service_registry"  "ip:172.2.1.15,dns:service-registry.seller,dns:localhost,ip:127.0.0.1"

create_seller_system_keystore "contract_proxy"    "ip:172.2.1.16,dns:contract-proxy.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "middleware"        "ip:172.2.1.17,dns:middleware.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "factory"           "ip:172.2.1.18,dns:factory.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "seller"            "ip:172.2.1.19,dns:seller.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "workflow_manager"  "ip:172.2.1.20,dns:workflow-manager.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "workflow_executor" "ip:172.2.1.21,dns:workflow-executor.seller,dns:localhost,ip:127.0.0.1"

create_seller_system_keystore "product1"          "ip:172.2.2.10,dns:product1.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product2"          "ip:172.2.2.11,dns:product2.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product3"          "ip:172.2.2.12,dns:product3.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product4"          "ip:172.2.2.13,dns:product4.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product5"          "ip:172.2.2.14,dns:product5.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product6"          "ip:172.2.2.15,dns:product6.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product7"          "ip:172.2.2.16,dns:product7.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product8"          "ip:172.2.2.17,dns:product8.seller,dns:localhost,ip:127.0.0.1"

create_sysop_keystore \
  "crypto/arrowhead.p12" "arrowhead.eu" \
  "crypto/seller/cloud.p12" "demo.seller.arrowhead.eu" \
  "crypto/seller/sysop.p12" "sysop.demo.seller.arrowhead.eu"

create_truststore \
  "crypto/seller/truststore.p12" \
  "crypto/arrowhead.crt" "arrowhead.eu" \
  "crypto/seller/cloud.crt" "demo.seller.arrowhead.eu"

