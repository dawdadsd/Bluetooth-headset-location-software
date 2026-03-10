# FreeClip Guard Android 深度学习文档

> 关联代码：`app/src/main/java/com/example/freeclipguard/MainActivity.java`、`app/src/main/java/com/example/freeclipguard/BindDeviceActivity.java`、`app/src/main/java/com/example/freeclipguard/receiver/BluetoothStateReceiver.java`、`app/src/main/java/com/example/freeclipguard/monitor/DeviceMonitor.java`、`app/src/main/java/com/example/freeclipguard/data/BoundDeviceStore.java`、`app/src/main/java/com/example/freeclipguard/data/LostEventRepository.java`、`app/src/main/java/com/example/freeclipguard/location/LocationSnapshotProvider.java`、`app/src/main/java/com/example/freeclipguard/companion/CompanionAssociationManager.java`、`app/src/main/java/com/example/freeclipguard/companion/FreeClipCompanionService.java`、`app/src/main/java/com/example/freeclipguard/util/NotificationHelper.java`

## 1. 全局定位（Where Are We?）

这个项目本质上不是一个“精准找耳机系统”，而是一个面向 `FreeClip/FreeBuds + 非华为 Android 手机` 场景的 **防丢提醒、最后位置记录、记忆辅助工具**。

如果用一句话概括它的设计初衷：

> 它不试图替代官方查找网络，而是试图在“你忘了耳机放哪了、耳机何时断开了、最后一次在哪里出现过”这些高频现实问题上，给出一个本地、低依赖、可运行的解决方案。

没有这个项目，用户在非华为生态里会遇到几个典型问题：

- 没有系统级离线查找网络，丢了之后无法远程持续追踪
- 蓝牙耳机即使还能连接，位置也往往不精确
- 更常见的问题不是“技术上找不到”，而是“人脑已经记不得放哪了”
- 手机厂商 ROM 差异大，想做一个稳定的后台提醒工具并不简单

所以这个项目的真正价值，不是做成 AirTag 那类高精度资产追踪器，而是做成一个 **“记忆增强器 + 最后现场记录器”**。

---

## 2. 上下文与调用链

### 2.1 用户视角的完整流程

从业务角度看，这个应用有 4 条核心主线：

1. **首次进入与边界说明**
   - 用户第一次打开 App
   - 先看到 4 页产品边界与隐私说明
   - 再进入权限/绑定向导

2. **设备绑定与身份确定**
   - 用户进入绑定页
   - App 读取系统已配对蓝牙设备列表
   - 用户选择目标耳机
   - App 保存耳机名称和 MAC 地址

3. **后台监听与事件记录**
   - Android 系统广播蓝牙连接/断开事件
   - App 在后台被唤醒
   - 只处理“已绑定耳机”的事件
   - 立刻尝试获取当前或最近可用定位
   - 写入本地数据库并弹通知

4. **前台可视化与找回辅助**
   - 首页显示绑定状态、播放状态、手动填写的放置记录、最近事件
   - 历史页展示连接/断开事件时间线
   - 附近搜索页尝试通过蓝牙扫描做近距离定位
   - 地图入口可直接打开高德或系统地图

### 2.2 代码中的关键入口

- App 主页入口：`app/src/main/java/com/example/freeclipguard/MainActivity.java`
- 首次产品说明入口：`app/src/main/java/com/example/freeclipguard/IntroActivity.java`
- 权限/绑定向导：`app/src/main/java/com/example/freeclipguard/OnboardingActivity.java`
- 设备绑定页：`app/src/main/java/com/example/freeclipguard/BindDeviceActivity.java`
- 系统蓝牙广播入口：`app/src/main/java/com/example/freeclipguard/receiver/BluetoothStateReceiver.java`
- 后台事件处理器：`app/src/main/java/com/example/freeclipguard/monitor/DeviceMonitor.java`
- 本地状态与配置存储：`app/src/main/java/com/example/freeclipguard/data/BoundDeviceStore.java`
- 事件持久化与通知协调：`app/src/main/java/com/example/freeclipguard/data/LostEventRepository.java`
- 定位获取：`app/src/main/java/com/example/freeclipguard/location/LocationSnapshotProvider.java`
- 伴生设备增强：`app/src/main/java/com/example/freeclipguard/companion/CompanionAssociationManager.java`
- 伴生服务回调：`app/src/main/java/com/example/freeclipguard/companion/FreeClipCompanionService.java`

