# SwipePP (FlipYear)

**SwipePP**（曾用名 FlipYear）是一款本地优先的 Android 3D 照片与视频整理应用：将相册按年份装进 3D 书架上的年度影集，结合不规则杂志拼图封面、双模式翻书月历、时间刻度弧形滑动清理、连拍智能堆叠、多阶无级缩放画廊、足迹地图和摇一摇撤销，让"清理相册"变成一次翻阅典藏年鉴的体验。全部数据保存在本机，无需联网账号。

- 应用名：`SwipePP`（包名 `com.aistudio.flipyear.xkqmvz`，源码包路径 `com.example`）
- SDK：`minSdk 24` · `targetSdk 36` · `compileSdk 36.1` · Kotlin + Jetpack Compose 单 Activity 架构

## 核心功能

**影集书架（Bookshelf）**
- 按年份自动归集本地照片与视频，生成自适应 3D 书架轮播年度影集，提供顶部媒体类型筛选（全部 / 照片 / 视频）与存储及压缩节省账本（Storage Ledger）
- 年度封面四种模式（`YearbookCoverMode`）：
  - **智能默认**：跟随全局偏好（智能不规则拼图或精选轮播），自动过滤截屏与低质图
  - **不规则拼图（Custom Collage）**：内置 9 套模板（杂志斜切 / 拍立得手账 / 艺术嵌窗，2–6 图），支持间距与底色（装帧色 / 暖白纸 / 曜石黑）调节、槽位点选互换、单格双指缩放与平移构图，并可一键离屏渲染 `1400×2000` 高清封面导出至系统相册
  - **单图定格**：锁定 1 张代表作作为静态典藏封面
  - **多图轮播**：自选 2–6 张年度高光照片动态轮播，支持节奏调节（3s / 5s / 8s）与系统减弱动画（Reduced Motion）自动降级

**画廊时间轴（Gallery）**
- 支持全局正序（最早优先）/ 倒序（最新优先）切换、月度摘要印章与可拖拽时间轴快滑标尺（Fast-scrub Thumb）
- 双指捏合四阶缩放视图（`GalleryZoomTier`）：`4列紧凑网格` ↔ `3列标准网格` ↔ `等高自然比例大图瀑布流` ↔ `沉浸单列大图流`
- 智能筛选胶囊：一键过滤**连拍组**、**大图（≥3MB）**、**缺失定位**以及自定义相册（Bucket）
- 时间轴高级搜索：关键词 + 日期范围 + 拍摄地点 + 自动提取描述标签联合筛选
- 连拍自动堆叠为 Burst 组，支持展开/收起、组内对比与"仅保留最佳"
- 多选与批处理：批量收藏、保留、移入回收站、针对选中项发起自定义弧形清理、移动到自定义相册（可选同步重归档年月）
- 安全批量压缩（`NEAR_LOSSLESS` / `BALANCED` / `COMPACT` 三档）：生成新压缩图并保留 EXIF 时间、机型与 GPS 定位，原图自动暂存回收站可随时还原；未变小或视频文件自动跳过
- 批量补全/编辑定位：基于 24 小时内临近已定位照片启发式推断（同日匹配），或通过预设城市/地理编码手动写入（可选同步写入 EXIF）

**翻书月历（Flipbook）**
- 逐页翻阅年度内的月份页面，完成当月全部待审项目后自动加盖月度整理完成印章
- 支持**月历网格**与**月内画廊**双视图切换（月内画廊同样支持四阶缩放、连拍管理与批量编辑），点击任意日期可直达当天弧形清理，并可配置是否显示空白月份

**弧形清理（Arc Cleaner）**
- 支持按年、月、指定日、足迹地点、无定位媒体或画廊自定义多选集合进入逐张/逐组清理流
- 上滑移入回收站、下滑保留、收藏切换；遇到连拍组可打开连拍对比面板一键"仅保留最佳"或整组处理
- 底部交互式贝塞尔曲线时间刻度条（`ArcTimeScaleScrubber`）：按跨度自动适配小时/天/月刻度，点击或拖拽即可跳转到对应时刻卡片
- 摇一摇手机撤销当前清理会话中的操作（加速度传感器 + 分级触觉反馈），并在内存中维护完整 `UndoSnapshot` 撤销栈；完成清理后展示统计庆祝页并支持直达本次暂存的回收站项目

**足迹地图（Footprint Map）**
- 将带 GPS 信息的媒体聚合为地点簇渲染在交互式 Slippy 地图上，内置中国大陆 `WGS-84 → GCJ-02` 坐标纠偏与暗夜暗房地图色相保持滤镜
- 四种底图图层切换：**中文（高德）** / **国际（ArcGIS）** / **OSM** / **离线矢量网格**，瓦片受限时提供一键重试
- 右侧地点快滑轨（`FootprintLocationScrubRail`）支持上下拖动预览并飞往目标城市，同时汇总无定位媒体入口，支持后台增量回填 EXIF GPS 与一键清理该地媒体

