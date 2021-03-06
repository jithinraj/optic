package com.opticdev.core.sourcegear

import com.opticdev.core.sourcegear.graph.model.AstMapping
import com.opticdev.marvin.common.ast.NewAstNode
import com.opticdev.parsers.AstGraph
import com.opticdev.parsers.graph.CommonAstNode
import com.opticdev.sdk.opticmarkdown2.lens.{OMComponentWithPropertyPath, OMLensCodeComponent, OMLensComponent, OMLensSchemaComponent}
import gnieh.diffson.playJson.Operation
import play.api.libs.json.JsValue

import scala.util.Try

package object mutate {
  trait Mutation

  case class AddItemToContainer(component: OMLensSchemaComponent, containerNode: CommonAstNode, newAstNode: NewAstNode)
  case class UpdatedField(component: OMComponentWithPropertyPath[OMLensComponent], mapping: AstMapping, newValue: JsValue)
  case class AstChange(mapping: AstMapping, replacementString: Try[String])

}
