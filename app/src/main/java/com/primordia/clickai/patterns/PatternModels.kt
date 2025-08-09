package com.primordia.clickai.patterns

data class Pattern(val name: String, val elements: List<Element>, val relations: List<Relation>)

sealed interface Element { val id: String }

data class ImageElement(override val id: String, val assetPath: String, val threshold: Float = 0.85f) : Element

data class TextElement(override val id: String, val query: String, val isRegex: Boolean = false) : Element

data class ColorElement(override val id: String, val color: Int, val tolerance: Int, val region: Region) : Element

data class Region(val left: Int, val top: Int, val right: Int, val bottom: Int)

enum class RelationType { ABOVE, BELOW, LEFT_OF, RIGHT_OF }

data class Relation(val fromId: String, val toId: String, val type: RelationType, val minPx: Int = 0, val maxPx: Int = Int.MAX_VALUE)