### 2.3 一张简化的调用链图

```text
用户首次打开
   -> IntroActivity
   -> OnboardingActivity
   -> BindDeviceActivity
   -> BoundDeviceStore.saveBoundDevice()

系统蓝牙 ACL_CONNECTED / ACL_DISCONNECTED 广播
   -> BluetoothStateReceiver
   -> DeviceMonitor
   -> BoundDeviceStore.getBoundDevice()
   -> BoundDevice.matchesAddress()
   -> LostEventRepository.recordConnect()/recordDisconnect()
   -> LocationSnapshotProvider.getFreshSnapshot()
   -> Room(AppDatabase / LostEventDao)
   -> NotificationHelper / DisconnectOverlayManager

用户回到前台
   -> MainActivity.refreshSummary()
   -> PlaybackStatusResolver / LostEventRepository.loadLatest()
   -> 首页状态展示
```

---

## 3. 为什么选择这些实现，而不是其他方案？

### 3.1 本项目的现实约束

这个项目的设计选择，明显受下面几个约束驱动：

- **目标设备是蓝牙耳机，不是持续在线的定位硬件**
- **目标手机是普通 Android，且可能不是华为手机**
- **开发语言要适合熟悉 Java 的维护者**
- **不依赖后端，不上传隐私数据**
- **后台行为必须尽量依赖系统机制，而不是长驻死循环服务**

### 3.2 为什么用 Java + XML，而不是 Kotlin + Compose？

这个选择并不是“技术上最先进”，而是“当前项目最匹配”。

收益：

- 对熟悉 Java 的维护者更友好
- Android 蓝牙、广播、位置、Room 这些能力用 Java 已完全够用
- XML 布局在 MVP 场景下学习曲线低、调试成本低

代价：

- UI 开发效率不如 Compose
- 状态同步与界面复用能力不如 Kotlin + Jetpack Compose
- 部分现代 Android API 示例更多偏 Kotlin，需要手动转换

### 3.3 为什么用静态 `BroadcastReceiver`，而不是一直开前台服务？

这是项目里很重要的工程取舍。

选静态广播接收器的原因：

- 用户不需要一直开着 App
- 系统蓝牙连接/断开事件本来就会通过广播投递
- 对“连上/断开就记一条”的需求来说，广播模型最自然
- 比长驻前台服务省电，也更符合 MVP 范围

替代方案 1：前台服务常驻监听

- 好处：更可控，后台行为更稳定
- 坏处：耗电更高、侵入性更强、用户观感差、通知栏会一直挂一个常驻服务

替代方案 2：周期性后台扫描

- 好处：理论上可以弥补广播缺失
- 坏处：Android 8+ 以后受限严重，功耗高，且对蓝牙耳机并不稳定

所以这里的设计很明确：

> 对“状态突变事件”优先使用系统广播；对“持续感知存在性”再用 Companion Device 做增强，而不是反过来。

### 3.4 为什么设备身份用 `address`，而不是设备名？

这是整个项目最关键的建模决策之一。

设备名只是展示给用户看的，真正用于逻辑判断的是 MAC 地址：

- 设备名可能重名、变化、或者被 ROM 裁剪
- 地址更像“业务主键”
- 后台广播里拿到的也是具体设备对象，最可靠的匹配方式就是地址比对

这也是项目能够做到“只监听我选中的那只耳机”的基础。

---

## 4. 核心源码摘录与实现细节

下面不是整文件翻译，而是挑出真正决定系统行为的代码片段来讲。

### 4.1 绑定设备：从系统已配对列表中选择目标耳机

**为什么看这段**：这是整个系统“知道你在看哪只耳机”的起点。

**文件位置**：`app/src/main/java/com/example/freeclipguard/BindDeviceActivity.java`

```java
Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
List<BluetoothDevice> sortedDevices = new ArrayList<>(bondedDevices);
sortedDevices.sort(Comparator.comparingInt(this::scoreDevice).reversed());
...
boundDeviceStore.saveBoundDevice(bluetoothDevice.getName(), bluetoothDevice.getAddress());
```

**What**
- 从系统拿到“已配对设备列表”
- 做一个简单排序，优先把名字里像 `FreeClip` / `HUAWEI` 的设备放前面
- 用户选择后，保存设备名和地址

