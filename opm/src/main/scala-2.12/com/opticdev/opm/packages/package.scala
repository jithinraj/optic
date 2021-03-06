package com.opticdev.opm.packages

import better.files.File
import com.opticdev.common.PackageRef
import com.opticdev.sdk.markdown.MarkdownParser
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

import scala.util.{Success, Try}

trait OpticPackage {
  val description: JsObject

  val name: String = (description \ "info" \ "package").get.as[JsString].value
  val author: String = (description \ "info" \ "author").get.as[JsString].value
  val version: String = (description \ "info" \ "version").get.as[JsString].value

  def packageId = author+":"+name
  def packageFull = packageId+"@"+version
  def packageRef: PackageRef = PackageRef(packageId, version)

  lazy val dependencies: Vector[PackageRef] = {
    val asJsObject: JsObject = (description \ "info" \ "dependencies" ).getOrElse(JsObject.empty).as[JsObject]
    asJsObject.value
      .collect { case (k:String, v:JsString) => PackageRef.fromString(s"$k@${v.value}")}
      .collect { case Success(i) => i }
      .toVector
  }

  def resolved(map: Map[PackageRef, PackageRef] = Map()) = OpticMDPackage(description, map)

}

object OpticPackage {

  def fromJson(value: JsValue) : Try[StagedPackage] = Try {
    val jsObject = value.as[JsObject]
    StagedPackage(jsObject)
  }

  def fromMarkdown(file: File) : Try[StagedPackage] = Try {
    //@todo cleanup errors
    val parsedOption = MarkdownParser.parseMarkdownFile(file)
    if (parsedOption.isSuccess /* && parsedOption.get.noErrors */) {
      StagedPackage(parsedOption.get.description)
    } else {
      throw new Error("Invalid Optic Markdown" + parsedOption.failed.get.toString)
    }
  }

  def fromString(contents: String) : Try[StagedPackage] = Try {
    //@todo cleanup errors
    val parsedOption = MarkdownParser.parseMarkdownString(contents)
    if (parsedOption.isSuccess /* && parsedOption.get.noErrors */) {
      StagedPackage(parsedOption.get.description)
    } else {
      throw new Error("Invalid Optic Markdown" + parsedOption.failed.get.toString)
    }
  }


}
