FROM grafana/grafana:latest

COPY provisioning /etc/grafana/provisioning

# Acceso público de solo lectura y permiso de embebido (iframe en el frontend)
ENV GF_AUTH_ANONYMOUS_ENABLED=true \
    GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer \
    GF_AUTH_ANONYMOUS_ORG_NAME="Main Org." \
    GF_SECURITY_ALLOW_EMBEDDING=true \
    GF_ANALYTICS_REPORTING_ENABLED=false \
    GF_NEWS_NEWS_FEED_ENABLED=false
