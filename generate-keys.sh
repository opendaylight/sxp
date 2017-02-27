#!/usr/bin/env bash

ID=${1}
PASS=admin123

keytool -genkeypair \
        -alias odl-sxp-${ID} \
        -keyalg RSA \
        -storepass ${PASS} \
        -keystore odl-keystore-${ID}

keytool -exportcert \
        -keystore odl-keystore-${ID} \
        -alias odl-sxp-${ID} \
        -storepass ${PASS} \
        -file odl-sxp-${ID}.cer

keytool -importcert \
        -keystore odl-truststore-${ID} \
        -alias odl-sxp-${ID} \
        -storepass ${PASS} \
        -file odl-sxp-${ID}.cer \
        -noprompt
rm odl-sxp-${ID}.cer
