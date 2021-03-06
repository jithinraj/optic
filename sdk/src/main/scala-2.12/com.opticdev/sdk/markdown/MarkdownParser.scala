package com.opticdev.sdk.markdown

import better.files.File
import com.opticdev.parsers.utils.Crypto
import com.opticdev.sdk.opticmarkdown2.schema.OMSchema
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.sys.process._
import scala.util.Try

object MarkdownParser {

  private val outputSchema = OMSchema(null, Json.parse(
    """
      |{ "type": "object", "title": "MD Parse Output", "properties": {
      |   "description": { "type": "object" },
      |   "errors": { "type": "array" }
      |} }
    """.stripMargin).as[JsObject])

  case class MDParseOutput(jsObject: JsObject) {
    def description = (jsObject \ "description").getOrElse(JsObject.empty).as[JsObject]

    def lenses = (jsObject \ "description" \ "lenses").getOrElse(JsArray.empty).as[JsArray]
    def schemas = (jsObject \ "description" \ "schemas").getOrElse(JsArray.empty).as[JsArray]
    def dependencies = (jsObject \ "description" \ "dependencies").getOrElse(JsArray.empty).as[JsArray]


    def errors = (jsObject \ "errors").getOrElse(JsArray.empty).as[JsArray]
    def noErrors = errors.value.isEmpty
  }


  def parseMarkdownString(string: String, useCache: Boolean = true) : Try[MDParseOutput] = Try {

    val cacheLookup = {
      if (useCache) MarkdownCache.lookup(Crypto.createSha1(string)) else None
    }
    if (cacheLookup.isDefined) {
      cacheLookup.get
    } else {

      val result = CallOpticMarkdown.parseString(string)
      if (!outputSchema.validate(result)) {
        throw new Error("Invalid output from markdown parser")
      } else {
        if (useCache) {
          MarkdownCache.cacheMarkdown(string, result)
        }
        MDParseOutput(result)
      }

    }
  }

  def parseMarkdownFile(file: File, useCache: Boolean = true) : Try[MDParseOutput] = Try {
    val cacheLookup = {
      if (useCache) MarkdownCache.lookup(file) else None
    }
    if (cacheLookup.isDefined) {
      cacheLookup.get
    } else {

      val result = CallOpticMarkdown.parseFile(file.pathAsString)
      if (!outputSchema.validate(result)) {
        throw new Error("Invalid output from markdown parser")
      } else {
        if (useCache) {
          MarkdownCache.cacheMarkdown(file, result)
        }
        MDParseOutput(result)
      }

    }
  }

}
