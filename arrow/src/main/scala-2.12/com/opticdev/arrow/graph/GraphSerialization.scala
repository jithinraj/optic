package com.opticdev.arrow.graph

import com.opticdev.sdk.descriptions.transformation.Transformation
import play.api.libs.json.{JsArray, JsBoolean, JsObject, JsString}
import scalax.collection.GraphEdge.UnDiEdge
import scalax.collection.edge.LkDiEdge

import scala.reflect.ClassTag

object GraphSerialization {
  def serialize(knowledgeGraph: KnowledgeGraph) : JsObject = {

    val noesAsJson =
    knowledgeGraph.nodes.toSeq.map {
      case (node) => jsonFromNode(node)
    }

    val edgesAsJson = knowledgeGraph.edges.toSeq.map(e=> {

      if (e.isDirected) {

        JsObject(Seq(
          "from" -> JsString(e.from.value.id),
          "to" -> JsString(e.to.value.id),
          "label" -> JsObject(Seq(
            "name" -> JsString(e.label.asInstanceOf[Transformation].yields),
            "packageFull" -> JsString(e.label.asInstanceOf[Transformation].packageId.full)
          )),
          "isTransformation" -> JsBoolean(true)
        ))

      } else {
        JsObject(Seq(
          "n1" -> JsString(e.nodes.head.id),
          "n2" -> JsString(e.nodes.last.id),
        ))
      }

    })

    JsObject(Seq(
      "nodes" -> JsArray(noesAsJson),
      "edges" -> JsArray(edgesAsJson)
    ))
  }

  def jsonFromNode(sGNode: SGNode) : JsObject = sGNode match {
    case g: GearNode => JsObject(Seq(
      "id" -> JsString(g.id),
      "name" -> JsString(g.gear.name),
      "packageFull" -> JsString(g.gear.packageFull),
      "type" -> JsString("gear")
    ))
    case s: SchemaNode => JsObject(Seq(
      "id" -> JsString(s.id),
      "name" -> JsString(s.schema.name),
      "packageFull" -> JsString(s.schema.schemaRef.packageRef.full),
      "type" -> JsString("schema")
    ))
  }

  def jsonFromEdge[A](edge: A) = {

  }

}