**回收站（Trash Vault）**
- 所有删除操作先暂存回收站，支持单张还原、多选部分还原/清理、按单次清理会话过滤查看，二次确认后才通过 MediaStore 系统授权真正删除并释放存储空间

**偏好设置、引导与外观**
- 统一偏好设置抽屉（`AppSettingsSheet`）：全局时间顺序、默认启动页（影集 / 画廊 / 足迹）、月历默认视图、空白月份开关、画廊默认网格密度、年度封面默认样式与媒体库维护（重扫、权限、移除示例影集）
- 首次启动提供可跳过的手势交互引导层（`OnboardingGestureGuideOverlay`）
- 全屏暗房灯箱（`PhotoDetailLightbox`）：依托窗口级 `FullScreenOverlayHost` 突破底部导航裁剪，支持左右翻页、双指/双击缩放、内联视频播放、下滑关闭与操作后自动推进
- **日光画廊 / 暗夜暗房**双主题（跟随系统或手动切换，首帧同步零闪烁），Edge-to-edge 渲染动态适配刘海与导航栏 insets；大屏（宽度 `≥640dp`）自动从底部悬浮毛玻璃胶囊栏切换为侧边 `NavigationRail`
- 内置 Plus Jakarta Sans / Outfit / Playfair Display / JetBrains Mono 字体

## 技术栈

| 层 | 选型 |
|---|---|
| UI | Jetpack Compose（Material 3、Navigation-Compose、自定义 Canvas / Layout 瀑布流与多边形拼图）、Edge-to-edge |
| 架构 | MVVM + 纯函数派生层：`MainActivity` → `FlipYearApp` → `FlipYearViewModel` + `LibraryDerivationEngine` → `PhotoRepository` |
| 持久化 | Room v6（KSP，含 v2→v6 完整数据库迁移）+ `SharedPreferences`（首帧同步主题与用户偏好） |
| 媒体与文件 | MediaStore API（增量扫描、分区存储写请求 `createWriteRequest` / `createDeleteRequest`、部分照片授权 `READ_MEDIA_VISUAL_USER_SELECTED`） |
| 图像与导出 | Coil（含视频帧解码与分级降采样）、`ExifInterface`、离屏 Canvas 高清拼图渲染（`YearbookCollageExporter`）与 Bitmap 压缩管线 |
| 视频 | Media3 ExoPlayer 内联播放 |
| 传感器 | 加速度计摇一摇检测（`ShakeDetector`）、分级触觉反馈引擎（`HapticPatternEngine`） |
| 构建 | AGP 9.1.1 + Kotlin + KSP + Gradle 9.3.1 Version Catalog（`gradle/libs.versions.toml`）+ Secrets Gradle Plugin |

## 项目结构

```text
app/src/main/java/com/example/
├── FlipYearApplication.kt             # Application：Coil ImageLoader 与内存回收回调
├── MainActivity.kt                    # 单 Activity 入口、Edge-to-edge 与首帧偏好注入
├── data/                              # 数据与系统交互层
│   ├── LocalMediaScanner.kt           # MediaStore 扫描、权限检测、EXIF GPS 回填与地理编码缓存
│   ├── PhotoRepository.kt             # 统一数据仓库
│   ├── FlipYearDatabase.kt            # Room 数据库（v6）与版本迁移
│   ├── FlipYearDao.kt                 # Room DAO
│   ├── PhotoEntities.kt               # 媒体、月度印章、统计、封面与拼图变换实体
│   ├── BatchMediaEditor.kt            # 批量安全压缩、同日定位推断与 EXIF 定位写入
│   ├── YearbookCollageExporter.kt     # 1400×2000 离屏高清拼图渲染与相册导出
│   ├── FlipYearImageLoaders.kt        # Coil 加载器工厂与降采样策略
│   └── SampleArchiveSeeder.kt         # 内置演示影集种子数据
├── sensor/
│   ├── ShakeDetector.kt               # 加速度传感器摇一摇检测
│   └── HapticPatternEngine.kt         # 触觉反馈封装
└── ui/
    ├── FlipYearApp.kt                 # NavHost、悬浮胶囊 Dock / NavigationRail、全局任务进度与 Snackbar
    ├── FlipYearViewModel.kt           # 全局状态编排、撤销栈与批处理任务调度
    ├── FlipYearUiState.kt             # UI 状态模型与预计算画廊索引（PrecomputedGalleryIndex）
    ├── TimelineSearchFilter.kt        # 时间轴关键词、日期、地点与标签筛选引擎
    ├── YearbookCoverCurator.kt        # 年度封面智能策展与模式解析
    ├── domain/
    │   └── LibraryDerivationEngine.kt # O(N) 线性派生引擎（影集聚合、连拍分组、月历建模、足迹聚类）
    ├── screens/                       # 顶层页面与子模块
    │   ├── BookshelfScreen.kt         # 3D 影集书架页（子组件位于 screens/bookshelf/）
    │   ├── GalleryScreen.kt           # 画廊时间轴页（批量操作栏位于 screens/gallery/）
    │   ├── FlipbookScreen.kt          # 年度翻书月历与月内画廊页
    │   ├── ArcCleanerScreen.kt        # 弧形滑动清理页（卡片、连拍对比与完成页位于 screens/cleaner/）
    │   ├── FootprintMapScreen.kt      # 足迹地图页
    │   └── TrashVaultScreen.kt        # 回收站页
    ├── components/                    # 可复用组件（拼图引擎/工坊、瀑布流、Slippy 地图、时间刻度弧、灯箱、设置抽屉、新手引导等）
    └── theme/                         # 日光画廊 / 暗夜暗房双主题、字体与 UserPreferences
```

