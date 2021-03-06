package com.opticdev.core.compiler.errors

import com.opticdev.common.SchemaRef
import com.opticdev.core.compiler.helpers.FinderPath
import com.opticdev.opm.packages.OpticMDPackage
import com.opticdev.sdk.opticmarkdown2.lens.{OMLens, OMLensCodeComponent, OMLensComponent, OMLensNodeFinder}

import scala.util.control.NonFatal

trait CompilerException extends Exception {
  val lens: OMLens
}

//Validation Stage
case class SchemaNotFound(schemaId: SchemaRef)(implicit val lens: OMLens) extends CompilerException {
  override def toString = "The schema "+schemaId.id+" was not found in description"
}

//Snippet Exception
case class ParserNotFound(lang: String)(implicit val lens: OMLens) extends CompilerException {
  override def toString = "Parser Not Found for "+lens.name+". Please install "+lang
}

case class DuplicateContainerNamesInSnippet(duplicateNames: Vector[String])(implicit val lens: OMLens) extends CompilerException {
  override def toString = s"Duplicate container names [${duplicateNames.mkString(", ")}] defined in snippet."
}

case class ContainerDefinitionConflict()(implicit val lens: OMLens) extends CompilerException {
  override def toString = s"More than one container is defined for the same AST node."
}

case class ContainerHookIsNotInAValidAstNode(containerName: String, validNodes: Seq[String])(implicit val lens: OMLens) extends CompilerException {
  override def toString = s"Container Hook $containerName is not in a valid node for this language: [${validNodes.mkString(", ")}]"
}


case class SyntaxError(error: Throwable)(implicit val lens: OMLens) extends CompilerException {
  override def toString = s"Syntax error in Snippet: ${lens.snippet.block} \n\n ${error.toString}"
}

case class UnexpectedSnippetFormat(description: String)(implicit val lens: OMLens) extends CompilerException {
  override def toString = "Unexpected Snippet Format: "+description
}

//Finder CompilerError

case class NodeNotFound(nodeFinder: OMLensNodeFinder)(implicit val lens: OMLens) extends CompilerException {
  override def toString = "A node of type " + nodeFinder.astType +" not found at range "+ nodeFinder.range
}

//Finder Stage CompilerError
case class InvalidComponents(invalidComponents: Seq[OMLensComponent])(implicit val lens: OMLens) extends CompilerException {
  override def toString = invalidComponents.size+" code components were not found in Snippet."
}

//Walkable Paths Error
case class AstPathNotFound(finderPath: FinderPath)(implicit val lens: OMLens) extends CompilerException {
  override def toString = "AstPathNotFound to target node. Internal Error. "+finderPath
}

case class SomePackagesFailedToCompile(errors: Map[OpticMDPackage, Map[OMLens, ErrorAccumulator]]) extends Exception