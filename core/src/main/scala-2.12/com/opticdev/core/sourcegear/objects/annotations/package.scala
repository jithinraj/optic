package com.opticdev.core.sourcegear.objects

import com.opticdev.common.ObjectRef
import com.opticdev.sdk.descriptions.SchemaRef
import play.api.libs.json.{JsObject, Json}
import com.opticdev.common.Regexes.packages
import com.opticdev.sdk.descriptions.transformation.TransformationRef

import scala.util.Try

package object annotations {

  //Annotation Value Classes
  sealed trait AnnotationValues {val name: String}
  case class StringValue(name: String) extends AnnotationValues
  case class ExpressionValue(name: String, transformationRef: TransformationRef, askJsonRaw: Option[String]) extends AnnotationValues {
    def jsObject = askJsonRaw.map(Json.parse).map(_.as[JsObject])
  }

  //Processed Annotation Classes
  sealed trait ObjectAnnotation
  case class NameAnnotation(name: String, schemaRef: SchemaRef) extends ObjectAnnotation {
    def objectRef = ObjectRef(name)
  }
  case class SourceAnnotation(sourceName: String, transformationRef: TransformationRef, askObject: JsObject = JsObject.empty) extends ObjectAnnotation

  //Regexes
  def topLevelCapture = "^(\\s*([a-z]+)\\s*:\\s*[a-zA-z \\-\\>\\{\\}\\.\\d\\@\\/\\:\\'\\\"]+)(,\\s*([a-z]+)\\s*:\\s*[a-zA-z \\-\\>\\{\\}\\.\\d\\@\\/\\:\\'\\\"]+)*".r
  def propertiesCapture = s"\\s*([a-z]+)\\s*:\\s*([a-zA-z ]+)(?:\\s*->\\s*($packages)\\s*(\\{.*\\}){0,1}){0,1}"
    .r("key", "name", "transformRef", "namespace", "packageName", "version", "id", "askJson")

}