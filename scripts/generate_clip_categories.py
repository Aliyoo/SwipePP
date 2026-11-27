#!/usr/bin/env python3
"""
Generates app/src/main/assets/clip_categories.bin in CLC2 multi-prototype format using the
shipped Chinese-CLIP int8 text encoder (`cnclip_text_int8.onnx`) and `cnclip_vocab.txt`.

Binary layout (little-endian):
  - magic[4]          : ASCII "CLC2"
  - dim               : int32 (1024)
  - categoryCount     : int32 (23)
  - neutralMeanVec    : float16[dim] (UNNORMALIZED mean of 60 diverse Chinese concepts;
                        dotting any unit image embedding with this vector yields the image's
                        expected cosine similarity against an average generic concept, used
                        for zero-centered Hubness debiasing in both tagging and search)
  - repeated categoryCount times:
      - keyLen        : int32
      - keyUtf8       : byte[keyLen]
      - protoCount    : int32 (number of concrete sub-prototype vectors for this category)
      - protos        : protoCount * float16[dim] (each unit L2-normalized)
"""

import struct
import unicodedata
from pathlib import Path

import numpy as np
import onnxruntime as ort

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS_DIR = REPO_ROOT / "app" / "src" / "main" / "assets"
VOCAB_PATH = ASSETS_DIR / "cnclip_vocab.txt"
TEXT_MODEL_PATH = ASSETS_DIR / "cnclip_text_int8.onnx"
OUT_PATH = ASSETS_DIR / "clip_categories.bin"

CONTEXT_LENGTH = 52
EMBEDDING_DIM = 1024

# 60 diverse everyday Chinese visual concepts spanning people, food, animals, landscapes,
# architecture, documents, screens, indoor scenes, vehicles, events, sports, art, weather.
# Their UNNORMALIZED mean vector captures the shared Chinese-CLIP text cone so that
# `dot(imgVec, neutralMean)` equals `E_w[dot(imgVec, textVec(w))]` for each image.
NEUTRAL_CONCEPTS = [
    "人物", "肖像", "合影", "美食", "饭菜", "甜品", "咖啡", "饮品", "猫咪", "小狗",
    "鸟类", "动物", "山脉", "森林", "草原", "大海", "湖泊", "沙滩", "雪山", "冰雪",
    "日落", "晚霞", "蓝天", "白云", "星空", "城市", "街道", "建筑", "夜景", "灯光",
    "鲜花", "植物", "树叶", "文档", "笔记", "发票", "截图", "电脑", "室内", "房间",
    "汽车", "飞机", "火车", "轮船", "古塔", "寺庙", "景点", "聚会", "婚礼", "演唱会",
    "运动", "球场", "绘画", "雕塑", "相机", "商品", "雨天", "晴天", "红叶", "灯笼",
]

# Concrete visual sub-prototypes per SmartCategory (max-pooled at inference time instead of
# averaging distinct visual concepts into a blurred centroid).
CATEGORY_SUBPROMPTS = [
    ("PERSON", ["人脸正面肖像特写", "正在微笑的年轻人自拍", "站在镜头前的男子或女子"]),
    ("GROUP", ["多人站在一起的合影合照", "一家人或朋友们的全家福", "一群人肩并肩微笑合影"]),
    ("FOOD", ["餐桌上的一盘美味佳肴", "碗里的热气腾腾饭菜面条", "精致的蛋糕面包甜品点心"]),
    ("DRINK", ["桌上一杯咖啡拿铁饮品", "一杯茶饮奶茶或果汁", "酒杯里的红酒啤酒鸡尾酒"]),
    ("PET", ["一只可爱的宠物猫咪", "一只可爱的宠物小狗", "家里养的小猫小狗萌宠"]),
    ("ANIMAL", ["自然界里的野生动物或飞鸟", "动物园里的动物特写", "水里的鱼或蝴蝶昆虫"]),
    ("NATURE", ["壮丽的山脉森林自然风光", "高山草原峡谷风景", "郁郁葱葱的山野树林"]),
    ("WATER", ["蔚蓝大海海岸与海边小镇", "平静的湖泊江河水面倒影", "海浪沙滩海湾瀑布"]),
    ("SNOW", ["白雪皑皑的雪山冰川", "冬季下雪后的积雪风景", "雪地滑雪冰雪世界"]),
    ("SUNSET", ["金色日落晚霞黄昏天空", "清晨日出朝霞火烧云", "落日余晖照耀天际"]),
    ("SKY", ["晴朗的蓝天与朵朵白云", "夜晚璀璨星空银河月亮", "雨后彩虹高空云海"]),
    ("CITY", ["繁华都市的高楼大厦街道", "传统老街小巷建筑与桥梁", "城市街景民居楼房"]),
    ("NIGHT", ["夜晚灯火通明的城市夜景", "夜间点亮的灯笼霓虹灯光", "夜空烟花与夜晚灯火"]),
    ("FLOWER", ["盛开的鲜花花朵特写", "五彩缤纷的花园花丛", "满树红叶绿叶花草植物"]),
    ("DOCUMENT", ["手写笔记或翻开的书页文字", "发票收据门票单据", "打印的文档表格合同纸张"]),
    ("SCREEN", ["手机应用软件界面截图", "电脑显示器或平板屏幕画面", "微信聊天窗口截屏"]),
    ("INDOOR", ["温馨室内房间家具摆设", "咖啡馆或餐厅室内桌椅", "客厅卧室厨房室内空间"]),
    ("VEHICLE", ["马路上行驶的汽车轿车", "天空飞行的飞机客机", "火车地铁轮船自行车"]),
    ("LANDMARK", ["著名古塔寺庙历史古迹", "标志性城堡宫殿名胜", "著名地标纪念碑建筑"]),
    ("EVENT", ["热闹的聚会派对庆祝现场", "舞台演唱会音乐节演出", "婚礼庆典节日人群"]),
    ("SPORT", ["体育场球类运动比赛", "户外跑步骑行徒步健身", "游泳瑜伽滑雪运动"]),
    ("ART", ["美术馆博物馆画作雕塑", "艺术展览手工艺品", "油画水彩画艺术作品"]),
    ("PRODUCT", ["桌面摆放的相机电子产品", "商品包装盒瓶子静物", "开箱物品桌面静物特写"]),
]