**Why**
- 项目不尝试自己做蓝牙配对流程，直接复用 Android 系统已配对信息
- 这样实现简单、权限更少、兼容性更好

**What if not**
- 如果不先绑定目标设备，后续广播到来时就无法知道“这次连接的是不是我要守护的耳机”

**Connection**
- 保存后的数据会被 `BoundDeviceStore` 持久化
- 后续广播监听、伴生设备回调、首页状态展示都依赖这里的输出

### 4.2 本地状态存储：把“目标耳机”变成整个 App 的全局事实

**为什么看这段**：这是全系统状态的轻量中心。

**文件位置**：`app/src/main/java/com/example/freeclipguard/data/BoundDeviceStore.java`

```java
public void saveBoundDevice(String name, String address) {
    preferences.edit()
            .putString(KEY_DEVICE_NAME, name)
            .putString(KEY_DEVICE_ADDRESS, address)
            .putBoolean(KEY_DEVICE_CONNECTED, false)
            .apply();
}

public BoundDevice getBoundDevice() {
    return new BoundDevice(
            preferences.getString(KEY_DEVICE_NAME, ""),
            preferences.getString(KEY_DEVICE_ADDRESS, "")
    );
}
```

**What**
- 使用 `SharedPreferences` 保存“绑定设备”“家位置”“自定义提示词”“首次引导状态”等轻量配置

**Why**
- 这些都是单条、小体积、低并发、无需复杂查询的数据
- 用 Room 会过重，用内存会丢失，用文件会更难维护

**What if not**
- 如果不把绑定信息持久化，App 杀掉后用户得每次重新选耳机

**Connection**
- `MainActivity`、`DeviceMonitor`、`CompanionAssociationManager`、`PlaybackStatusResolver` 都会读取这里的数据

### 4.3 设备身份匹配：名字是展示，地址才是主键

**为什么看这段**：这是系统避免误判的根基。

**文件位置**：`app/src/main/java/com/example/freeclipguard/model/BoundDevice.java`

```java
public boolean isConfigured() {
    return !address.isBlank();
}

public boolean matchesAddress(String otherAddress) {
    return otherAddress != null && address.equalsIgnoreCase(otherAddress);
}
```

**What**
- 这个模型非常小，但表达了两个关键事实：
  - 是否已绑定
  - 当前事件是否属于这只耳机

**Why**
- 把匹配逻辑集中在模型对象中，比散落在各处更清晰

**What if not**
- 如果到处自己写字符串比较，容易重复、遗漏空值判断、引入不一致行为

**Connection**
- 这是 `DeviceMonitor` 和 `FreeClipCompanionService` 的核心判断条件

### 4.4 系统广播唤醒：App 被划掉后也能在事件到来时工作

**为什么看这段**：这决定了项目能否“静默运行”。

**文件位置**：`app/src/main/AndroidManifest.xml`、`app/src/main/java/com/example/freeclipguard/receiver/BluetoothStateReceiver.java`

```xml
<receiver
    android:name=".receiver.BluetoothStateReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="android.bluetooth.device.action.ACL_CONNECTED" />
        <action android:name="android.bluetooth.device.action.ACL_DISCONNECTED" />
    </intent-filter>
</receiver>
```

```java
@Override
public void onReceive(Context context, Intent intent) {
    PendingResult pendingResult = goAsync();
    DeviceMonitor.handleBluetoothBroadcast(context.getApplicationContext(), intent, pendingResult);
}
```

**What**
- 在清单里静态注册蓝牙连接/断开广播
- 收到广播后使用 `goAsync()`，把后续工作交给后台处理

**Why**
- 事件发生时，系统可以唤醒接收器
- `goAsync()` 避免在主线程里做数据库和定位等慢操作

**What if not**
- 不用静态接收器，就只能在 App 打开时动态注册，后台不会触发
- 不用 `goAsync()`，在广播回调里做重操作容易超时或卡主线程

**Connection**
- 实际业务逻辑不写在 Receiver 本身，而是转交给 `DeviceMonitor`

### 4.5 事件处理器：只处理“我绑定的那只耳机”

**为什么看这段**：这里把系统原始广播变成项目可理解的业务事件。

**文件位置**：`app/src/main/java/com/example/freeclipguard/monitor/DeviceMonitor.java`

