# 行情小部件

给小米 17 Ultra / HyperOS 用的 **4×1 桌面小部件**（250×40dp）。整块界面用 Canvas 画成一张 Bitmap，字体、字重、圆角、间距都精确可控，不受 `RemoteViews` 排版限制。

**三列等权紧凑布局**：每列两行，上排是品种标签 + 24h 涨跌，下排是价格，中间用细竖线分隔。显示上金所 Au99.99（元/克）、BTC/USDT、ETH/USDT。界面上标签简写成「黄金 / BTC / ETH」，BTC 与 ETH 的报价口径是 **USDT**（见数据源）。**涨红跌绿**，深浅色跟随系统，点一下立即刷新。

平时不显示任何时间戳；只有刷新失败、正在显示上次缓存值时，右上角才出现「缓存」角标。

## 数据源

| 数据 | 接口 | 说明 |
| --- | --- | --- |
| 黄金 Au99.99 | `https://hq.sinajs.cn/list=gds_AU9999` | 必须带 `Referer: https://finance.sina.com.cn`；返回 GBK 文本，字段 0=最新价、7=昨收，涨跌幅本地计算 |
| BTC | `https://api.gateio.ws/api/v4/spot/tickers?currency_pair=BTC_USDT` | 免 key，取 `last`、`change_percentage` |
| ETH | 同上，`ETH_USDT` | Gate 一次只认一个交易对，两个请求并发 |

三个请求并发，各自 6 秒超时。**任一失败就沿用该项上次的值**，右上角出现「缓存」。没有引入新浪 BTC/USD 指数做兜底——同一块界面里混 USDT 和 USD 两种口径容易看错价，宁可显示旧值并明确标注。

零第三方依赖：网络用 `HttpURLConnection`，JSON 用平台自带 `org.json`，没有 AndroidX / Kotlin / WorkManager。

## 出 APK（GitHub Actions）

本机没装 JDK 和 Android SDK，所以用云编译。GitHub 的 Ubuntu runner 自带 JDK 17 + Android SDK，workflow 已写好。

1. 在 GitHub 新建一个**空仓库**（不要勾 README / license，避免首次 push 冲突）
2. 把这个目录里**所有文件**（含隐藏的 `.github/` 和 `.gitignore`）放进去
   - 网页方式：把交付的 `market-widget.zip` 解压，在仓库页 Drag & Drop 上传，注意 `.github` 目录要一起传
   - 命令行方式：
     ```bash
     cd market-widget
     git init
     git remote add origin git@github.com:<你的用户名>/<仓库名>.git
     git add -A
     git commit -m "init market widget"
     git branch -M main
     git push -u origin main
     ```
3. 打开仓库的 **Actions** → `Build debug APK`，也可以在 Actions 页手动 `Run workflow`
4. 跑完在该次运行的 **Artifacts** 下载 `market-widget-debug`，解压得到 `app-debug.apk`

`main` 和 `master` 分支 push 都会触发。以后改样式只要 push，就会重新出包。

## 装到小米手机

1. 把 `app-debug.apk` 传到手机（微信文件传输 / 数据线 / 局域网都行），点开安装
2. 如果提示被拦截：设置 → 应用设置 → **纯净模式** → 关掉，或选「仍允许安装」；并给来源 App 开「安装未知应用」
3. 长按桌面空白处 → 小部件 / 添加工具 → 找到「行情」→ 拖到桌面
4. **后台白名单**（不做这步，30 分钟定时刷新可能被 HyperOS 冻结）：
   - 设置 → 应用设置 → 应用管理 → 行情 → **省电策略 → 无限制**
   - 同页面 → **自启动 → 开启**
   - 最近任务里把这个应用的卡片下拉加锁

## 想改的话

| 改什么 | 动哪个文件 |
| --- | --- |
| 配色 / 字号 / 布局 | `app/src/main/java/com/leox/marketwidget/WidgetRenderer.java`（设计画布 **300×48**，`DESIGN_W/H`；三列坐标 `PAD` / `INSET` / `LABEL_Y` / `PRICE_Y`） |
| 尺寸 / 刷新周期 | `app/src/main/res/xml/market_widget_info.xml`（`targetCellHeight=1`、`minHeight=40dp`） |
| 数据源 / 超时 / 解析 | `app/src/main/java/com/leox/marketwidget/QuoteFetcher.java` |
| 刷新调度 / 点击事件 | `app/src/main/java/com/leox/marketwidget/MarketWidget.java` |

拉伸小部件时，内容按 300×48 等比居中，不会变形，多出来的部分由背景填满。价格统一保留 2 位小数。

## 已知限制

- `updatePeriodMillis` 系统最小 30 分钟，改小也无效；灭屏和省电策略下可能进一步延后。要即时数据点一下小部件。
- 上金所非交易时段，新浪返回的是最近一笔收盘价，涨跌幅按昨收计算。
- 4×1 高度只有 40dp，BTC 价格涨到 7 位数时列宽会偏紧，必要时把 `PRICE_Y` 上方的字号从 14f 调到 13f。
- debug APK 是自签名，仅限自用。长期使用建议自己签 release。
- 工程未经本地编译验证（本机无 JDK/SDK），首次云编译请以 Actions 日志为准。
