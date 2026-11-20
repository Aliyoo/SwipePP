# FlipYear (SwipePP)

FlipYear 是一款本地优先的 Android 照片/视频整理应用：把相册按年份装进 3D 书架上的年度影集，用翻书式月历、弧形滑动清理、连拍智能堆叠、足迹地图和摇一摇撤销，让"清理相册"变成一次翻阅年鉴的体验。全部数据保存在本机，无需联网账号。

- 包名：`com.aistudio.flipyear.xkqmvz`（源码目录为 `com.example`）
- minSdk 24 · targetSdk 36 · Kotlin + Jetpack Compose 单 Activity 架构

## 核心功能

**影集书架（Bookshelf）**
- 按年份自动归集本地媒体，生成 3D 书架上的年度影集
- 年度封面三种模式：智能推荐（过滤截屏/低质图）、单张典藏封面、自选 2–6 张轮播封面
- 支持封面轮播节奏调节（3s / 5s / 8s）与"换封面"工作室

**画廊时间轴（Gallery）**
- 按时间倒序的媒体流，日期跳转月份印章与快速月份跳转条
- 连拍照片自动堆叠为 Burst 组，可展开/收起、对比连拍、"仅保留最佳"
- 长按进入多选：批量收藏、保留、删除、移动到自定义相册并可选重新归档年月
- 时间轴搜索：关键词 + 日期范围 + 拍摄地点 + 描述标签的高级筛选
- 批量近无损压缩（`NEAR_LOSSLESS` / `BALANCED` / `COMPACT` 三档），自动保留 EXIF 拍摄时间、机型与定位；压缩后未变小则自动跳过原图
- 批量补全/编辑定位：按"同日已有定位照片"启发式匹配，或手动指定地点

**翻书月历（Flipbook）**
- 每个年份影集可逐页翻阅的月份日历，点击任意日期直达当天整理

**弧形清理（Arc Cleaner）**
- 按年/月/日或地点进入逐张清理流：滑动卡片做保留 / 移入回收站 / 收藏决策
- 摇一摇手机撤销误删（加速度传感器 + 震动反馈）
- 撤销快照（UndoSnapshot）在内存中保留操作前的完整状态

**足迹地图（Footprint Map）**
- 带定位信息的媒体聚合为地点簇，渲染在可交互的 Slippy 地图上
- 缩放/平移/图层切换/纵览全部足迹；点击簇可直接清理该地媒体
- 地图瓦片来自公开 OSM 系服务，加载受限时提供重试入口

**回收站（Trash Vault）**
- 移入回收站的项目先暂存，二次确认后才会调用系统接口真正删除并释放空间
- 支持单张还原与全部还原

**外观**
- 日光画廊 / 暗夜暗房双主题，可跟随系统或手动切换
- Edge-to-edge 渲染，动态适配刘海与导航栏 insets
- 内置 Plus Jakarta Sans / Outfit / Playfair Display / JetBrains Mono 字体

## 技术栈

| 层 | 选型 |
|---|---|
| UI | Jetpack Compose（Material 3、Navigation-Compose、动画）、Edge-to-edge |
| 架构 | MVVM：`MainActivity` → `FlipYearApp`（NavHost）→ `FlipYearViewModel` → `PhotoRepository` |
| 持久化 | Room（KSP）：媒体元数据、月度印章、清理统计、封面配置 |
| 媒体扫描 | MediaStore API（`LocalMediaScanner`），支持部分照片授权（`READ_MEDIA_VISUAL_USER_SELECTED`） |
| 图像 | Coil（含视频帧解码）、ExifInterface、Bitmap 压缩管线 |
| 视频 | Media3 ExoPlayer 内联播放 |
| 传感器 | 加速度计摇一摇检测、Haptic 震动反馈 |
| 构建 | Gradle Kotlin DSL + Version Catalog（`gradle/libs.versions.toml`）、Secrets Gradle Plugin |

## 项目结构