```java
BoundDeviceStore boundDeviceStore = new BoundDeviceStore(context);
BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
if (!boundDevice.isConfigured() || bluetoothDevice == null
        || !boundDevice.matchesAddress(safeAddress(bluetoothDevice))) {
    pendingResult.finish();
    return;
}

if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
    LostEventRepository.getInstance(context).recordConnect(...);
    return;
}

if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
    LostEventRepository.getInstance(context).recordDisconnect(...);
    return;
}
```

**What**
- 先过滤掉不相关设备
- 再把连接/断开统一转成仓库层事件写入流程

**Why**
- 广播层是系统事件；仓库层才是业务事件
- 这样 Receiver/Monitor 保持轻量，数据库、定位、通知集中在 Repository 协调

**What if not**
- 如果广播处理直接散落写数据库、发通知，会让代码耦合非常严重

**Connection**
- 这是“Android 系统事件”进入“本项目状态机”的门口

### 4.6 定位获取：先尝试 current location，再回退 last known

**为什么看这段**：这个项目的价值高度依赖“最后位置记录”的质量。

**文件位置**：`app/src/main/java/com/example/freeclipguard/location/LocationSnapshotProvider.java`

```java
public static LocationSnapshot getFreshSnapshot(Context context) {
    Location currentBest = getCurrentBestLocation(locationManager, providers, DEFAULT_CURRENT_LOCATION_TIMEOUT_MS);
    Location lastKnownBest = getBestLastKnownLocation(locationManager, providers);
    Location chosenLocation = choosePreferredLocation(currentBest, lastKnownBest);
    return toSnapshot(chosenLocation);
}
```

```java
private static Location choosePreferredLocation(@Nullable Location currentLocation,
        @Nullable Location lastKnownLocation) {
    if (currentLocation == null) {
        return lastKnownLocation;
    }
    if (lastKnownLocation == null) {
        return currentLocation;
    }
    boolean currentIsFreshAndAccurate = isFresh(currentLocation)
            && currentLocation.getAccuracy() <= DESIRABLE_ACCURACY_METERS;
    if (currentIsFreshAndAccurate) {
        return currentLocation;
    }
    return scoreLocation(currentLocation) >= scoreLocation(lastKnownLocation)
            ? currentLocation : lastKnownLocation;
}
```

**What**
- 尝试同时从多个 provider 获取当前位置
- 如果拿不到足够好的 current location，就退回 last known location
- 再基于精度、时间、provider 做评分选优

**Why**
- 纯 `lastKnownLocation` 太容易过期
- 纯等“最新定位”又可能超时、为空、拖慢广播处理
- “current + last known”混合策略是当前项目中性价比最高的实现

**What if not**
- 只用 last known：记录速度快，但误差常常更大
- 只等 current：更准，但后台事件容易拿不到、耗时更长

**Connection**
- `LostEventRepository` 每次写事件前都依赖这里返回的位置快照

### 4.7 仓库层：统一协调定位、去重、落库、通知

**为什么看这段**：这是项目最像“应用服务层”的地方。

**文件位置**：`app/src/main/java/com/example/freeclipguard/data/LostEventRepository.java`

```java
public void recordConnect(...) {
    recordEvent(boundDeviceStore, bluetoothDevice, source, EVENT_TYPE_CONNECTED, rssi, note, onComplete);
}

public void recordDisconnect(...) {
    recordEvent(boundDeviceStore, bluetoothDevice, source, EVENT_TYPE_DISCONNECTED, rssi, note, onComplete);
}
```

```java
LocationSnapshot snapshot = LocationSnapshotProvider.getFreshSnapshot(appContext);
...
event.id = appDatabase.lostEventDao().insert(event);

if (!connected) {
    DisconnectOverlayManager.show(appContext, event, disconnectPrompt);
    NotificationHelper.showDisconnectAlert(appContext, event, disconnectPrompt);
} else {
    NotificationHelper.showConnectionRecorded(appContext, event);
}
```

**What**
- 统一处理连接/断开事件
- 获取位置
- 做重复事件过滤
- 写入 Room
- 决定发哪种通知/弹窗

**Why**
- 这层把 Android 平台细节和业务意图隔开了
- 它不像 Repository 的教科书定义那么纯，但在小型本地 App 中很实用

