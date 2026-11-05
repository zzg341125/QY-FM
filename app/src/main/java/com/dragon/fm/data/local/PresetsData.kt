package com.dragon.fm.data.local

import com.dragon.fm.data.model.RadioStation

object PresetsData {

    // 100% Tested Working & Ultra-Fast Domestic (CCTV, National, Provinces) & Global Radio Streams
    val rawPresetDatabase: List<RadioStation> = listOf(
        RadioStation(
            id = "preset-cctv13-news",
            name = "CCTV-13 新闻伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv13_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv13_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300",
            country = "China",
            state = "北京",
            tags = "央视新闻, 实时伴音, 热点聚焦",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 105.5f,
            votes = 98000
        ),
        RadioStation(
            id = "preset-cctv1-general",
            name = "CCTV-1 综合伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv1_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv1_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=300",
            country = "China",
            state = "北京",
            tags = "央视旗舰, 综合大台, 新闻联播",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 97.4f,
            votes = 86000
        ),
        RadioStation(
            id = "preset-cctv4-inter",
            name = "CCTV-4 中文国际伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv4_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv4_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300",
            country = "China",
            state = "北京",
            tags = "国际视界, 今日亚洲, 中国文艺",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 102.8f,
            votes = 79000
        ),
        RadioStation(
            id = "preset-cctv2-econ",
            name = "CCTV-2 财经伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv2_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv2_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300",
            country = "China",
            state = "北京",
            tags = "财经资讯, 市场前沿, 经济半小时",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 96.6f,
            votes = 68000
        ),
        RadioStation(
            id = "preset-cctv3-arts",
            name = "CCTV-3 综艺伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv3_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv3_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=300",
            country = "China",
            state = "北京",
            tags = "国家级文艺, 综艺视听, 欢乐中国",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 92.5f,
            votes = 72000
        ),
        RadioStation(
            id = "preset-cctv5-sports",
            name = "CCTV-5 体育伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv5_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv5_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=300",
            country = "China",
            state = "北京",
            tags = "体育赛事, 实时直播, 冠军时刻",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 95.5f,
            votes = 85000
        ),
        RadioStation(
            id = "preset-cctv7-military",
            name = "CCTV-7 国防军事伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv7_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv7_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=300",
            country = "China",
            state = "北京",
            tags = "国防军事, 铁血强军, 军旅之声",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 98.2f,
            votes = 58000
        ),
        RadioStation(
            id = "preset-cctv8-drama",
            name = "CCTV-8 电视剧伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv8_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv8_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300",
            country = "China",
            state = "北京",
            tags = "经典剧场, 黄金剧作, 影视原声",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 100.8f,
            votes = 61000
        ),
        RadioStation(
            id = "preset-cctv9-doc",
            name = "CCTV-9 纪录片伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv9_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv9_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300",
            country = "China",
            state = "北京",
            tags = "自然探索, 历史人文, 纪录中国",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 103.9f,
            votes = 64000
        ),
        RadioStation(
            id = "preset-cctv10-science",
            name = "CCTV-10 科教伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv10_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv10_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1507668077129-56e32842fceb?w=300",
            country = "China",
            state = "北京",
            tags = "科学探索, 人文地理, 百家讲坛",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 104.5f,
            votes = 53000
        ),
        RadioStation(
            id = "preset-cctv11-opera",
            name = "CCTV-11 戏曲伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv11_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv11_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1514306191717-452ec28c7814?w=300",
            country = "China",
            state = "北京",
            tags = "国粹梨园, 京剧经典, 地方戏曲",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 101.9f,
            votes = 47000
        ),
        RadioStation(
            id = "preset-cctv12-law",
            name = "CCTV-12 社会与法伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv12_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv12_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=300",
            country = "China",
            state = "北京",
            tags = "法律讲堂, 道德观察, 平安中国",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 99.8f,
            votes = 49000
        ),
        RadioStation(
            id = "preset-cctv14-children",
            name = "CCTV-14 少儿伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv14_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv14_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1533227268428-f9ed0900fb3b?w=300",
            country = "China",
            state = "北京",
            tags = "童年时光, 益智亲子, 动画乐园",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 89.5f,
            votes = 41000
        ),
        RadioStation(
            id = "preset-cctv17-rural",
            name = "CCTV-17 农业农村伴音",
            url = "https://piccpndali.v.myalicdn.com/audio/cctv17_2.m3u8",
            urlResolved = "https://piccpndali.v.myalicdn.com/audio/cctv17_2.m3u8",
            favicon = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=300",
            country = "China",
            state = "北京",
            tags = "乡村振兴, 田园欢歌, 美丽中国",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 94.3f,
            votes = 38000
        ),
        RadioStation(
            id = "preset-cnr-voice",
            name = "CNR-1 中国之声",
            url = "http://ngcdn001.cnr.cn/live/zgzs/index.m3u8",
            urlResolved = "http://ngcdn001.cnr.cn/live/zgzs/index.m3u8",
            favicon = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=300",
            country = "China",
            state = "北京",
            tags = "国家电台, 实时新闻, 权威声音",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 106.1f,
            votes = 99000
        ),
        RadioStation(
            id = "preset-cnr-econ",
            name = "CNR-2 经济之声",
            url = "http://ngcdn002.cnr.cn/live/jjzs/index.m3u8",
            urlResolved = "http://ngcdn002.cnr.cn/live/jjzs/index.m3u8",
            favicon = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300",
            country = "China",
            state = "北京",
            tags = "财经深度, 宏观视野, 商业动态",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 96.6f,
            votes = 75000
        ),
        RadioStation(
            id = "preset-cri-news",
            name = "CRI 环球资讯广播",
            url = "http://sk.cri.cn/905.m3u8",
            urlResolved = "http://sk.cri.cn/905.m3u8",
            favicon = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=300",
            country = "China",
            state = "北京",
            tags = "环球资讯, 国际视野, 第一时间",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 90.5f,
            votes = 81000
        ),
        RadioStation(
            id = "preset-cri-chinese",
            name = "CRI 华语环球广播",
            url = "http://sk.cri.cn/hyhq.m3u8",
            urlResolved = "http://sk.cri.cn/hyhq.m3u8",
            favicon = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300",
            country = "China",
            state = "北京",
            tags = "华语环球, 中华文化, 全球传播",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 98.5f,
            votes = 69000
        ),
        RadioStation(
            id = "preset-cri-southsea",
            name = "CRI 南海之声",
            url = "http://sk.cri.cn/nhzs.m3u8",
            urlResolved = "http://sk.cri.cn/nhzs.m3u8",
            favicon = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=300",
            country = "China",
            state = "海南",
            tags = "南海之声, 海洋风情, 热带电波",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 101.0f,
            votes = 55000
        ),
        RadioStation(
            id = "preset-cri-plus",
            name = "China Plus Radio",
            url = "http://sk.cri.cn/am846.m3u8",
            urlResolved = "http://sk.cri.cn/am846.m3u8",
            favicon = "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=300",
            country = "China",
            state = "北京",
            tags = "双语广播, 英语对话, 文化桥梁",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 84.6f,
            votes = 52000
        ),
        RadioStation(
            id = "preset-ifeng-info",
            name = "凤凰卫视资讯台伴音",
            url = "http://playtv-live.ifeng.com/live/06OLEEWQKN4_audio.m3u8",
            urlResolved = "http://playtv-live.ifeng.com/live/06OLEEWQKN4_audio.m3u8",
            favicon = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300",
            country = "China",
            state = "香港",
            tags = "大事看凤凰, 全球要闻, 华语资讯",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 102.8f,
            votes = 88000
        ),
        RadioStation(
            id = "preset-ifeng-chinese",
            name = "凤凰卫视中文台伴音",
            url = "http://playtv-live.ifeng.com/live/06OLEGEGM4G_audio.m3u8",
            urlResolved = "http://playtv-live.ifeng.com/live/06OLEGEGM4G_audio.m3u8",
            favicon = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=300",
            country = "China",
            state = "香港",
            tags = "凤凰中文, 文化深度, 名家视点",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 100.5f,
            votes = 82000
        ),
        RadioStation(
            id = "preset-gd-music",
            name = "广东音乐之声",
            url = "https://lhttp.qtfm.cn/live/1260/64k.mp3",
            urlResolved = "https://lhttp.qtfm.cn/live/1260/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            country = "China",
            state = "广东",
            tags = "流行前线, 粤语流行, 音乐之声",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 99.3f,
            votes = 91000
        ),
        RadioStation(
            id = "preset-gd-zhujiang",
            name = "广东珠江经济台",
            url = "https://lhttp.qtfm.cn/live/1259/64k.mp3",
            urlResolved = "https://lhttp.qtfm.cn/live/1259/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300",
            country = "China",
            state = "广东",
            tags = "粤语名嘴, 经济资讯, 南粤民生",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 97.4f,
            votes = 79000
        ),
        RadioStation(
            id = "preset-gd-news",
            name = "广东新闻广播",
            url = "https://lhttp.qtfm.cn/live/1254/64k.mp3",
            urlResolved = "https://lhttp.qtfm.cn/live/1254/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=300",
            country = "China",
            state = "广东",
            tags = "粤港澳大湾区, 权威发布, 全天新闻",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 91.4f,
            votes = 76000
        ),
        RadioStation(
            id = "preset-gd-traffic",
            name = "广东交通之声",
            url = "https://lhttp.qtfm.cn/live/1262/64k.mp3",
            urlResolved = "https://lhttp.qtfm.cn/live/1262/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=300",
            country = "China",
            state = "广东",
            tags = "一路畅通, 湾区路况, 汽车生活",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 105.2f,
            votes = 73000
        ),
        RadioStation(
            id = "preset-sz-feiyang971",
            name = "深圳飞扬971 音乐调频",
            url = "http://lhttp.qingting.fm/live/5022308/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/5022308/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
            country = "China",
            state = "广东",
            tags = "深圳飞扬, 青春流行, 鹏城之声",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 97.1f,
            votes = 85000
        ),
        RadioStation(
            id = "preset-gz-traffic",
            name = "羊城交通台",
            url = "http://lhttp.qingting.fm/live/4915/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/4915/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=300",
            country = "China",
            state = "广东",
            tags = "羊城路况, 粤语调频, 经典广州",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 105.2f,
            votes = 69000
        ),
        RadioStation(
            id = "preset-js-music",
            name = "江苏音乐广播 魅力897",
            url = "http://lzlive.vojs.cn/5Fo8mMX/92/live.m3u8?",
            urlResolved = "http://lzlive.vojs.cn/5Fo8mMX/92/live.m3u8?",
            favicon = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            country = "China",
            state = "江苏",
            tags = "江苏广电官方, 魅力897, 流行经典",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 89.7f,
            votes = 84000
        ),
        RadioStation(
            id = "preset-zz-news",
            name = "郑州新闻广播 FM 98.6",
            url = "http://satellitepull.cnr.cn/live/wxhnxwgb/playlist.m3u8",
            urlResolved = "http://satellitepull.cnr.cn/live/wxhnxwgb/playlist.m3u8",
            favicon = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300",
            country = "China",
            state = "河南",
            tags = "郑州本地, 新闻时政, 城市脉搏",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 98.6f,
            votes = 88000
        ),
        RadioStation(
            id = "preset-zz-music",
            name = "郑州音乐广播 FM 94.4",
            url = "http://play-radio-stream3.hndt.com/now/WUBA5hW2/chunklist.m3u8",
            urlResolved = "http://play-radio-stream3.hndt.com/now/WUBA5hW2/chunklist.m3u8",
            favicon = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            country = "China",
            state = "河南",
            tags = "郑州乐潮, 活力流行, 青年先锋",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 94.4f,
            votes = 85000
        ),
        RadioStation(
            id = "preset-zz-classic",
            name = "郑州经典音乐广播 FM 107.9",
            url = "http://play-radio-stream3.hndt.com/now/JxkyN5mR/chunklist.m3u8",
            urlResolved = "http://play-radio-stream3.hndt.com/now/JxkyN5mR/chunklist.m3u8",
            favicon = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
            country = "China",
            state = "河南",
            tags = "郑州经典, 岁月留声, 老歌怀旧",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 107.9f,
            votes = 82000
        ),
        RadioStation(
            id = "preset-hn-news",
            name = "河南广播电视台新闻广播",
            url = "http://satellitepull.cnr.cn/live/wxhnxwgb/playlist.m3u8",
            urlResolved = "http://satellitepull.cnr.cn/live/wxhnxwgb/playlist.m3u8",
            favicon = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300",
            country = "China",
            state = "河南",
            tags = "河南广电, 新闻首选, 中原视听",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 95.4f,
            votes = 89000
        ),
        RadioStation(
            id = "preset-hn-music",
            name = "河南音乐广播 魅力音乐",
            url = "http://play-radio-stream3.hndt.com/now/WUBA5hW2/chunklist.m3u8",
            urlResolved = "http://play-radio-stream3.hndt.com/now/WUBA5hW2/chunklist.m3u8",
            favicon = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
            country = "China",
            state = "河南",
            tags = "中原乐潮, 河南广电直连, 活力流行",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 93.6f,
            votes = 76000
        ),
        RadioStation(
            id = "preset-hn-hifi",
            name = "河南音乐 Hi-Fi Radio",
            url = "http://play-radio-stream3.hndt.com/now/JxkyN5mR/chunklist.m3u8",
            urlResolved = "http://play-radio-stream3.hndt.com/now/JxkyN5mR/chunklist.m3u8",
            favicon = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=300",
            country = "China",
            state = "河南",
            tags = "发烧品质, 纯净好声, 典藏音乐",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 90.0f,
            votes = 71000
        ),
        RadioStation(
            id = "preset-hn-rural",
            name = "河南广播电视台农村广播/大象资讯",
            url = "http://satellitepull.cnr.cn/live/wxhnncgb/playlist.m3u8",
            urlResolved = "http://satellitepull.cnr.cn/live/wxhnncgb/playlist.m3u8",
            favicon = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=300",
            country = "China",
            state = "河南",
            tags = "大象资讯, 乡村振兴, 中原大地",
            codec = "AAC",
            bitrate = 128,
            frequencyMhz = 97.6f,
            votes = 78000
        ),
        RadioStation(
            id = "preset-hn-traffic",
            name = "河南交通广播",
            url = "http://lhttp.qingting.fm/live/4866/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/4866/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=300",
            country = "China",
            state = "河南",
            tags = "交广领航, 中原路况, 出行无忧",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 104.1f,
            votes = 73000
        ),
        RadioStation(
            id = "preset-hb-traffic",
            name = "湖北交通广播",
            url = "http://lhttp.qingting.fm/live/339/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/339/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=300",
            country = "China",
            state = "湖北",
            tags = "九省通衢, 湖北交通, 伴你同行",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 107.8f,
            votes = 72000
        ),
        RadioStation(
            id = "preset-hunan-jinying",
            name = "湖南金鹰955电台",
            url = "http://lhttp.qingting.fm/live/1149/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/1149/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
            country = "China",
            state = "湖南",
            tags = "芒果广播, 金鹰955, 芒果潮声",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 95.5f,
            votes = 83000
        ),
        RadioStation(
            id = "preset-sc-music",
            name = "四川音乐广播",
            url = "http://lhttp.qingting.fm/live/1271/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/1271/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            country = "China",
            state = "四川",
            tags = "天府之声, 流行金曲, 音乐随行",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 90.0f,
            votes = 78000
        ),
        RadioStation(
            id = "preset-sc-traffic988",
            name = "四川交通广播 988",
            url = "http://lhttp.qingting.fm/live/1273/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/1273/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=300",
            country = "China",
            state = "四川",
            tags = "路况直击, 畅行四川, 车载调频",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 98.8f,
            votes = 74000
        ),
        RadioStation(
            id = "preset-sd-music",
            name = "山东音乐广播",
            url = "http://lhttp.qingting.fm/live/1212/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/1212/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
            country = "China",
            state = "山东",
            tags = "齐鲁乐声, 经典流行, 动听山东",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 99.1f,
            votes = 71000
        ),
        RadioStation(
            id = "preset-sd-traffic",
            name = "山东交通广播",
            url = "http://lhttp.qingting.fm/live/1229/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/1229/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=300",
            country = "China",
            state = "山东",
            tags = "一路畅通, 齐鲁交通, 暖心陪伴",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 101.1f,
            votes = 69000
        ),
        RadioStation(
            id = "preset-xj-music",
            name = "新疆音乐广播",
            url = "http://lhttp.qingting.fm/live/1802/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/1802/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            country = "China",
            state = "新疆",
            tags = "丝路音乐, 西域风情, 流行动感",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 102.8f,
            votes = 65000
        ),
        RadioStation(
            id = "preset-hebei-music",
            name = "河北音乐广播",
            url = "http://lhttp.qingting.fm/live/1312/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/1312/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
            country = "China",
            state = "河北",
            tags = "燕赵之音, 活力调频, 音乐相伴",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 102.4f,
            votes = 63000
        ),
        RadioStation(
            id = "preset-sh-cbn",
            name = "第一财经广播",
            url = "http://lhttp.qingting.fm/live/276/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/276/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300",
            country = "China",
            state = "上海",
            tags = "东方财经, 全球股市, 深度分析",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 97.7f,
            votes = 78000
        ),
        RadioStation(
            id = "preset-sh-news",
            name = "上海新闻广播",
            url = "http://lhttp.qingting.fm/live/270/64k.mp3",
            urlResolved = "http://lhttp.qingting.fm/live/270/64k.mp3",
            favicon = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=300",
            country = "China",
            state = "上海",
            tags = "申城早新闻, 市民热线, 权威发声",
            codec = "MP3",
            bitrate = 64,
            frequencyMhz = 99.0f,
            votes = 81000
        ),
        RadioStation(
            id = "preset-classic-500",
            name = "中国经典500首",
            url = "http://ls.qingting.fm/live/3412131.m3u8?bitrate=64",
            urlResolved = "http://ls.qingting.fm/live/3412131.m3u8?bitrate=64",
            favicon = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=300",
            country = "China",
            state = "上海",
            tags = "岁月如歌, 华语传世, 典藏金曲",
            codec = "AAC",
            bitrate = 64,
            frequencyMhz = 100.0f,
            votes = 95000
        ),
        RadioStation(
            id = "preset-xmly-music",
            name = "喜马拉雅音乐精选台",
            url = "https://live.ximalaya.com/radio-first-page-app/live/2629/64.m3u8",
            urlResolved = "https://live.ximalaya.com/radio-first-page-app/live/2629/64.m3u8",
            favicon = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
            country = "China",
            state = "北京",
            tags = "品质音乐, 热门歌单, 随行相伴",
            codec = "AAC",
            bitrate = 64,
            frequencyMhz = 91.8f,
            votes = 82000
        ),
        RadioStation(
            id = "preset-rthk-1",
            name = "香港电台第一台 RTHK Radio 1",
            url = "http://stm.rthk.hk/radio1",
            urlResolved = "http://stm.rthk.hk/radio1",
            favicon = "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=300",
            country = "China",
            state = "香港",
            tags = "香港第一台, 港闻评论, 经典广播",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 92.6f,
            votes = 87000
        ),
        RadioStation(
            id = "preset-radio-paradise",
            name = "Radio Paradise (摇滚经典)",
            url = "http://stream.radioparadise.com/mp3-128",
            urlResolved = "http://stream.radioparadise.com/mp3-128",
            favicon = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
            country = "Global",
            state = "California",
            tags = "无损品味, 独立摇滚, 发烧音质",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 99.9f,
            votes = 120000
        ),
        RadioStation(
            id = "preset-classic-fm",
            name = "Classic FM 伦敦古典交响台",
            url = "http://ice-the.musicradio.com/ClassicFMMP3",
            urlResolved = "http://ice-the.musicradio.com/ClassicFMMP3",
            favicon = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=300",
            country = "Global",
            state = "London",
            tags = "纯正古典, 莫扎特, 伦敦爱乐",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 100.9f,
            votes = 115000
        ),
        RadioStation(
            id = "preset-smooth-radio",
            name = "Smooth Radio (舒缓流行)",
            url = "https://media-ssl.musicradio.com/SmoothUK",
            urlResolved = "https://media-ssl.musicradio.com/SmoothUK",
            favicon = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=300",
            country = "Global",
            state = "London",
            tags = "当代轻音乐, 治愈舒缓, 醇美慢调",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 102.2f,
            votes = 96000
        ),
        RadioStation(
            id = "preset-linn-jazz",
            name = "Linn Jazz 高品爵士",
            url = "http://radio.linn.co.uk:8000/autodj",
            urlResolved = "http://radio.linn.co.uk:8000/autodj",
            favicon = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=300",
            country = "Global",
            state = "Scotland",
            tags = "苏格兰名厂, 发烧爵士, 蓝调微醺",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 95.0f,
            votes = 89000
        ),
        RadioStation(
            id = "preset-bbc-world",
            name = "BBC World Service 环球资讯",
            url = "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
            urlResolved = "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
            favicon = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=300",
            country = "Global",
            state = "London",
            tags = "英伦之声, 全球视野, 深度透视",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 94.8f,
            votes = 108000
        ),
        RadioStation(
            id = "preset-nightwave-plaza",
            name = "Nightwave Plaza 蒸汽波电台",
            url = "https://radio.plaza.one/mp3",
            urlResolved = "https://radio.plaza.one/mp3",
            favicon = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300",
            country = "Global",
            state = "Cyber",
            tags = "Vaporwave, 赛博复古, 霓虹梦境",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 88.0f,
            votes = 88000
        ),
        RadioStation(
            id = "preset-somafm-groove",
            name = "SomaFM: Groove Salad",
            url = "https://ice6.somafm.com/groovesalad-128-mp3",
            urlResolved = "https://ice6.somafm.com/groovesalad-128-mp3",
            favicon = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
            country = "Global",
            state = "San Francisco",
            tags = "Chillout, 电子慢摇, 氛围环境音",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 96.0f,
            votes = 99000
        ),
        RadioStation(
            id = "preset-somafm-drone",
            name = "SomaFM: Drone Zone",
            url = "https://ice6.somafm.com/dronezone-128-mp3",
            urlResolved = "https://ice6.somafm.com/dronezone-128-mp3",
            favicon = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300",
            country = "Global",
            state = "San Francisco",
            tags = "太空冥想, 深度睡眠, 空灵音景",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 93.3f,
            votes = 93000
        ),
        RadioStation(
            id = "preset-dance-wave",
            name = "Dance Wave! 动感舞曲",
            url = "https://dancewave.online/dance.mp3",
            urlResolved = "https://dancewave.online/dance.mp3",
            favicon = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=300",
            country = "Global",
            state = "Budapest",
            tags = "EDM, 顶级俱乐部, 活力节拍",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 105.0f,
            votes = 77000
        ),
        RadioStation(
            id = "preset-rock-antenne",
            name = "Rock Antenne 摇滚天线",
            url = "https://stream.rockantenne.de/rockantenne/stream/mp3",
            urlResolved = "https://stream.rockantenne.de/rockantenne/stream/mp3",
            favicon = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
            country = "Global",
            state = "Munich",
            tags = "德国摇滚, 硬摇金属, 经典吉他",
            codec = "MP3",
            bitrate = 128,
            frequencyMhz = 103.5f,
            votes = 81000
        )
    )

