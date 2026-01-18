╔═ generates comments ═╗
package com.example.test

import kotlin.String
import kotlin.jvm.JvmInline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue

/**
 * This is an element with a comment.
 */
@Serializable
@XmlElement(value = true)
@SerialName(value = "comments")
public data class Comments(
  /**
   * This is an attribute with a comment.
   */
  @XmlElement(value = false)
  @SerialName(value = "attr1")
  public val attr1: String,
  @XmlValue
  public val content: ParsedCharacterData,
)

@Serializable
@JvmInline
public value class ParsedCharacterData(
  public val content: String,
)

╔═ generates either elements ═╗
package com.example.test

import kotlin.String
import kotlin.jvm.JvmInline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue

/**
 * Holds either a date or a time, but never both.
 */
@Serializable
@XmlElement(value = true)
@SerialName(value = "date_or_time")
public sealed interface DateOrTime {
  /**
   * An ISO8601 date.
   */
  @Serializable
  @XmlElement(value = true)
  @SerialName(value = "date")
  public data class Date(
    @XmlValue
    public val content: ParsedCharacterData,
  ) : DateOrTime

  /**
   * An ISO8601 time.
   */
  @Serializable
  @XmlElement(value = true)
  @SerialName(value = "time")
  public data class Time(
    @XmlValue
    public val content: ParsedCharacterData,
  ) : DateOrTime
}

@Serializable
@JvmInline
public value class ParsedCharacterData(
  public val content: String,
)

╔═ generates nested elements ═╗
package com.example.test

import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmInline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement

@Serializable
@XmlElement(value = true)
@SerialName(value = "ARTICLE")
public data class Article(
  @XmlElement(value = false)
  @SerialName(value = "AUTHOR")
  public val author: String,
  @XmlElement(value = false)
  @SerialName(value = "EDITOR")
  public val editor: String?,
  @XmlElement(value = false)
  @SerialName(value = "DATE")
  public val date: String?,
  @XmlElement(value = false)
  @SerialName(value = "EDITION")
  public val edition: String?,
  @XmlElement(value = true)
  @SerialName(value = "HEADLINE")
  public val headline: ParsedCharacterData,
  @XmlElement(value = true)
  @SerialName(value = "BYLINE")
  public val byline: ParsedCharacterData,
  @XmlElement(value = true)
  @SerialName(value = "LEAD")
  public val lead: ParsedCharacterData,
  @XmlElement(value = true)
  @SerialName(value = "BODY")
  public val body: ParsedCharacterData,
  @XmlElement(value = true)
  @SerialName(value = "NOTES")
  public val notes: ParsedCharacterData,
)

@Serializable
@XmlElement(value = true)
@SerialName(value = "NESTED")
public data class Nested(
  @XmlElement(value = true)
  @SerialName(value = "ARTICLE")
  public val articles: List<Article>,
)

@Serializable
@JvmInline
public value class ParsedCharacterData(
  public val content: String,
)

╔═ generates pcdata parser ═╗
package com.example.test

import kotlin.String
import kotlin.collections.Map
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class ParsedCharacterData(
  internal val rawValue: String,
) {
  public fun parse(entities: Map<String, String> = InternalEntities): String {
    val regex = Regex("&([a-zA-Z0-9]+);")
    return regex.replace(rawValue) { matchResult ->
      val key = matchResult.groupValues[1]
      entities[key] ?: key
    }
  }

  public companion object {
    public val InternalEntities: Map<String, String> = mapOf(
      "NEWSPAPER" to "Vervet Logic Times",
      "PUBLISHER" to "Vervet Logic Press",
      "COPYRIGHT" to "Copyright 1998 Vervet Logic Press",
    )
  }
}

╔═ generates prefixes ═╗
package com.example.test

import kotlin.String
import kotlin.jvm.JvmInline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlElement(value = true)
@SerialName(value = "prefixes")
public data class Prefixes(
  @XmlElement(value = false)
  @XmlSerialName(
    prefix = "xml",
    value = "name",
  )
  @SerialName(value = "name")
  public val name: String,
  @XmlValue
  public val content: ParsedCharacterData,
)

@Serializable
@JvmInline
public value class ParsedCharacterData(
  public val content: String,
)

╔═ generates with no nested elements ═╗
package com.example.test

import kotlin.String
import kotlin.jvm.JvmInline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlElement(value = true)
@SerialName(value = "no-nested-data")
public data class NoNestedData(
  @XmlElement(value = false)
  @SerialName(value = "attr1")
  public val attr1: String,
  @XmlValue
  public val content: ParsedCharacterData,
)

@Serializable
@JvmInline
public value class ParsedCharacterData(
  public val content: String,
)

╔═ [end of file] ═╗
