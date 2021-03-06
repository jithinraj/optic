package com.opticdev.opm.storage

import java.io.FileNotFoundException

import better.files.File
import com.opticdev.common.PackageRef
import com.opticdev.common.storage.DataDirectory
import com.opticdev.common.utils.SemverHelper
import com.opticdev.opm.packages.{OpticMDPackage, OpticPackage, StagedPackage}
import com.opticdev.common.utils.SemverHelper.VersionWrapper
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.Semver.SemverType
import play.api.libs.json.{JsObject, Json}

import scala.util.{Failure, Try}

object PackageStorage {

  def writeToStorage(opticPackage: OpticPackage): File =
    writeToStorage(opticPackage.packageRef, opticPackage.description.toString())

  def writeToStorage(packageRef: PackageRef, contents: String): File = {
    val packages = DataDirectory.packages / ""  createIfNotExists(asDirectory = true)

    val author = packages / packageRef.namespace createIfNotExists(asDirectory = true)
    val name = author / packageRef.name createIfNotExists(asDirectory = true)
    val version = name / packageRef.version

    version.delete(true)
    version.touch()

    version.write(contents)
  }

  def loadFromStorage(packageRef: PackageRef) : Try[OpticPackage] = {

    def notFound = Failure(new FileNotFoundException("Can not find local version of package "+packageRef.packageId))

    val packageDirectory = DataDirectory.packages / packageRef.namespace / packageRef.name

    if (packageDirectory.exists && packageDirectory.isDirectory) {

      val versionOption = SemverHelper.findVersion(packageDirectory.list.toSet, (file: File) => VersionWrapper(file.name), packageRef.version)

      if (versionOption.isDefined) {
        val (version, file) = versionOption.get
        Try(StagedPackage(Json.parse(file.contentAsString).as[JsObject]))
      } else {
        notFound
      }

    } else {
      notFound
    }

  }

  def installedPackages: Vector[String] = {
    val authors = DataDirectory.packages.list.filter(i=> !i.isHidden && i.isDirectory)

    authors.flatMap(i=> {
      val authorName = i.name
      val packages = i.list.filter(i=> !i.isHidden && i.isDirectory)

      packages.flatMap(p=> {

        val packageName = p.name

        p.list.filter(i=> !i.isHidden && i.isRegularFile && Try(new Semver(i.name, SemverType.NPM)).isSuccess)
          .map(ver=> authorName+":"+packageName+"@"+ver.name)

      })

    }).toVector.sorted
  }

  def clearLocalPackages = {
    DataDirectory.packages.list.foreach(_.delete(true))
  }


}