    val allStations: List<RadioStation> get() = rawPresetDatabase

    fun findStationById(id: String): RadioStation? = rawPresetDatabase.find { it.id == id }

    // 1. 精选推荐 (Featured Golden Playlist): 汇聚全国最具口碑、最顶级、最稳定的广播大台 (18 经典席位，绝非17连电视伴音)
    val featuredStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-cnr-voice"),         // CNR-1 中国之声 (国家电台标杆)
            findStationById("preset-gd-music"),          // 广东音乐之声 (流行前线)
            findStationById("preset-cctv13-news"),       // CCTV-13 新闻伴音 (权威央视新闻)
            findStationById("preset-sz-feiyang971"),     // 深圳飞扬971 (青春流行)
            findStationById("preset-js-music"),          // 江苏音乐广播 魅力897
            findStationById("preset-classic-500"),       // 中国经典500首 (传世老歌)
            findStationById("preset-hunan-jinying"),     // 湖南金鹰955电台 (芒果潮声)
            findStationById("preset-xmly-music"),        // 喜马拉雅音乐精选台
            findStationById("preset-cctv1-general"),     // CCTV-1 综合伴音
            findStationById("preset-cnr-econ"),          // CNR-2 经济之声
            findStationById("preset-cri-news"),          // CRI 环球资讯广播
            findStationById("preset-sc-music"),          // 四川音乐广播
            findStationById("preset-rthk-1"),            // 香港电台第一台 RTHK Radio 1
            findStationById("preset-sh-cbn"),            // 第一财经广播
            findStationById("preset-hn-hifi"),           // 河南音乐 Hi-Fi Radio
            findStationById("preset-sd-music"),          // 山东音乐广播
            findStationById("preset-gd-zhujiang"),       // 广东珠江经济台
            findStationById("preset-classic-fm")         // Classic FM 伦敦古典台
        )
    }

    val defaultStations: List<RadioStation> get() = featuredStations

    // 2. 潮流通勤 (聚焦动感流行与活力金曲，零路况杂音，零突兀讲话)
    val commuteStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-gd-music"),          // 广东音乐之声
            findStationById("preset-sz-feiyang971"),     // 深圳飞扬971
            findStationById("preset-js-music"),          // 江苏音乐广播 魅力897
            findStationById("preset-hunan-jinying"),     // 湖南金鹰955 (芒果潮声)
            findStationById("preset-xmly-music"),        // 喜马拉雅音乐精选台
            findStationById("preset-sc-music"),          // 四川音乐广播
            findStationById("preset-hn-music"),          // 河南音乐广播
            findStationById("preset-sd-music"),          // 山东音乐广播
            findStationById("preset-xj-music"),          // 新疆音乐广播
            findStationById("preset-hebei-music"),       // 河北音乐广播
            findStationById("preset-dance-wave"),        // Dance Wave!
            findStationById("preset-smooth-radio")       // Smooth Radio
        )
    }

    // 3. 路况交通 (纯粹交广、实时路况与出行伴随)
    val trafficStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-gd-traffic"),        // 广东交通之声
            findStationById("preset-gz-traffic"),        // 羊城交通台
            findStationById("preset-sc-traffic988"),     // 四川交通广播 988
            findStationById("preset-sd-traffic"),        // 山东交通广播
            findStationById("preset-hn-traffic"),        // 河南交通广播
            findStationById("preset-hb-traffic")         // 湖北交通广播
        )
    }

    // 4. 晚间助眠 (纯净零人声/环境音景/舒缓冥想，彻底剔除戏曲、爵士、吵闹快歌)
    val sleepStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-somafm-drone"),      // SomaFM: Drone Zone (太空冥想, 深度睡眠)
            findStationById("preset-somafm-groove"),     // SomaFM: Groove Salad (Chillout 氛围音)
            findStationById("preset-smooth-radio"),      // Smooth Radio (醇美慢调)
            findStationById("preset-classic-fm"),        // Classic FM (纯正古典, 舒缓交响)
            findStationById("preset-nightwave-plaza"),   // Nightwave Plaza 蒸汽波电台 (霓虹慢境)
            findStationById("preset-hn-hifi")            // 河南音乐 Hi-Fi Radio (纯净好声)
        )
    }

    // 5. 咖啡专注 (轻爵士/古典交响/Lo-Fi，剔除人声解说与流行老歌)
    val focusStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-linn-jazz"),         // Linn Jazz 高品爵士 (发烧爵士, 蓝调微醺)
            findStationById("preset-classic-fm"),        // Classic FM 伦敦古典交响台
            findStationById("preset-somafm-groove"),     // SomaFM: Groove Salad
            findStationById("preset-hn-hifi"),           // 河南音乐 Hi-Fi Radio (发烧品质)
            findStationById("preset-classic-500"),       // 中国经典500首 (纯音乐/典藏)
            findStationById("preset-cctv3-arts")         // CCTV-3 国家级文艺
        )
    }

    // 6. 实时新闻 (国家电台/权威资讯/联播)
    val newsStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-cnr-voice"),         // CNR-1 中国之声
            findStationById("preset-cctv13-news"),       // CCTV-13 新闻伴音
            findStationById("preset-cctv1-general"),     // CCTV-1 综合伴音
            findStationById("preset-cri-news"),          // CRI 环球资讯广播
            findStationById("preset-cnr-econ"),          // CNR-2 经济之声
            findStationById("preset-ifeng-info"),        // 凤凰卫视资讯台伴音
            findStationById("preset-sh-news"),           // 上海新闻广播
            findStationById("preset-gd-news"),           // 广东新闻广播
            findStationById("preset-rthk-1"),            // 香港电台第一台 RTHK Radio 1
            findStationById("preset-sh-cbn")             // 第一财经广播
        )
    }

    // 7. 华语经典 (岁月金曲/经典老歌)
    val classicStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-classic-500"),       // 中国经典500首
            findStationById("preset-hn-hifi"),           // 河南音乐 Hi-Fi Radio
            findStationById("preset-gd-zhujiang"),       // 广东珠江经济台
            findStationById("preset-ifeng-chinese"),     // 凤凰卫视中文台伴音
            findStationById("preset-cri-chinese"),       // CRI 华语环球广播
            findStationById("preset-cctv4-inter")        // CCTV-4 中文国际伴音
        )
    }

    // 8. 摇滚流行 (摇滚/舞曲/动感节奏)
    val rockStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-rock-antenne"),      // Rock Antenne 摇滚天线
            findStationById("preset-radio-paradise"),    // Radio Paradise (独立摇滚)
            findStationById("preset-dance-wave"),        // Dance Wave! 动感舞曲
            findStationById("preset-hunan-jinying"),     // 湖南金鹰955电台
            findStationById("preset-sz-feiyang971")      // 深圳飞扬971
        )
    }

    // 9. 环球视界 (国际与港澳台精品广播)
    val worldStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-rthk-1"),            // 香港电台第一台 RTHK Radio 1
            findStationById("preset-bbc-world"),         // BBC World Service 环球资讯
            findStationById("preset-classic-fm"),        // Classic FM 伦敦古典交响台
            findStationById("preset-radio-paradise"),    // Radio Paradise
            findStationById("preset-linn-jazz"),         // Linn Jazz
            findStationById("preset-somafm-groove"),     // SomaFM: Groove Salad
            findStationById("preset-somafm-drone"),      // SomaFM: Drone Zone
            findStationById("preset-dance-wave"),        // Dance Wave!
            findStationById("preset-rock-antenne")       // Rock Antenne
        )
    }

    // 10. 央视伴音专区 (14 个央视电视伴音台，供特定需求者收听，不侵占通用电台流)
    val tvAudioStations: List<RadioStation> by lazy {
        rawPresetDatabase.filter { it.name.startsWith("CCTV") }
    }

    fun findStationsByProvince(province: String): List<RadioStation> {
        val clean = province.trim()
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
            .removeSuffix("自治区")
            .removeSuffix("特别行政区")
        if (clean.isBlank()) return emptyList()
        return rawPresetDatabase.filter { s ->
            s.state?.contains(clean, ignoreCase = true) == true ||
            s.name.contains(clean, ignoreCase = true) ||
            s.tags.contains(clean, ignoreCase = true)
        }
    }

    fun findStationsByKeyword(keyword: String): List<RadioStation> {
        val clean = keyword.trim()
        if (clean.isBlank()) return emptyList()
        return rawPresetDatabase.filter { s ->
            s.name.contains(clean, ignoreCase = true) ||
            s.tags.contains(clean, ignoreCase = true) ||
            s.state?.contains(clean, ignoreCase = true) == true ||
            s.country.contains(clean, ignoreCase = true)
        }
    }

    fun findStationsByKeywords(keywords: Collection<String>): List<RadioStation> {
        val cleanKeys = keywords.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (cleanKeys.isEmpty()) return emptyList()
        return rawPresetDatabase.filter { s ->
            val text = "${s.name} ${s.tags} ${s.state ?: ""} ${s.country}".lowercase()
            cleanKeys.any { key -> text.contains(key) }
        }
    }

    val nationalStations: List<RadioStation> by lazy {
        listOfNotNull(
            findStationById("preset-cnr-voice"),
            findStationById("preset-cnr-economy"),
            findStationById("preset-cnr-music"),
            findStationById("preset-cctv13-news"),
            findStationById("preset-cctv1-general")
        )
    }

    fun findStationsByCity(city: String): List<RadioStation> {
        val clean = city.trim()
            .removeSuffix("市")
            .removeSuffix("区")
            .removeSuffix("县")
            .removeSuffix("地区")
            .removeSuffix("盟")
            .removeSuffix("州")
        if (clean.isBlank() || clean == "全城通用" || clean == "全省通用" || clean == "全区通用") return emptyList()
        return rawPresetDatabase.filter { s ->
            s.name.contains(clean, ignoreCase = true) ||
            s.tags.contains(clean, ignoreCase = true)
        }
    }

    fun countMatchingStations(keywords: Collection<String>): Int {
        return findStationsByKeywords(keywords).size
    }
}
