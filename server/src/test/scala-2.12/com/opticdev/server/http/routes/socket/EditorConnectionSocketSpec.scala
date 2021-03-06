package com.opticdev.server.http.routes.socket

import akka.http.scaladsl.testkit.WSProbe
import better.files.File
import com.opticdev.core.Fixture.{SocketTestFixture, TestBase}
import com.opticdev.core.sourcegear.project.monitoring.StagedContent
import com.opticdev.server.Fixture.ProjectsManagerFixture
import com.opticdev.server.http.controllers.ContextQuery
import com.opticdev.server.http.routes.socket.editors.EditorConnection
import com.opticdev.server.http.routes.socket.editors.Protocol._
import com.opticdev.server.state.ProjectsManager
import play.api.libs.json.{JsNumber, JsObject, JsString}

import scala.concurrent.duration._
import scala.concurrent.Await

class EditorConnectionSocketSpec extends SocketTestFixture with TestBase with ProjectsManagerFixture {

  super.beforeAll()

  val future = instanceWatchingTestProject
  implicit val projectsManager = Await.result(future, 10 seconds)

  def fixture = new {
    val wsClient = WSProbe()
    val socketRoute = new SocketRoute()
  }

  describe("Editor Socket") {

    val f = fixture

    WS("/socket/editor/sublime?autorefreshes=true", f.wsClient.flow) ~> f.socketRoute.route ~>
      check {

        it("Connects properly") {
          assert(EditorConnection.listConnections.size == 1)
          assert(EditorConnection.listConnections.head._1 == "sublime")
          assert(EditorConnection.listConnections.head._2.autorefreshes)
        }

        describe("Can send a context query") {

          it("Accepts a valid context query") {

            f.wsClient.sendMessage(
              JsObject(
                Seq("event" -> JsString("context"),
                  "file" -> JsString("test-examples/resources/tmp/test_project/app.js"),
                  "start" -> JsNumber(35),
                  "end" -> JsNumber(37)
                ))
                .toString())
          }

          it("Rejects invalid queries") {

            f.wsClient.sendMessage(
              JsObject(
                Seq("event" -> JsString("context")))
                .toString())

            f.wsClient.expectMessage("Invalid Request")

          }
        }

        describe("Can send messages to client") {

          it("FileUpdate works") {
            val connection = EditorConnection.listConnections.head._2
            val event = FilesUpdated(Map(File("path/to/file") -> StagedContent("")))

            connection.sendUpdate(event)

            f.wsClient.expectMessage(event.asString)

          }

        }

      }
  }
}

