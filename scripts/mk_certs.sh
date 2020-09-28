#!/bin/bash

cd "$(dirname "$0")" || exit
source "lib_certs.sh"
cd ..

# Root Certificate

create_root_keystore \
  "config/crypto/arrowhead.p12" "arrowhead.eu"

# Buyer Cloud

create_cloud_keystore \
  "config/crypto/arrowhead.p12" "arrowhead.eu" \
  "config/crypto/buyer/cloud.p12" "demo.buyer.arrowhead.eu"

create_buyer_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_SAN=$2

  create_system_keystore \
    "config/crypto/arrowhead.p12" "arrowhead.eu" \
    "config/crypto/buyer/cloud.p12" "demo.buyer.arrowhead.eu" \
    "config/crypto/buyer/system.${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.demo.buyer.arrowhead.eu" \
    "${SYSTEM_SAN}"
}

create_buyer_system_keystore "authorization"     "ip:10.1.1.10,dns:authorization.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "event_handler"     "ip:10.1.1.11,dns:event-handler.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "gatekeeper"        "ip:10.1.1.12,dns:gatekeeper.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "gateway"           "ip:10.1.1.13,dns:gateway.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "orchestrator"      "ip:10.1.1.14,dns:orchestrator.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "service_registry"  "ip:10.1.1.15,dns:service-registry.buyer,dns:localhost,ip:127.0.0.1"

create_buyer_system_keystore "contract_proxy"    "ip:10.1.1.16,dns:contract-proxy.buyer,dns:localhost,ip:127.0.0.1"
create_buyer_system_keystore "buyer"             "ip:10.1.1.19,dns:sys-buyer.buyer,dns:localhost,ip:127.0.0.1"

create_sysop_keystore \
  "config/crypto/arrowhead.p12" "arrowhead.eu" \
  "config/crypto/buyer/cloud.p12" "demo.buyer.arrowhead.eu" \
  "config/crypto/buyer/sysop.p12" "sysop.demo.buyer.arrowhead.eu"

create_truststore \
  "config/crypto/buyer/truststore.p12" \
  "config/crypto/arrowhead.crt" "arrowhead.eu" \
  "config/crypto/buyer/demo.buyer.arrowhead.eu.crt" "demo.buyer.arrowhead.eu"

# Seller Cloud

create_cloud_keystore \
  "config/crypto/arrowhead.p12" "arrowhead.eu" \
  "config/crypto/seller/cloud.p12" "demo.seller.arrowhead.eu"

create_seller_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_SAN=$2

  create_system_keystore \
    "config/crypto/arrowhead.p12" "arrowhead.eu" \
    "config/crypto/seller/cloud.p12" "demo.seller.arrowhead.eu" \
    "config/crypto/seller/system.${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.demo.seller.arrowhead.eu" \
    "${SYSTEM_SAN}"
}

create_seller_system_keystore "authorization"     "ip:10.1.2.10,dns:authorization.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "event_handler"     "ip:10.1.2.11,dns:event-handler.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "gatekeeper"        "ip:10.1.2.12,dns:gatekeeper.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "gateway"           "ip:10.1.2.13,dns:gateway.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "orchestrator"      "ip:10.1.2.14,dns:orchestrator.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "service_registry"  "ip:10.1.2.15,dns:service-registry.seller,dns:localhost,ip:127.0.0.1"

create_seller_system_keystore "contract_proxy"    "ip:10.1.2.16,dns:contract-proxy.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "middleware"        "ip:10.1.2.17,dns:middleware.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "seller"            "ip:10.1.2.18,dns:sys-seller.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "workflow_manager"  "ip:10.1.2.19,dns:workflow-manager.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "workflow_executor" "ip:10.1.2.20,dns:workflow-executor.seller,dns:localhost,ip:127.0.0.1"

create_seller_system_keystore "product1"          "ip:10.1.2.101,dns:product1.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product2"          "ip:10.1.2.102,dns:product2.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product3"          "ip:10.1.2.103,dns:product3.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product4"          "ip:10.1.2.104,dns:product4.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product5"          "ip:10.1.2.105,dns:product5.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product6"          "ip:10.1.2.106,dns:product6.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product7"          "ip:10.1.2.107,dns:product7.seller,dns:localhost,ip:127.0.0.1"
create_seller_system_keystore "product8"          "ip:10.1.2.108,dns:product8.seller,dns:localhost,ip:127.0.0.1"

create_sysop_keystore \
  "config/crypto/arrowhead.p12" "arrowhead.eu" \
  "config/crypto/seller/cloud.p12" "demo.seller.arrowhead.eu" \
  "config/crypto/seller/sysop.p12" "sysop.demo.seller.arrowhead.eu"

create_truststore \
  "config/crypto/seller/truststore.p12" \
  "config/crypto/arrowhead.crt" "arrowhead.eu" \
  "config/crypto/seller/cloud.crt" "demo.seller.arrowhead.eu"

# Unified Cloud

create_cloud_keystore \
  "config/crypto/arrowhead.p12" "arrowhead.eu" \
  "config/crypto/uni/cloud.p12" "demo.uni.arrowhead.eu"

create_uni_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_SAN=$2

  create_system_keystore \
    "config/crypto/arrowhead.p12" "arrowhead.eu" \
    "config/crypto/uni/cloud.p12" "demo.uni.arrowhead.eu" \
    "config/crypto/uni/system.${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.demo.uni.arrowhead.eu" \
    "${SYSTEM_SAN}"
}

create_uni_system_keystore "authorization"         "ip:10.1.3.10,dns:authorization.uni,dns:localhost,ip:127.0.0.1"
create_uni_system_keystore "event_handler"         "ip:10.1.3.11,dns:event-handler.uni,dns:localhost,ip:127.0.0.1"
create_uni_system_keystore "orchestrator"          "ip:10.1.3.14,dns:orchestrator.uni,dns:localhost,ip:127.0.0.1"
create_uni_system_keystore "service_registry"      "ip:10.1.3.15,dns:service-registry.uni,dns:localhost,ip:127.0.0.1"

create_uni_system_keystore "buyer"                 "ip:10.1.3.16,dns:sys-buyer.uni,dns:localhost,ip:127.0.0.1"
create_uni_system_keystore "buyer_operator"        ""
create_uni_system_keystore "contract_proxy_buyer"  "ip:10.1.3.17,dns:contract-proxy-buyer.uni,dns:localhost,ip:127.0.0.1"

create_uni_system_keystore "middleware"            "ip:10.1.3.18,dns:middleware.uni,dns:localhost,ip:127.0.0.1"
create_uni_system_keystore "middleware_operator"   ""
create_uni_system_keystore "seller"                "ip:10.1.3.18,dns:sys-seller.uni,dns:localhost,ip:127.0.0.1"
create_uni_system_keystore "contract_proxy_seller" "ip:10.1.3.19,dns:contract-proxy-seller.uni,dns:localhost,ip:127.0.0.1"

create_sysop_keystore \
  "config/crypto/arrowhead.p12" "arrowhead.eu" \
  "config/crypto/uni/cloud.p12" "demo.uni.arrowhead.eu" \
  "config/crypto/uni/sysop.p12" "sysop.demo.uni.arrowhead.eu"

create_truststore \
  "config/crypto/uni/truststore.p12" \
  "config/crypto/arrowhead.crt" "arrowhead.eu" \
  "config/crypto/uni/cloud.crt" "demo.uni.arrowhead.eu"