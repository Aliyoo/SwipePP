package com.example.data.smarttag

import com.example.data.PhotoItemEntity

/**
 * Fixed smart content taxonomy for the timeline filter. Each category is backed by concrete
 * Chinese sub-prototype embeddings in `clip_categories.bin` (CLC2 format, max-pooled and
 * debiased against the shared neutral-concept baseline vector).
 *
 * `searchAliases` intentionally contains ONLY category-level synonyms (no mutually exclusive
 * concrete hyponyms such as "猫"/"狗" inside PET or "汽车"/"飞机" inside VEHICLE) so searching
 * a specific subject is handled by fine-grained debiased semantic search rather than pulling
 * every sibling in the coarse category.
 *
 * Keys are stored comma-separated in `photos.contentLabels`; `zhLabel` is what the user sees.
 */
enum class SmartCategory(
    val key: String,
    val zhLabel: String,
    val mlKitLabelHints: List<String>,
    val searchAliases: List<String>,
    val exactAliases: List<String> = emptyList()
) {
    PERSON(
        "PERSON", "人物",
        listOf("person", "people", "face", "selfie", "man", "woman", "child", "girl", "boy", "portrait"),
        listOf("人物", "人像", "肖像")
    ),
    FOOD(
        "FOOD", "美食",
        listOf("food", "dish", "meal", "dessert", "cake", "cuisine", "breakfast", "lunch", "dinner", "snack"),
        listOf("美食", "食物", "饭菜", "餐饮", "料理")
    ),
    DRINK(
        "DRINK", "饮品",
        listOf("drink", "coffee", "tea", "cocktail", "beer", "wine", "beverage", "juice", "latte"),
        listOf("饮品", "饮料", "茶饮")
    ),
    PET(
        "PET", "宠物",
        listOf("dog", "cat", "pet", "puppy", "kitten"),
        listOf("宠物", "萌宠")
    ),
    ANIMAL(
        "ANIMAL", "动物",
        listOf("animal", "bird", "wildlife", "horse", "insect", "fish", "deer", "monkey", "rabbit"),
        listOf("动物", "野生动物")
    ),
    NATURE(
        "NATURE", "山野自然",
        listOf("mountain", "forest", "landscape", "nature", "valley", "hill", "tree", "hiking", "meadow"),
        listOf("山野", "自然", "自然风光", "风景", "风光")
    ),
    WATER(
        "WATER", "江河湖海",
        listOf("sea", "ocean", "beach", "coast", "lake", "river", "water", "waterfall", "wave", "island", "sand"),
        listOf("江河湖海", "海景", "湖景", "水景", "水域")
    ),
    SNOW(
        "SNOW", "冰雪",
        listOf("snow", "winter", "ice", "ski", "glacier"),
        listOf("冰雪", "雪景", "冬雪")
    ),
    SUNSET(
        "SUNSET", "日出日落",
        listOf("sunset", "sunrise", "dusk", "dawn", "twilight"),
        listOf("日落", "日出", "黄昏", "晚霞", "夕阳", "朝霞")
    ),
    SKY(
        "SKY", "天空云朵",
        listOf("sky", "cloud", "rainbow", "star", "moon", "aurora"),
        listOf("天空", "云朵", "晴空")
    ),
    CITY(
        "CITY", "城市街巷",
        listOf("city", "building", "skyscraper", "urban", "street", "architecture", "tower", "bridge", "alley"),
        listOf("城市", "街巷", "街景", "建筑", "都市")
    ),
    NIGHT(
        "NIGHT", "夜景",
        listOf("night", "neon", "lantern", "fireworks"),
        listOf("夜景", "夜晚", "夜色")
    ),
    FLOWER(
        "FLOWER", "花草植物",
        listOf("flower", "plant", "blossom", "garden", "leaf", "botanical", "rose", "tulip", "sakura", "floral"),
        listOf("花草", "植物", "花卉", "花朵")
    ),
    DOCUMENT(
        "DOCUMENT", "文档票据",
        listOf("document", "text", "paper", "receipt", "book", "handwriting", "menu", "newspaper", "ticket", "note"),
        listOf("文档", "票据", "单据", "纸质文档")
    ),
    SCREEN(
        "SCREEN", "屏幕",
        listOf("screen", "monitor", "computer", "laptop", "television", "display"),
        listOf("屏幕", "电子屏幕", "截屏界面")
    ),
    INDOOR(
        "INDOOR", "室内居家",
        listOf("room", "interior", "furniture", "chair", "table", "bed", "sofa", "kitchen", "indoor"),
        listOf("室内", "居家", "室内环境")
    ),
    VEHICLE(
        "VEHICLE", "交通工具",
        listOf("car", "vehicle", "bicycle", "train", "airplane", "boat", "ship", "bus", "motorcycle", "aircraft"),
        listOf("交通工具", "载具", "交通")
    ),
    LANDMARK(
        "LANDMARK", "旅行地标",
        listOf("landmark", "tourism", "monument", "temple", "castle", "church", "shrine", "palace", "statue"),
        listOf("地标", "景点", "名胜", "古迹", "旅行地标")
    ),
    EVENT(
        "EVENT", "聚会活动",
        listOf("party", "concert", "festival", "crowd", "celebration", "wedding", "performance", "stage"),
        listOf("聚会", "活动", "聚会活动")
    ),
    SPORT(
        "SPORT", "运动户外",
        listOf("sport", "soccer", "basketball", "stadium", "yoga", "running", "swimming", "ball", "tennis", "cycling"),
        listOf("运动", "户外运动", "健身", "体育")
    ),
    ART(
        "ART", "艺术展品",
        listOf("art", "painting", "sculpture", "museum", "drawing", "gallery", "exhibition", "craft"),
        listOf("艺术", "艺术品", "展览", "展品")
    ),
    PRODUCT(
        "PRODUCT", "静物商品",
        listOf("product", "bottle", "package", "box", "object"),
        listOf("商品", "静物", "静物特写")
    ),
    GROUP(
        "GROUP", "合影",
        listOf("group", "crowd", "team", "family", "audience"),
        listOf("合影", "合照", "多人合影", "全家福")
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
 * Zero-centered lift decision thresholds calibrated on Chinese-CLIP RN50 + CLC2 multi-prototype
 * category table. For each image embedding x, we subtract its baseline dot product against the
 * unnormalized neutral-concept mean vector (`dot(x, neutralMean)`), removing both per-image
 * Hubness and the shared text cone before max-pooling across each category's sub-prototypes.
 */
object SmartTagHeuristics {
    const val CLIP_CATEGORY_MIN_LIFT = 0.030f
    const val CLIP_CATEGORY_TOP1_MARGIN = 0.032f
    const val CLIP_CATEGORY_STRONG_LIFT = 0.052f
    const val MIN_IMAGE_LUMA_STD = 0.03f
    const val MAX_TAGS_PER_PHOTO = 3
    const val CONTENT_LABEL_VERSION = 5
}

fun PhotoItemEntity.smartCategories(): List<SmartCategory> = SmartCategory.keysInCsv(contentLabels)

fun PhotoItemEntity.hasSmartContentFor(zhOrKey: String): Boolean {
    if (contentLabels.isBlank()) return false
    return SmartCategory.keysInCsv(contentLabels).any { it.zhLabel == zhOrKey || it.key == zhOrKey }
}
