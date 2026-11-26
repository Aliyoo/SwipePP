package com.example.data.smarttag

import com.example.data.PhotoItemEntity

/**
 * Fixed smart content taxonomy for the timeline filter. Each category is produced either by
 * zero-shot CLIP similarity against an English prompt (embeddings precomputed offline into
 * `clip_categories.bin`) or by mapping ML Kit's 400+ English labels, then merged.
 *
 * Keys are stored comma-separated in `photos.contentLabels`; `zhLabel` is what the user sees.
 */
enum class SmartCategory(
    val key: String,
    val zhLabel: String,
    val mlKitLabelHints: List<String>,
    val searchAliases: List<String>
) {
    PERSON(
        "PERSON", "人物",
        listOf("person", "people", "face", "selfie", "man", "woman", "child", "girl", "boy", "portrait"),
        listOf("人", "人物", "人像", "自拍", "合影", "脸")
    ),
    FOOD(
        "FOOD", "美食",
        listOf("food", "dish", "meal", "dessert", "cake", "cuisine", "breakfast", "lunch", "dinner", "snack"),
        listOf("吃", "美食", "食物", "餐", "菜", "甜品", "蛋糕")
    ),
    DRINK(
        "DRINK", "饮品",
        listOf("drink", "coffee", "tea", "cocktail", "beer", "wine", "beverage", "juice", "latte"),
        listOf("喝", "咖啡", "茶", "酒", "饮品", "奶茶")
    ),
    PET(
        "PET", "宠物",
        listOf("dog", "cat", "pet", "puppy", "kitten"),
        listOf("宠物", "猫", "狗", "喵", "汪", "猫咪", "狗狗", "猫猫")
    ),
    ANIMAL(
        "ANIMAL", "动物",
        listOf("animal", "bird", "wildlife", "horse", "insect", "fish", "deer", "monkey", "rabbit"),
        listOf("动物", "鸟", "鱼", "野生")
    ),
    NATURE(
        "NATURE", "山野自然",
        listOf("mountain", "forest", "landscape", "nature", "valley", "hill", "tree", "hiking", "meadow"),
        listOf("山", "森林", "自然", "风景", "徒步", "草原")
    ),
    WATER(
        "WATER", "江河湖海",
        listOf("sea", "ocean", "beach", "coast", "lake", "river", "water", "waterfall", "wave", "island", "sand"),
        listOf("海", "湖", "河", "沙滩", "海滩", "水", "瀑布")
    ),
    SNOW(
        "SNOW", "冰雪",
        listOf("snow", "winter", "ice", "ski", "glacier"),
        listOf("雪", "冰", "冬", "滑雪")
    ),
    SUNSET(
        "SUNSET", "日出日落",
        listOf("sunset", "sunrise", "dusk", "dawn", "twilight"),
        listOf("日落", "日出", "黄昏", "晚霞", "夕阳", "朝阳")
    ),
    SKY(
        "SKY", "天空云朵",
        listOf("sky", "cloud", "rainbow", "star", "moon", "aurora"),
        listOf("天空", "云", "星空", "月亮", "彩虹")
    ),
    CITY(
        "CITY", "城市街巷",
        listOf("city", "building", "skyscraper", "urban", "street", "architecture", "tower", "bridge", "alley"),
        listOf("城市", "建筑", "街", "楼", "都市", "巷")
    ),
    NIGHT(
        "NIGHT", "夜景",
        listOf("night", "neon", "lantern", "fireworks"),
        listOf("夜", "夜景", "夜晚", "霓虹", "烟花", "灯火")
    ),
    FLOWER(
        "FLOWER", "花草植物",
        listOf("flower", "plant", "blossom", "garden", "leaf", "botanical", "rose", "tulip", "sakura", "floral"),
        listOf("花", "植物", "草", "花园", "叶", "樱花")
    ),
    DOCUMENT(
        "DOCUMENT", "文档票据",
        listOf("document", "text", "paper", "receipt", "book", "handwriting", "menu", "newspaper", "ticket", "note"),
        listOf("文档", "票据", "发票", "证件", "纸", "合同", "书")
    ),
    SCREEN(
        "SCREEN", "屏幕",
        listOf("screen", "monitor", "computer", "laptop", "television", "display"),
        listOf("屏幕", "截图", "电脑", "显示器")
    ),
    INDOOR(
        "INDOOR", "室内居家",
        listOf("room", "interior", "furniture", "chair", "table", "bed", "sofa", "kitchen", "indoor"),
        listOf("室内", "家", "房间", "家具", "居家")
    ),
    VEHICLE(
        "VEHICLE", "交通工具",
        listOf("car", "vehicle", "bicycle", "train", "airplane", "boat", "ship", "bus", "motorcycle", "aircraft"),
        listOf("车", "汽车", "飞机", "火车", "船", "交通")
    ),
    LANDMARK(
        "LANDMARK", "旅行地标",
        listOf("landmark", "tourism", "monument", "temple", "castle", "church", "shrine", "palace", "statue"),
        listOf("地标", "景点", "旅行", "旅游", "寺", "塔", "古迹")
    ),
    EVENT(
        "EVENT", "聚会活动",
        listOf("party", "concert", "festival", "crowd", "celebration", "wedding", "performance", "stage"),
        listOf("聚会", "活动", "派对", "演唱会", "节", "婚礼", "演出")
    ),
    SPORT(
        "SPORT", "运动户外",
        listOf("sport", "soccer", "basketball", "stadium", "yoga", "running", "swimming", "ball", "tennis", "cycling"),
        listOf("运动", "球", "健身", "跑步", "游泳", "体育")
    ),
    ART(
        "ART", "艺术展品",
        listOf("art", "painting", "sculpture", "museum", "drawing", "gallery", "exhibition", "craft"),
        listOf("艺术", "画", "展览", "博物馆", "展品")
    ),
    PRODUCT(
        "PRODUCT", "静物商品",
        listOf("product", "bottle", "package", "box", "object"),
        listOf("商品", "静物", "物品", "开箱")
    ),
    GROUP(
        "GROUP", "合影",
        listOf("group", "crowd", "team", "family", "audience"),
        listOf("合影", "合照", "多人", "团体", "全家福")
    );

    fun matchesMlKitLabel(label: String): Boolean {
        val l = label.lowercase()
        return mlKitLabelHints.any { hint -> l == hint || (l.length <= 24 && l.split(' ', '_', '-').any { it == hint }) }
    }

    companion object {
        private val byKey = entries.associateBy { it.key }

        fun fromKey(key: String): SmartCategory? = byKey[key]

        fun keysInCsv(csv: String): List<SmartCategory> =
            csv.split(',').mapNotNull { byKey[it.trim()] }

        fun zhLabelPresentInCsv(csv: String, zhLabel: String): Boolean =
            keysInCsv(csv).any { it.zhLabel == zhLabel }
    }
}

/**
 * Zero-shot tag decision. Measured on reference photos with the shipped fp16 model:
 * correct matches score 0.16-0.26 (a coastal town can be as low as 0.168) while unrelated
 * categories cluster below ~0.15. The strongest match is trusted once it clears the floor;
 * additional near-ties within a small margin join as secondary tags.
 */
object SmartTagHeuristics {
    const val CLIP_MIN_SIMILARITY = 0.16f
    const val CLIP_TIE_MARGIN = 0.02f
    const val MAX_TAGS_PER_PHOTO = 4
    const val CONTENT_LABEL_VERSION = 3
}

fun PhotoItemEntity.smartCategories(): List<SmartCategory> = SmartCategory.keysInCsv(contentLabels)

fun PhotoItemEntity.hasSmartContentFor(zhOrKey: String): Boolean {
    if (contentLabels.isBlank()) return false
    return SmartCategory.keysInCsv(contentLabels).any { it.zhLabel == zhOrKey || it.key == zhOrKey }
}
