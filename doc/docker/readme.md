1. 脚本上传
```text
  将docker-compose.yaml和startup.sh上传到同一目录，startup.sh是支持重复运行的
```

2. 赋予执行权限
```bash
  sudo chmod +x startup.sh
```

3. 执行部署
```bash
  # 执行部署前，先提前docker pull下载好镜像
  sudo ./startup.sh
```

4. 检查所有容器状态
```bash
  docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

5. 错误排查：一般的nacos会失败，no datasource set
```shell
  docker logs -f nacos # 查看nacos容器启动日志，初始化nacos数据库表mysql-schema.sql后，等他自动重启，或手动重启
  docker restart nacos # 手动重启
  
  ## 如果是报Publickey错误，在nacos的配置文件中的application.properties的数据库连接字符串中追加如下字符串，然后重启nacos即可
  &allowPublicKeyRetrieval=true
  
  ## 如果报用户密码错误等，可以看下文，使用root用户登录mysql并进行新用户创建和授权，完成后重启nacos即可
```

6. 错误排查：rocketmq-broker启动成功，但是实际应用过程无法连接，启动rocketmq-broker后默认的配置文件已挂载出来，修改ip,namesrvAddr等
```shell
# broker.conf文件内容
namesrvAddr = rocketmq-namesrv:9876
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
brokerIP1 = 192.168.2.130
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH
deleteWhen = 04
fileReservedTime = 72
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true
tlsTestModeEnable = false


## 重新启动broker
docker compose restart rocketmq-broker
```


## Mysql常见使用
**创建用户**
```sql
CREATE USER 'guru_im'@'%' IDENTIFIED BY 'mysql1234!';
```

**修改用户名密码**
```sql
ALTER USER 'test_user'@'localhost' IDENTIFIED BY 'NewPass456';
```

**授权用户renren_fast在数据库renren_fast上所有权限**
```sql
GRANT ALL ON guru_im.* TO 'guru_im';
```

**授权访问MySQL上的所有资源**
```sql
GRANT ALL ON *.* TO 'guru_im';