**What if not**
- 如果定位、数据库、通知都分散在广播处理器里，维护难度会迅速上升

**Connection**
- 上游是 `DeviceMonitor` / `FreeClipCompanionService`
- 下游是 `LocationSnapshotProvider`、`LostEventDao`、`NotificationHelper`

### 4.8 Room 持久化：把“耳机事件”变成可回看的历史

**为什么看这段**：这个项目的“记忆能力”来自历史数据，而不是瞬时状态。

**文件位置**：`app/src/main/java/com/example/freeclipguard/data/LostEvent.java`、`app/src/main/java/com/example/freeclipguard/data/LostEventDao.java`

```java
@Entity(tableName = "lost_events")
public class LostEvent {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String deviceName;
    public String deviceAddress;
    public long eventTimeMs;
    public Double latitude;
    public Double longitude;
    public Float accuracyMeters;
    public Long locationSampleTimeMs;
    public String eventSource;
    public String eventType;
    public Integer rssi;
    public boolean atHome;
    public String note;
}
```

```java
@Query("SELECT * FROM lost_events WHERE deviceAddress = :deviceAddress AND eventType = :eventType ORDER BY eventTimeMs DESC LIMIT 1")
LostEvent findLatestForDeviceAndType(String deviceAddress, String eventType);
```

**What**
- 用一张表保存连接/断开事件
- 保存时间、经纬度、精度、事件来源、类型、备注等上下文

**Why**
- 这类数据天然适合时间线查询
- Room 在本地查询、排序、持久化上比 `SharedPreferences` 更合理

**What if not**
- 如果只在内存里保存最近一条，用户一关 App 记忆就丢了
- 如果只用配置存储，不适合按时间序列回看

**Connection**
- 历史页、首页最近事件摘要都依赖这里的数据

### 4.9 伴生设备增强：给广播不稳定的 ROM 再加一层保险

**为什么看这段**：这体现了作者对 Android 现实世界的认识。

**文件位置**：`app/src/main/java/com/example/freeclipguard/companion/CompanionAssociationManager.java`、`app/src/main/java/com/example/freeclipguard/companion/FreeClipCompanionService.java`

```java
BluetoothDeviceFilter.Builder filterBuilder = new BluetoothDeviceFilter.Builder()
        .setNamePattern(Pattern.compile(".*(FreeClip|HUAWEI).*", Pattern.CASE_INSENSITIVE));
if (!boundDevice.getAddress().isBlank()) {
    filterBuilder.setAddress(boundDevice.getAddress());
}
...
manager.startObservingDevicePresence(boundDevice.getAddress());
```

```java
@Override
public void onDeviceAppeared(String address) {
    if (boundDevice.matchesAddress(address)) {
        LostEventRepository.getInstance(this).recordConnect(...);
    }
}
```

**What**
- 在支持 Android 12+ Companion Device 的设备上，额外观察目标耳机是否出现/消失

**Why**
- 有些 ROM 对蓝牙 ACL 广播不够稳定
- Companion Device 不能替代广播，但能补充存在性检测

**What if not**
- 在部分机型上，后台漏记概率会更高

**Connection**
- 这条链路和广播链路最终都收敛到 `LostEventRepository`

---

## 5. 底层原理：从机制到实现

### 5.1 协议/契约层

这个项目没有远程 API，但内部依然存在明确契约：

- `BoundDeviceStore`：定义“当前守护的是谁”
- `LostEvent`：定义“事件记录长什么样”
- `LocationSnapshot`：定义“位置快照长什么样”
- `NotificationHelper`：定义“如何把事件翻译成用户通知”

这些对象不是随意拼出来的，它们分别对应了“设备身份”“历史事实”“环境上下文”“用户反馈”四个层次。

### 5.2 执行与调度层

- 广播由系统触发
- Receiver 用 `goAsync()` 把耗时逻辑转交后台执行
- `LostEventRepository` 用单线程 `ExecutorService` 保证写库顺序和去重一致性
- 主线程只负责更新 UI 或回调 `onComplete`

这说明作者的想法是：

> 后台事件处理可以慢一点，但必须顺序稳定；UI 可以异步拿结果，不参与核心写入逻辑。

### 5.3 状态与一致性层

项目同时维护两类状态：

