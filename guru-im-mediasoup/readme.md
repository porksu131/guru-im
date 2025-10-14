# 上传mediasoup代码文件，别上传node_modules，它很大，进入guru-im-mediasoup目录，制作镜像后可选择推送到自己的harbor服务器保存起来
cd /usr/code/guru-im-mediasoup/

# 构建镜像
docker build --build-arg NPM_REGISTRY=https://registry.npmmirror.com/ -t mediasoup-server:latest .

# 测试服务稳定后，docker-compose.xml增加mediasoup服务配置，统一加入startup.sh

# 使用docker-compose启动，或者直接运行startup.sh重新启动所有
docker compose pull mediasoup
docker compose up -d mediasoup

# 查看日志
docker compose logs -f mediasoup

# 登录 Harbor
docker login 192.168.100.170:8080

# 标记镜像
docker tag mediasoup-server 192.168.100.170:8080/docker-repo/mediasoup-server:latest

# 推送镜像到 Harbor
docker push 192.168.100.170:8080/docker-repo/mediasoup-server:latest

