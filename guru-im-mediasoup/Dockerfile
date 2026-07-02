# 使用 Node.js 20 官方镜像
FROM node:20-alpine

# 设置国内源
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# 安装 mediasoup 需要的系统依赖
RUN apk add --no-cache \
    python3 \
    py3-pip \
    make \
    g++ \
    git \
    bash \
    linux-headers \
    libc6-compat

# 设置 npm 镜像源
RUN if [ -n "$NPM_REGISTRY" ]; then npm config set registry $NPM_REGISTRY && \
         npm config set fetch-retry-maxtimeout 600000 && \
         npm config set fetch-timeout 600000; \
    fi

# 创建应用目录
WORKDIR /app

# 复制 package.json 和 package-lock.json
COPY package*.json ./

# 安装依赖
RUN npm install --omit=dev --fetch-timeout=600000

# 复制源代码
COPY . .

# 创建非 root 用户（安全考虑）
RUN addgroup -g 1001 -S nodejs && \
    adduser -S mediasoup -u 1001 && \
    chown -R mediasoup:nodejs /app

USER mediasoup

# 暴露必要的端口
EXPOSE 3000
# mediasoup 需要 UDP 端口范围
EXPOSE 40000-40100/udp

# 设置环境变量
ENV NODE_ENV=production

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD node -e "const http = require('http'); http.get('http://localhost:3000/health', (res) => { process.exit(res.statusCode === 200 ? 0 : 1); })"

# 启动应用
CMD ["npm", "start"]