1. **轻量状态**：当前绑定设备、连接标记、家位置、提示词、首次引导状态
   - 存 `SharedPreferences`

2. **历史状态**：每次连接/断开事件
   - 存 Room

这个分层很合理，因为它把“当前真相”和“历史记忆”分开了。

### 5.4 错误、取消与资源清理

项目里很多地方都体现出一种“温和兜底”的风格：

- 权限不足时返回 `null`，而不是直接崩
- 地图打不开时回退系统地图，仍失败再提示用户
- 定位 current 获取失败时回退 last known
- Companion 功能不支持时直接降级

这不是最理想化的做法，但很符合 Android 实战：

> 在设备碎片化环境下，优先保证可运行，再争取能力上限。

### 5.5 可观测性与诊断

当前项目在可观测性上还比较轻：

- 少量 `Log.e`
- 主要靠页面文本和通知给用户反馈
- 没有统一埋点、没有性能/错误统计

这对本地 MVP 是可以接受的，但如果以后要长期迭代，建议把“广播命中率、定位成功率、通知触达率、去重命中率”补成内部调试指标。

---

## 6. 在本项目里的功能落地路径

### 6.1 路径一：绑定并监听目标耳机

```text
BindDeviceActivity
  -> getBondedDevices()
  -> 用户选择耳机
  -> BoundDeviceStore.saveBoundDevice()
  -> 以后所有广播都只处理这个 address
```

### 6.2 路径二：耳机连接时自动记录位置

```text
系统广播 ACL_CONNECTED
  -> BluetoothStateReceiver
  -> DeviceMonitor
  -> 匹配已绑定耳机 address
  -> LostEventRepository.recordConnect()
  -> LocationSnapshotProvider.getFreshSnapshot()
  -> Room.insert(CONNECTED event)
  -> NotificationHelper.showConnectionRecorded()
```

### 6.3 路径三：耳机断开时自动记录位置与提醒

```text
系统广播 ACL_DISCONNECTED
  -> BluetoothStateReceiver
  -> DeviceMonitor
  -> 匹配已绑定耳机 address
  -> LostEventRepository.recordDisconnect()
  -> 获取位置
  -> Room.insert(DISCONNECTED event)
  -> 悬浮提醒 + 顶部通知 + 地图入口
```

### 6.4 路径四：用户自己补充“我把耳机放哪了”

```text
MainActivity 输入框
  -> BoundDeviceStore.saveManualPlaceNote()
  -> 首页与搜索页读取手动记录
  -> 与自动事件历史一起辅助回忆
```

这条链路特别能体现本项目的设计哲学：

- 自动记录解决“事件事实”
- 手动记录解决“人的记忆”
- 两者组合，才是真正有价值的找回辅助

---

## 7. 除了这样写，还可以怎么写？

### 7.1 方案 A：使用 `FusedLocationProviderClient`

**设计思路**
- 用 Google Play Services 的定位能力替代 `LocationManager`

**等价点**
- 同样能拿 current location / last location
- 常见机型上精度和行为通常更稳定

**收益**
- API 更现代
- 对 provider 细节封装更好
- 精度策略可控性更强

**代价**
- 依赖 Google Play Services
- 对没有 GMS 的环境不友好
- 项目复杂度上升

**什么时候更适合**
- 你明确只面向有 GMS 的 Android 设备
- 你更关心定位质量而不是最小依赖

### 7.2 方案 B：使用前台服务长期运行

**设计思路**
- 常驻一个前台服务，统一监听连接状态和位置变化

**等价点**
- 一样能在后台做自动记录

**收益**
- 后台行为更稳定
- 某些 ROM 下被系统限制的概率更低

**代价**
- 更耗电
- 通知栏常驻，用户感受差
- 对 MVP 的侵入性过强

**什么时候更适合**
- 如果未来产品目标变成“高可靠全天守护”，而不是“尽量轻量记录”

### 7.3 方案 C：把历史与配置统一迁到 Room

**设计思路**
- 不再用 `SharedPreferences` 存绑定设备、家位置、提示词等，全部建表

**收益**
- 数据模型更统一
- 多条手动记录、复杂设置、迁移版本控制更容易

**代价**
- 对当前小项目来说有点过度设计
- 启动时读取简单配置反而变复杂

**什么时候更适合**
- 配置项明显增多，需要版本化迁移时

---

## 8. 扩展场景与未来演进

