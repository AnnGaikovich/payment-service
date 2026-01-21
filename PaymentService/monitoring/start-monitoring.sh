cd monitoring
docker-compose up -d
echo "Мониторинг запущен:"
echo "   Grafana:      http://localhost:3000 (admin/adminadmin)"
echo "   Prometheus:   http://localhost:9090"
echo "   Loki:         http://localhost:3100"