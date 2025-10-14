#!/bin/bash
# 创建所有挂载目录并设置权限
base_dir="/usr/docker/data"
NACOS_IMAGE="nacos/nacos-server:v2.4.3"
mkdir -p $base_dir
chmod -R 777 $base_dir

# 创建子目录结构
dirs=(
  mysql/{conf,data,logs}
  nacos/{conf,data,logs}
  redis/{conf,data,logs}
  minio/{certs,data}
  rocketmq/{namesrv/{conf,logs,store},broker/{conf,logs,store}}
  coturn/{conf,logs,certs}
  mediasoup/logs
)

for dir in "${dirs[@]}"; do
  full_path="$base_dir/$dir"
  mkdir -p "$full_path"
  chmod 777 "$full_path"
done

# 配置初始化（仅首次执行）,参数 --rm ：容器退出时自动移除，也就是临时容器
if [ ! -f "${base_dir}/nacos/conf/application.properties" ]; then
    docker run --rm -d --name nacos_init ${NACOS_IMAGE}
    sleep 10
    docker cp nacos_init:/home/nacos/conf ${base_dir}/nacos
    docker stop nacos_init >/dev/null
fi

# 创建RocketMQ Broker配置文件
BROKER_CONF="$base_dir/rocketmq/broker/conf/broker.conf"
if [ ! -f "$BROKER_CONF" ]; then
    cat > "$BROKER_CONF" <<EOF
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
deleteWhen = 04
fileReservedTime = 48
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH
namesrvAddr = rocketmq-namesrv:9876
EOF
    chmod 666 "$BROKER_CONF"
fi

# 创建RocketMQ Namesrv配置文件
NAMESRV_CONF="$base_dir/rocketmq/namesrv/conf/namesrv.properties"
if [ ! -f "$NAMESRV_CONF" ]; then
    touch "$NAMESRV_CONF"
    chmod 666 "$NAMESRV_CONF"
fi

# 创建TurnServer配置文件
TURN_CONF="$base_dir/coturn/conf/coturn.conf"
if [ ! -f "$TURN_CONF" ]; then
    cat > "$TURN_CONF" <<'EOF'
# TURN服务器配置
listening-ip=0.0.0.0
listening-port=3478
tls-listening-port=5349

# 外部IP - 如果是本地测试使用127.0.0.1，生产环境请替换为公网IP
external-ip=192.168.100.130

# Realm配置
realm=local.webrtc.test

# 用户认证（测试用用户名密码）
user=coturn:coturn
lt-cred-mech

# TLS证书配置
cert=/etc/coturn/certs/turn_server_cert.pem
pkey=/etc/coturn/certs/turn_server_pkey.pem

# 中继端口范围
min-port=49152
max-port=65535

# 日志配置
log-file=/var/log/coturn/coturn.log
simple-log
verbose

# 安全配置
no-loopback-peers
no-multicast-peers
no-cli

# 性能配置
max-allocate-lifetime=3600
default-allocation-lifetime=600
no-stun
EOF
    chmod 666 "$TURN_CONF"
fi

# 生成TLS证书（如果不存在）
CERT_DIR="$base_dir/coturn/certs"
if [ ! -f "$CERT_DIR/turn_server_cert.pem" ]; then
    echo "生成TLS证书..."
    openssl req -x509 -newkey rsa:2048 \
        -keyout "$CERT_DIR/turn_server_pkey.pem" \
        -out "$CERT_DIR/turn_server_cert.pem" \
        -days 365 -nodes \
        -subj "/C=CN/ST=Beijing/L=Beijing/O=GuruIM/CN=turn.local.webrtc.test"
    
    chmod 777 "$CERT_DIR/turn_server_cert.pem"
    chmod 777 "$CERT_DIR/turn_server_pkey.pem"
    echo "TLS证书生成完成"
fi

# 停止并删除旧容器（如果存在）
docker compose down

# 启动所有服务
docker compose up -d

echo "所有服务已启动完成！"