def load_vocab(path: Path) -> dict[str, int]:
    vocab: dict[str, int] = {}
    with path.open(encoding="utf-8") as f:
        for idx, line in enumerate(f):
            vocab[line.rstrip("\r\n")] = idx
    return vocab


def is_chinese_char(cp: int) -> bool:
    return (
        (0x4E00 <= cp <= 0x9FFF)
        or (0x3400 <= cp <= 0x4DBF)
        or (0x20000 <= cp <= 0x2A6DF)
        or (0x2A700 <= cp <= 0x2B73F)
        or (0x2B740 <= cp <= 0x2B81F)
        or (0x2B820 <= cp <= 0x2CEAF)
        or (0xF900 <= cp <= 0xFAFF)
        or (0x2F800 <= cp <= 0x2FA1F)
    )


def is_punctuation(ch: str) -> bool:
    cp = ord(ch)
    if (33 <= cp <= 47) or (58 <= cp <= 64) or (91 <= cp <= 96) or (123 <= cp <= 126):
        return True
    return unicodedata.category(ch).startswith("P")


def tokenize(text: str, vocab: dict[str, int]) -> np.ndarray:
    cleaned = []
    for ch in text:
        cp = ord(ch)
        if cp == 0 or cp == 0xFFFD:
            continue
        cat = unicodedata.category(ch)
        if ch not in "\t\n\r" and cat in ("Cc", "Cf"):
            continue
        cleaned.append(" " if (ch in " \t\n\r" or cat == "Zs") else ch)
    spaced = "".join(f" {c} " if is_chinese_char(ord(c)) else c for c in "".join(cleaned))
    basic_tokens: list[str] = []
    for tok in spaced.strip().split():
        tok = "".join(
            c for c in unicodedata.normalize("NFD", tok.lower()) if unicodedata.category(c) != "Mn"
        )
        cur: list[str] = []
        for c in tok:
            if is_punctuation(c):
                if cur:
                    basic_tokens.append("".join(cur))
                    cur = []
                basic_tokens.append(c)
            else:
                cur.append(c)
        if cur:
            basic_tokens.append("".join(cur))

    pieces: list[str] = []
    for tok in basic_tokens:
        if len(tok) > 200:
            pieces.append("[UNK]")
            continue
        start = 0
        sub: list[str] = []
        ok = True
        while start < len(tok):
            end = len(tok)
            found = None
            while start < end:
                cand = ("##" + tok[start:end]) if start > 0 else tok[start:end]
                if cand in vocab:
                    found = cand
                    break
                end -= 1
            if found is None:
                ok = False
                break
            sub.append(found)
            start = end
        pieces.extend(sub if ok else ["[UNK]"])

    pieces = pieces[: CONTEXT_LENGTH - 2]
    ids = np.zeros((1, CONTEXT_LENGTH), dtype=np.int64)
    ids[0, 0] = vocab["[CLS]"]
    unk_id = vocab["[UNK]"]
    for i, piece in enumerate(pieces):
        ids[0, i + 1] = vocab.get(piece, unk_id)
    ids[0, len(pieces) + 1] = vocab["[SEP]"]
    return ids


def encode_unit(sess: ort.InferenceSession, vocab: dict[str, int], text: str) -> np.ndarray:
    ids = tokenize(text, vocab)
    out = sess.run(None, {"text": ids})[0][0].astype(np.float32)
    norm = float(np.linalg.norm(out))
    if norm > 0.0:
        out /= norm
    return out


def main() -> None:
    vocab = load_vocab(VOCAB_PATH)
    sess = ort.InferenceSession(str(TEXT_MODEL_PATH), providers=["CPUExecutionProvider"])

    neutral_vecs = np.stack([encode_unit(sess, vocab, w) for w in NEUTRAL_CONCEPTS])
    # Deliberately UNNORMALIZED mean so dot(img, neutral_mean) == E_w[dot(img, w)].
    neutral_mean = np.mean(neutral_vecs, axis=0).astype(np.float32)

    buf = bytearray()
    buf.extend(b"CLC2")
    buf.extend(struct.pack("<ii", EMBEDDING_DIM, len(CATEGORY_SUBPROMPTS)))
    buf.extend(neutral_mean.astype("<f2").tobytes())

    total_protos = 0
    for key, prompts in CATEGORY_SUBPROMPTS:
        key_bytes = key.encode("utf-8")
        buf.extend(struct.pack("<i", len(key_bytes)))
        buf.extend(key_bytes)
        buf.extend(struct.pack("<i", len(prompts)))
        for p in prompts:
            vec = encode_unit(sess, vocab, p)
            buf.extend(vec.astype("<f2").tobytes())
            total_protos += 1

    OUT_PATH.write_bytes(buf)
    print(
        f"Wrote {OUT_PATH} ({len(buf):,} bytes): "
        f"{len(CATEGORY_SUBPROMPTS)} categories, {total_protos} sub-prototypes, "
        f"neutral_mean_norm={float(np.linalg.norm(neutral_mean)):.4f}"
    )


if __name__ == "__main__":
    main()
