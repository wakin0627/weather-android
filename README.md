# 天气 App（Android）

街道级天气 App，Apple Weather 视觉。Kotlin + Jetpack Compose，单 Activity 三页（天气 / 地图 / 我的）。

> 本仓库只包含 Android 客户端。服务端（天气聚合、网格插值、账户）不在开源范围内，通过 `local.properties` 里的 `apiBase` 指向自建实例。

## 功能

- **街道级定位**：逆地理到街道 / 社区 / POI，精确到米
- **天空引擎**：按天气码实时渲染雨、雪、云、星、雾、雷暴
- **地图页**：高德底图 + 风场粒子（未来 12 小时，逐 3 小时）+ 温度层 + 云量层
- **生活指南**：穿衣、带伞、洗车、晾晒、运动窗口
- **逐小时 / 15 天预报**、空气质量
- **定位与多城市**：首次启动申请定位权限，逆地理到街道；城市面板支持搜索（小区/地标）、收藏、切换
- **液态玻璃底栏**：Haze 真实背景模糊 + 镜面扫光 + 弹簧选中胶囊
- **下拉刷新**
- **应用内更新**：检查 GitHub Releases → 下载 → 拉起系统安装器

## 构建

需要 JDK 17、Android SDK 34。

```bash
# 1. 本地配置（不入库）
cat > local.properties <<EOF
sdk.dir=/path/to/android-sdk
apiBase=https://your-server.example.com
EOF

# 2. 构建
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

`apiBase` 为空时会显示「未配置服务端地址」，不会崩溃。

## 发布与更新

发版只需要一个 tag + 两个附件，客户端自己会读：

```bash
GITHUB_TOKEN=xxx ./release.sh "更新说明"
```

脚本会：改 tag → 构建 → 生成 `version.json`（versionCode / versionName / notes / sha256）→ 建 Release → 上传 `version.json` 和 `weather-vX.Y.apk`。

客户端更新流程：

1. `GET https://api.github.com/repos/<owner>/<repo>/releases/latest`
2. 从附件里取 `version.json` 拿到 `versionCode`
3. 与本地 `BuildConfig.VERSION_CODE` 比较，新的就弹窗
4. 下载 `.apk` 附件 → `FileProvider` → 系统安装器

**两级兜底**（国内网络实测必需）：

- 检查：`api.github.com` 通就用它；不通退回自建服务端 `/api/app/version`（服务端再代理 GitHub）
- 下载：先直连 GitHub 资产；失败自动改走自建服务端 `/api/app/apk?tag=vX.Y.Z` 中转

`release-assets.githubusercontent.com` 在国内会被 DNS 污染，直连基本必失败，所以中转这条后路不是可选而是必需。

## 结构

```
app/src/main/java/com/yoho/weather/
├── MainActivity.kt     三页导航 + 更新弹窗
├── Api.kt              API 客户端与数据模型
├── WeatherScreen.kt    天气首页（天空引擎、生活指南、逐小时、15 天）
├── MapScreen.kt        地图页（osmdroid 底图、风场粒子、温度/云量栅格）
├── MeScreen.kt         我的
├── UpdateManager.kt    版本检查、下载、安装
└── Theme.kt            设计 token
```

## 数据来源

天气与风场来自 [Open-Meteo](https://open-meteo.com)（CC-BY 4.0）；地址与底图来自高德；图标为 [Meteocons](https://github.com/basmilius/weather-icons)（MIT）。

个人项目，不作商业气象服务，数据仅供参考。
