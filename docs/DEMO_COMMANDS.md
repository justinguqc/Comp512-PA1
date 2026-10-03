# Group 30：五机 TCP / RMI 演示命令

以下命令使用这次已连接成功的主机。每个代码块只在标注的主机上执行，
不要把五个角色都运行在一台机器上。

| 角色 | 主机 |
| --- | --- |
| Client | `mimi.cs.mcgill.ca` |
| Middleware | `tr-open-01.cs.mcgill.ca` |
| Flights | `open-gpu-1.cs.mcgill.ca` |
| Cars | `lab1-1.cs.mcgill.ca` |
| Rooms | `tr-open-28.cs.mcgill.ca` |

启动顺序：三个 ResourceManager → 等待 ready → Middleware → 等待 ready → Client。
服务器终端保持打开。每次完整彩排前先停止旧进程，再重启四个服务，以清空内存状态，
避免重复客户 ID 和之前的库存影响结果。这里的“连接命令”是应用启动命令；SSH 登录
使用你自己的 McGill 用户名。

## 1. 准备与编译（共享目录只做一次）

在 Middleware 主机 `tr-open-01.cs.mcgill.ca` 执行。
如果代码已经编译过且未变更，可跳过编译。

```bash
cd ~/Comp512-PA1
git pull --ff-only
export JDK_JAVAC_OPTIONS="--release 17"
make -C Template/Server
make -C Template/Client
```

RMI 独立包：只在 `~/Comp512-PA1-RMI` 尚不存在时解压。
它是已确认的 AI-assisted RMI 版本 `rmi-complete`，不是 non-AI 初版。

```bash
cd ~/Comp512-PA1
unzip releases/rmi-complete.zip -d ~/Comp512-PA1-RMI
cd ~/Comp512-PA1-RMI
export JDK_JAVAC_OPTIONS="--release 17"
make -C Template/Server
make -C Template/Client
```

## 2. TCP 启动

### Flights — open-gpu-1.cs.mcgill.ca

```bash
cd ~/Comp512-PA1
bash Template/Server/run_tcp_server.sh Flights 4001
```

### Cars — lab1-1.cs.mcgill.ca

```bash
cd ~/Comp512-PA1
bash Template/Server/run_tcp_server.sh Cars 4002
```

### Rooms — tr-open-28.cs.mcgill.ca

```bash
cd ~/Comp512-PA1
bash Template/Server/run_tcp_server.sh Rooms 4003
```

### Middleware — tr-open-01.cs.mcgill.ca

等三个后端都显示 `ready on TCP` 后启动。

```bash
cd ~/Comp512-PA1
bash Template/Server/run_tcp_middleware.sh \
  4004 \
  open-gpu-1.cs.mcgill.ca:4001 \
  lab1-1.cs.mcgill.ca:4002 \
  tr-open-28.cs.mcgill.ca:4003
```

### Client — mimi.cs.mcgill.ca

```bash
cd ~/Comp512-PA1
bash Template/Client/run_tcp_client.sh tr-open-01.cs.mcgill.ca 4004
```

## 3. 功能演示（TCP / RMI 共用）

这些是 Client 的 `>]` 命令，不是 Bash 命令。按下列顺序输入。

### 3.1 Bundle、账单与剩余库存

```text
Help
AddFlight,512,3,100
AddCars,Montreal,2,30
AddRooms,Montreal,2,80
AddCustomerID,700
Bundle,700,512,512,Montreal,1,1
QueryCustomer,700
QueryFlight,512
QueryCars,Montreal
QueryRooms,Montreal
```

预期：bundle 成功；2 个座位、1 辆车、1 个房间，总额 **$310**；三种库存各剩 **1**。

### 3.2 删除客户恢复库存

```text
DeleteFlight,512
DeleteCustomer,700
QueryFlight,512
QueryCars,Montreal
QueryRooms,Montreal
DeleteFlight,512
```

预期：第一次删除航班失败；删除客户后库存恢复为 **3 / 2 / 2**；最后删除航班成功。

### 3.3 失败 bundle 的补偿

```text
AddFlight,513,2,100
AddCars,SoldOut,0,30
AddCustomerID,701
Bundle,701,513,SoldOut,1,0
QueryFlight,513
QueryCustomer,701
```

预期：bundle 失败；航班 513 仍有 **2** 个座位，客户账单 **$0**。

## 4. TCP 并发演示