```
app/src/main/java/com/example/
├── FlipYearApplication.kt        # Application：Coil ImageLoader 与内存事件
├── MainActivity.kt               # 单 Activity 入口
├── data/                         # 数据层
│   ├── LocalMediaScanner.kt      # MediaStore 全量/增量扫描
│   ├── PhotoRepository.kt        # 统一数据仓库
│   ├── FlipYearDatabase.kt       # Room 数据库
│   ├── FlipYearDao.kt            # DAO
│   ├── PhotoEntities.kt          # 实体与状态枚举
│   ├── BatchMediaEditor.kt       # 批量压缩 / EXIF / 定位编辑
│   ├── FlipYearImageLoaders.kt   # Coil 加载器工厂
│   └── SampleArchiveSeeder.kt    # 示例影集（演示数据）
├── sensor/
│   ├── ShakeDetector.kt          # 摇一摇检测
│   └── HapticPatternEngine.kt    # 震动模式
└── ui/
    ├── FlipYearApp.kt            # NavHost、底部导航、路由
    ├── FlipYearViewModel.kt      # 全局状态与业务逻辑
    ├── FlipYearUiState.kt        # UI 状态模型
    ├── TimelineSearchFilter.kt   # 搜索/筛选引擎
    ├── YearbookCoverCurator.kt   # 封面智能挑选
    ├── screens/                  # Bookshelf/Gallery/Flipbook/Map/Vault/ArcCleaner
    ├── components/               # 可复用组件（灯箱、连拍网格、地图、主题面板等）
    └── theme/                    # 双主题与字体
```

路由：`bookshelf` · `gallery` · `footprint_map` · `trash_vault` · `flipbook/{year}` · `arc_cleaner?…`

## 构建与运行

**环境要求**
- JDK 17+
- Android SDK（compileSdk 36）
- Gradle 9.3.1（仓库未内置 `gradlew`，可直接用本机 Gradle 9.3.1，或先执行 `gradle wrapper` 生成）

```bash
# 编译
gradle :app:assembleDebug

# 安装到已连接设备/模拟器
gradle :app:installDebug

# 单元测试（含 Robolectric）
gradle :app:testDebugUnitTest

# 仪器测试（需要运行中的模拟器/真机）
gradle :app:connectedDebugAndroidTest
```

**首次启动**
1. 克隆后在根目录创建 `local.properties`，写入 `sdk.dir=/path/to/Android/Sdk`。
2. 如需自定义配置，复制 `.env.example` 为 `.env`（当前无必填密钥；`MAPS_API_KEY` 为可选预留项，足迹地图使用公开瓦片源，不依赖该 key）。
3. 构建并安装后，按提示授予照片/视频访问权限；无媒体库时可点"加载示例影集"体验完整流程。

**Release 签名**

Release 构建读取环境变量：

```
KEYSTORE_PATH   # 默认 ${rootDir}/my-upload-key.jks
STORE_PASSWORD
KEY_PASSWORD    # keyAlias 固定为 upload
```

## 权限说明

| 权限 | 用途 |
|---|---|
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | 读取本地媒体库（Android 13+） |
| `READ_MEDIA_VISUAL_USER_SELECTED` | 支持 Android 14+ 部分照片授权 |
| `READ_EXTERNAL_STORAGE`（≤SDK 32） | 旧版本媒体访问 |
| `ACCESS_MEDIA_LOCATION` | 读取照片 EXIF 定位用于足迹地图与批量补定位 |
| `INTERNET` / `ACCESS_NETWORK_STATE` | 地图瓦片加载 |
| `VIBRATE` | 清理/撤销触觉反馈 |

## 测试

`app/src/test/` 下为 JVM 单元测试与 Robolectric UI 测试（覆盖连拍撤销、灯箱布局、书架筛选、主题切换、批量压缩、时间戳印章、搜索筛选等）；`app/src/androidTest/` 为仪器测试模板。Compose UI 层验证约定：使用 uiautomator 语义 bounds + 坐标探针，不依赖 Compose 坐标 API。

## 备注

- `google-services` 插件以 `WARN` 容错模式启用：未放置 `google-services.json` 也能正常构建；Firebase/Maps 相关依赖已在 `app/build.gradle.kts` 中预留（注释状态）。
- `.env` 由 Secrets Gradle Plugin 注入 `BuildConfig`，已加入 `.gitignore`，请勿提交真实密钥。