路由：`bookshelf` · `gallery` · `footprint_map` · `trash_vault` · `flipbook/{year}` · `arc_cleaner?year=…&month=…&day=…&location=…&includeReviewed=…&source=…&custom=…`

## 构建与运行

**环境要求**
- JDK 17+（编译字节码目标为 Java 11，已配置 Foojay Toolchain 自动解析）
- Android SDK（`compileSdk 36`，minor API level `1`）
- Gradle `9.3.1`（主分支未内置 `gradlew`，可直接用本机 Gradle 9.3.1，或先执行 `gradle wrapper` 生成）

```bash
# 编译 Debug APK
gradle :app:assembleDebug

# 安装到已连接设备/模拟器
gradle :app:installDebug

# 运行 JVM 单元测试与 Robolectric UI 测试
gradle :app:testDebugUnitTest

# 运行仪器测试（需要运行中的模拟器/真机）
gradle :app:connectedDebugAndroidTest
```

**首次启动**
1. 克隆仓库后在根目录创建 `local.properties`，写入 `sdk.dir=/path/to/Android/Sdk`。
2. 如需自定义环境变量，复制 `.env.example` 为 `.env`（当前无必填密钥；`MAPS_API_KEY` 为可选预留项，足迹地图使用公开瓦片与离线矢量图层，不依赖该 Key）。
3. 安装启动后可按提示授予照片/视频访问权限；若设备无媒体或希望快速预览，可点击**"加载示例影集"**体验完整功能（后续可在偏好设置中一键移除示例数据）。

**Release 签名**

Release 构建从环境变量读取签名配置：

```text
KEYSTORE_PATH   # 默认 ${rootDir}/my-upload-key.jks
STORE_PASSWORD
KEY_PASSWORD    # keyAlias 固定为 upload
```

## 权限说明

| 权限 | 用途 |
|---|---|
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | 读取本地照片与视频库（Android 13+） |
| `READ_MEDIA_VISUAL_USER_SELECTED` | 支持 Android 14+ 部分照片/视频选择性授权 |
| `READ_EXTERNAL_STORAGE`（`maxSdkVersion="32"`） | Android 12L 及以下版本媒体读取 |
| `ACCESS_MEDIA_LOCATION` | 读取原图未脱敏 EXIF GPS 坐标，用于足迹地图聚类与同日定位推断 |
| `INTERNET` / `ACCESS_NETWORK_STATE` | 加载在线地图瓦片（离线矢量图层无需网络） |
| `VIBRATE` | 滑动清理、刻度跳转与摇一摇撤销触觉反馈 |

> 注：批量压缩生成新文件、写入 EXIF 定位或从回收站永久删除系统媒体时，均遵循 Android 分区存储规范，通过系统 `MediaStore` 弹窗请求用户单次授权确认。

## 测试

`app/src/test/java/com/example/` 下包含 13 个 JVM 单元测试与 Robolectric Compose UI 测试文件（覆盖弧形清理与摇一摇撤销、书架筛选切换、全屏灯箱布局与导航栏 Insets、毛玻璃遮罩点击穿透、双主题切换、时间轴月度印章、高级搜索筛选、封面智能策展、安全压缩与定位推断，以及 `LibraryDerivationEngine` 线性派生重构验证）；`app/src/androidTest/` 提供仪器测试入口。

## 备注

- `google-services` 插件以 `MissingGoogleServicesStrategy.WARN` 容错模式启用：未放置 `google-services.json` 也可直接编译运行；Firebase / Google Maps 相关依赖在 `app/build.gradle.kts` 中默认处于注释预留状态。
- `.env` 由 Secrets Gradle Plugin 注入 `BuildConfig`，已加入 `.gitignore`，请勿提交真实密钥。