本节用于 TCP；演示前先完成 3.2，使 Montreal 汽车库存恢复为 **2**。

### 客户端 A：准备数据

在当前 TCP Client 中输入：

```text
AddFlight,514,2,100
AddCustomerID,702
```

### 客户端 B：第二个 mimi SSH 终端

```bash
cd ~/Comp512-PA1
bash Template/Client/run_tcp_client.sh tr-open-01.cs.mcgill.ca 4004
```

先在 B 中输入 `QueryCars,Montreal`，确认返回 **2**。保持两个客户端打开。

### 暂停 Flights：新的 open-gpu-1 SSH 终端

```bash
ps -u "$USER" -o pid,args | grep '[S]erver.TCP.TCPResourceManager Flights 4001'
```

找到该 Java 进程的 PID。以下第一行中的 `实际PID` 必须替换成对应数字。
第二行安排 20 秒后恢复，然后暂停 Flights；执行后立即切到两个客户端。

```bash
FLIGHT_PID=实际PID
( sleep 20; kill -CONT "$FLIGHT_PID" ) & kill -STOP "$FLIGHT_PID"
```

在 **A** 输入：

```text
ReserveFlight,702,514
```

A 等待时，在 **B** 输入：

```text
QueryCars,Montreal
```

预期：B 返回 **2**，此时 A 仍等待；Flights 恢复后，A 预订成功。
这个场景说明 middleware 等待 Flights 时仍能处理 Cars 请求。
默认超时为 30 秒，因此要及时切换客户端；也可在暂停终端手动提前恢复：

```bash
kill -CONT "$FLIGHT_PID"
```

## 5. 停止 TCP / 切换 RMI

两个客户端分别输入：

```text
Quit
```

然后对 Middleware 终端按 `Ctrl+C`，再对三个 ResourceManager 终端按 `Ctrl+C`。
不要在共享实验室机器上使用 `killall java`，以免影响其他人的程序。

## 6. RMI 独立版本启动

使用 `~/Comp512-PA1-RMI`。脚本自动管理 registry，**不用另外启动 rmiregistry**。
前缀统一为 `group_30_`；registry 端口 3101–3104，对象端口 4101–4104。

### Flights — open-gpu-1.cs.mcgill.ca

```bash
cd ~/Comp512-PA1-RMI
RMI_HOSTNAME=open-gpu-1.cs.mcgill.ca \
bash Template/Server/run_server.sh Flights 3101 group_30_ 4101
```

### Cars — lab1-1.cs.mcgill.ca

```bash
cd ~/Comp512-PA1-RMI
RMI_HOSTNAME=lab1-1.cs.mcgill.ca \
bash Template/Server/run_server.sh Cars 3102 group_30_ 4102
```

### Rooms — tr-open-28.cs.mcgill.ca

```bash
cd ~/Comp512-PA1-RMI
RMI_HOSTNAME=tr-open-28.cs.mcgill.ca \
bash Template/Server/run_server.sh Rooms 3103 group_30_ 4103
```

### Middleware — tr-open-01.cs.mcgill.ca

等三个后端都 ready 后启动。

```bash
cd ~/Comp512-PA1-RMI
RMI_HOSTNAME=tr-open-01.cs.mcgill.ca \
bash Template/Server/run_middleware.sh \
  open-gpu-1.cs.mcgill.ca:3101 \
  lab1-1.cs.mcgill.ca:3102 \
  tr-open-28.cs.mcgill.ca:3103 \
  3104 group_30_ 4104
```

### Client — mimi.cs.mcgill.ca

```bash
cd ~/Comp512-PA1-RMI
RMI_PORT=3104 RMI_PREFIX=group_30_ \
bash Template/Client/run_client.sh tr-open-01.cs.mcgill.ca Middleware
```

连接成功后执行第 3 节的功能命令。RMI 和 TCP 的内存状态独立，需要重新添加数据。
结束时输入 `Quit`，再按 Middleware → 三个 ResourceManager 的顺序 `Ctrl+C`。

## 7. 本次已经确认的结果

- TCP：五机连接、bundle/账单/库存、删除恢复、失败补偿、暂停 Flights 时 Cars 查询继续完成，均已确认通过。
- RMI 独立版：五机连接、Help、bundle、$310 账单、剩余库存查询，均已确认通过。
- 本次五机 RMI 测试在 3.1 后结束；3.2 和 3.3 可用于之后的演示，未声称本次已在五机 RMI 上执行。