### 8.1 可以继续扩展什么？

- 把经纬度反解成中文地址
- 历史页按“连接 / 断开 / 手动放置记录”分类展示
- 给低精度位置打风险标签
- 增加“出租车模式”“通勤模式”的识别策略
- 增加“最近 5 条手动放置记录”和容量上限管理
- 做一套更完整的品牌化视觉系统（猫咪图标、启动页、引导页统一）

### 8.2 哪些业务可以复用这套思路？

- 钥匙、手环、鼠标、游戏手柄等蓝牙外设的最后出现记录
- 局部离线资产守护工具
- 本地隐私优先的轻量设备日志工具

### 8.3 什么情况下不适用？

- 需要真正远程追踪设备位置
- 需要米级甚至更高精度追踪
- 设备本身常常关机、休眠、不广播
- 用户处在强后台限制的 ROM 环境中，且不愿配置权限

---

## 9. 优化方向还可以有哪些？

### 9.1 短期低风险优化

- 历史页展示 `provider + accuracy + eventType` 标签
- 手动放置记录改成最近 N 条而不是单条覆盖
- 通知点击增加更多上下文入口（比如直接跳历史页特定事件）
- README 同步一份“ROM 设置建议”

### 9.2 中期架构优化

- 引入 `FusedLocationProviderClient`
- 用 `ViewModel + LiveData/StateFlow` 管理前台界面状态
- 把手动记录、设置项逐步迁到 Room
- 增加统一的日志与调试页

### 9.3 长期演进方向

- 支持多设备守护，而不只是单耳机
- 引入事件规则引擎（例如“家外断开才提醒”“速度高时标记可能落在车上”）
- 与 Wear OS、通知历史或日历时间线做联动，增强“回忆”能力

---

## 10. 风险与边界

### 10.1 最脆弱的点

- Android ROM 对后台广播和定位的限制
- 蓝牙耳机广播行为本身不稳定
- 后台定位不一定能及时拿到高质量坐标
- 不同机型对 Companion Device 支持差异较大

### 10.2 典型触发条件

- 用户把 App 强行停止
- 系统省电策略收紧
- 耳机进盒、没电、休眠
- 室内或车内定位环境差
- 用户只给了前台定位权限，没有始终允许

### 10.3 当前缓解策略

- 广播 + Companion 双链路
- current location + last known location 双策略
- Room 持久化 + SharedPreferences 轻量配置分层
- 地图 fallback、通知 fallback、功能降级运行

但需要诚实地说：

> 这些是“尽量减少丢失后的混乱”，不是“彻底解决所有耳机找回问题”。

---

## 11. 学习者思考题

1. 如果要把“只支持 1 个耳机”扩展成“支持多个耳机同时守护”，你会先改 `BoundDeviceStore`，还是先改 `LostEvent` 数据模型？为什么？
2. 如果你决定把定位改成 `FusedLocationProviderClient`，你会怎么处理没有 GMS 的设备？是双实现，还是直接放弃兼容？
3. 如果某些 ROM 不再稳定投递 `ACL_CONNECTED`，你会如何在不引入前台服务的前提下提高可靠性？

---

## Open Questions / Uncertainties

- 当前项目是否已经在所有目标 ROM 上实测过 `ACL_CONNECTED` 与 `ACL_DISCONNECTED` 的稳定性：**证据不足**
- `CompanionDeviceManager` 在小米/OPPO/vivo 等 ROM 上的真实稳定性：**需要更多真机验证**
- 目前定位策略虽然已做“多源选优”，但是否足以覆盖地库、出租车、室内等弱 GPS 场景：**需要更多现场数据验证**

---

## 给未来阅读这篇文档的你

如果你以后想重新进入这个项目，建议按下面顺序回看代码：

1. `README.md`：先找产品边界和用户流程
2. `BoundDeviceStore.java` + `BoundDevice.java`：先理解“项目如何识别目标耳机”
3. `BluetoothStateReceiver.java` + `DeviceMonitor.java`：再理解“系统事件如何进入业务链路”
4. `LostEventRepository.java`：再看“定位、写库、通知如何被统一协调”
5. `LocationSnapshotProvider.java`：最后看“为什么定位结果会有误差、作者如何做精度优化”

这样阅读，你会比从某个 Activity 一头扎进去更快理解整个系统。
