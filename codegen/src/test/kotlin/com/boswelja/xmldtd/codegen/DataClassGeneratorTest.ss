╔═ generates comments ═╗
package com.example.test

import kotlin.String
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
  public val content: String,
)

╔═ generates either elements ═╗
package com.example.test

import kotlin.String
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
    public val content: String,
  ) : DateOrTime

  /**
   * An ISO8601 time.
   */
  @Serializable
  @XmlElement(value = true)
  @SerialName(value = "time")
  public data class Time(
    @XmlValue
    public val content: String,
  ) : DateOrTime
}

╔═ generates nested elements ═╗
package com.example.test

import kotlin.String
import kotlin.collections.List
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
  public val headline: String,
  @XmlElement(value = true)
  @SerialName(value = "BYLINE")
  public val byline: String,
  @XmlElement(value = true)
  @SerialName(value = "LEAD")
  public val lead: String,
  @XmlElement(value = true)
  @SerialName(value = "BODY")
  public val body: String,
  @XmlElement(value = true)
  @SerialName(value = "NOTES")
  public val notes: String,
)

@Serializable
@XmlElement(value = true)
@SerialName(value = "NESTED")
public data class Nested(
  @XmlElement(value = true)
  @SerialName(value = "ARTICLE")
  public val articles: List<Article>,
)

╔═ generates pcdata parser ═╗
package com.example.test

import kotlin.String
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlElement(value = true)
@SerialName(value = "parseable_data")
public data class ParseableData(
  @XmlValue
  public val content: String,
)

╔═ generates prefixes ═╗
package com.example.test

import kotlin.String
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
  public val content: String,
)

╔═ generates with no nested elements ═╗
package com.example.test

import kotlin.String
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
  public val content: String,
)

╔═ [end of file] ═╗
