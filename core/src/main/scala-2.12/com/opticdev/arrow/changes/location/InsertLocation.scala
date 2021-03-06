package com.opticdev.arrow.changes.location

import better.files.File
import com.opticdev.core.sourcegear.SourceGear
import com.opticdev.parsers.{AstGraph, ParserBase, SourceParserManager}
import com.opticdev.parsers.graph.CommonAstNode
import com.opticdev.core.sourcegear.graph.GraphImplicits._
import com.opticdev.core.sourcegear.project.monitoring.FileStateMonitor
import com.opticdev.sdk.descriptions.transformation.generate.StagedNode

import scala.util.Try

sealed trait InsertLocation {
  val file: File
  def resolveToLocation(sourceGear: SourceGear, stagedNodeOption: Option[StagedNode] = None)(implicit filesStateMonitor: FileStateMonitor) : Try[ResolvedLocation]
}

case class AsChildOf(file: File, position: Int) extends InsertLocation {
  override def resolveToLocation(sourceGear: SourceGear, stagedNodeOption: Option[StagedNode] = None)(implicit filesStateMonitor: FileStateMonitor) : Try[ResolvedLocation] = Try {

    val renderInOtherFile = stagedNodeOption.flatMap(_.options.flatMap(_.inFile))

    if (renderInOtherFile.isDefined) {
      return Try(EndOfFile(File(renderInOtherFile.get), SourceParserManager.selectParserForFileName(renderInOtherFile.get).get))
    }

    val fileContents = filesStateMonitor.contentsForFile(file).get
    val languageName = SourceParserManager.selectParserForFileName(file.name).get.languageName

    val parsed = sourceGear.parseString(fileContents)(null, languageName)
    val graph = parsed.get.astGraph
    val parser = parsed.get.parser

    val blockTypes = parser.blockNodeTypes

    val backupRoot = graph.root

    val possibleParents = graph.nodes.toVector.collect {
      case n if (n.value.isAstNode() &&
        blockTypes.nodeTypes.contains(n.value.asInstanceOf[CommonAstNode].nodeType) &&
        n.value.asInstanceOf[CommonAstNode].range.contains(position)) ||

        n.value == backupRoot.get //ensures that there is always a parent node even if its outside of range

          => n.value.asInstanceOf[CommonAstNode]
    }


    //we want the deepest block node that contains our desired insert location
    val actualParent = possibleParents.maxBy(_.graphDepth(graph))

    val children = actualParent.childrenOfType(parser.blockNodeTypes.getPropertyPath(actualParent.nodeType).get)(graph)
      .map(_._2)
    //by counting all the children that come before we can determine the insertion index
    var insertionIndex = children.count((n)=> {
      n.range.end < position
    })

    //prefer insertions after current node
    if (children.exists(_.range.contains(position))) {
      insertionIndex = children.indexOf(children.find(_.range.contains(position)).get) + 1
    }

    ResolvedChildInsertLocation(file, insertionIndex, actualParent, graph, parser)
  }
}

case class RawPosition(file: File, position: Int) extends InsertLocation {
  override def resolveToLocation(sourceGear: SourceGear, stagedNodeOption: Option[StagedNode] = None)(implicit filesStateMonitor: FileStateMonitor) : Try[ResolvedLocation] = Try {

    val renderInOtherFile = stagedNodeOption.flatMap(_.options.flatMap(_.inFile))

    if (renderInOtherFile.isDefined) {
      return Try(EndOfFile(File(renderInOtherFile.get), SourceParserManager.selectParserForFileName(renderInOtherFile.get).get))
    }

    val fileContents = filesStateMonitor.contentsForFile(file).get
    val languageName = SourceParserManager.selectParserForFileName(file.name).get.languageName
    val parsed = sourceGear.parseString(fileContents)(null, languageName)
    val parser = parsed.get.parser
    ResolvedRawLocation(file, position, parser)
  }
}

//case class InContainer(container: CommonAstNode, atIndex: RelativeIndex = Last) extends InsertLocation

/* Resolved Location */
sealed trait ResolvedLocation {
  val parser : ParserBase
  val file : File
}
case class ResolvedRawLocation(file: File, rawPosition: Int, parser : ParserBase) extends ResolvedLocation
case class ResolvedChildInsertLocation(file: File, index: Int, parent: CommonAstNode, graph: AstGraph, parser : ParserBase) extends ResolvedLocation
case class EndOfFile(file: File, parser : ParserBase) extends ResolvedLocation