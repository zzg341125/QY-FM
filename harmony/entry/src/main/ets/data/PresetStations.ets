import { Station } from '../models/Station';

export const PRESET_STATIONS: Station[] = [
  {
    id: 'preset-cnr-voice',
    name: 'CNR-1 中国之声',
    streamUrl: 'http://ngcdn001.cnr.cn/live/zgzs/index.m3u8',
    iconUrl: 'https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=300',
    tags: '国家电台, 实时新闻, 权威声音',
    country: '中国',
    state: '北京',
    favorite: true
  },
  {
    id: 'preset-gd-music',
    name: '广东音乐之声',
    streamUrl: 'https://lhttp.qtfm.cn/live/1260/64k.mp3',
    iconUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300',
    tags: '流行前线, 粤语流行, 音乐之声',
    country: '中国',
    state: '广东',
    favorite: true
  },
  {
    id: 'preset-cctv13-news',
    name: 'CCTV-13 新闻伴音',
    streamUrl: 'https://piccpndali.v.myalicdn.com/audio/cctv13_2.m3u8',
    iconUrl: 'https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300',
    tags: '央视新闻, 实时伴音, 热点聚焦',
    country: '中国',
    state: '北京',
    favorite: true
  },
  {
    id: 'preset-sz-feiyang971',
    name: '深圳飞扬971 音乐调频',
    streamUrl: 'http://lhttp.qingting.fm/live/5022308/64k.mp3',
    iconUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300',
    tags: '深圳飞扬, 青春流行, 鹏城之声',
    country: '中国',
    state: '广东',
    favorite: true
  },
  {
    id: 'preset-js-music',
    name: '江苏音乐广播 魅力897',
    streamUrl: 'http://lzlive.vojs.cn/5Fo8mMX/92/live.m3u8?',
    iconUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300',
    tags: '江苏广电官方, 魅力897, 流行经典',
    country: '中国',
    state: '江苏',
    favorite: true
  },
  {
    id: 'preset-classic-500',
    name: '中国经典500首',
    streamUrl: 'http://ls.qingting.fm/live/3412131.m3u8?bitrate=64',
    iconUrl: 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=300',
    tags: '岁月如歌, 华语传世, 典藏金曲',
    country: '中国',
    state: '上海',
    favorite: true
  },
  {
    id: 'preset-xmly-music',
    name: '喜马拉雅音乐精选台',
    streamUrl: 'https://live.ximalaya.com/radio-first-page-app/live/2629/64.m3u8',
    iconUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300',
    tags: '品质音乐, 热门歌单, 随行相伴',
    country: '中国',
    state: '北京',
    favorite: true
  },
  {
    id: 'preset-cnr-econ',
    name: 'CNR-2 经济之声',
    streamUrl: 'http://ngcdn002.cnr.cn/live/jjzs/index.m3u8',
    iconUrl: 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300',
    tags: '财经深度, 宏观视野, 商业动态',
    country: '中国',
    state: '北京',
    favorite: true
  },
  {
    id: 'preset-rthk-1',
    name: '香港电台第一台 RTHK Radio 1',
    streamUrl: 'http://stm.rthk.hk/radio1',
    iconUrl: 'https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=300',
    tags: '香港第一台, 港闻评论, 经典广播',
    country: '中国',
    state: '香港',
    favorite: true
  },
  {
    id: 'preset-somafm-groove',
    name: 'SomaFM: Groove Salad',
    streamUrl: 'https://ice6.somafm.com/groovesalad-128-mp3',
    iconUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300',
    tags: 'Chillout, 电子慢摇, 氛围环境音',
    country: 'Global',
    state: 'San Francisco',
    favorite: true
  }
];
