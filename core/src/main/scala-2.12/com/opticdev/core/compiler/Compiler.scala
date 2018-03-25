package com.opticdev.core.compiler

import com.opticdev.core.compiler.errors.ErrorAccumulator
import com.opticdev.core.compiler.stages._
import com.opticdev.core.sourcegear.Gear
import com.opticdev.core.sourcegear.containers.SubContainerManager
import com.opticdev.core.sourcegear.gears.parsing.ParseAsModel
import com.opticdev.core.sourcegear.variables.VariableManager
import com.opticdev.opm.context.{Context, PackageContext}
import com.opticdev.opm.DependencyTree
import com.opticdev.opm.packages.OpticMDPackage
import com.opticdev.sdk.descriptions.{Lens, Schema}

import scala.collection.mutable.ListBuffer
import scala.util.Try

object Compiler {
  def setup(opticPackage: OpticMDPackage)(implicit logToCli: Boolean = false, dependencyTree: DependencyTree) : CompilerPool = {

    implicit val packageContext = dependencyTree.treeContext(opticPackage.packageFull).get

    implicit val errorAccumulator: ErrorAccumulator = new ErrorAccumulator

    new CompilerPool(opticPackage, opticPackage.lenses.map(l=> new CompileWorker(l)).toSet)
  }

  class CompilerPool(opticPackage: OpticMDPackage, val compilers: Set[CompileWorker])(implicit packageContext: Context, dependencyTree: DependencyTree, errorAccumulator: ErrorAccumulator, logToCli: Boolean = false) {

    private implicit var completed: ListBuffer[Output] = new scala.collection.mutable.ListBuffer[Output]()

    private def clear = completed = new scala.collection.mutable.ListBuffer[Output]()

    def execute: CompilerOutput = {
      clear
      //@todo par should ideally be used here, but it is inconsistent for some reason. need to look into race conditions
      CompilerOutput(opticPackage, compilers.map(_.compile).seq, dependencyTree.flattenSchemas)
    }
  }

  class CompileWorker(sourceLens: Lens) {
    def compile()(implicit packageContext: Context, completed: ListBuffer[Output] = ListBuffer(), errorAccumulator: ErrorAccumulator = new ErrorAccumulator, logToCli: Boolean = false): LensCompilerOutput = {
      implicit val lens = sourceLens

//      val cliLogger = new InstallSessionMonitor(lens.name)

//      if (logToCli) cliLogger.start

      //@todo reorder this / abstract. Looks very dirty.

//      if (logToCli) cliLogger.validateDescription

      val validationOutput = new ValidationStage().run

//      if (logToCli) cliLogger.parsingSnippets

      //Find the right parser and snippets into an AST Tree Graph
      val snippetBuilder = new SnippetStage(lens.snippet)
      val snippetOutput = Try(snippetBuilder.run)


      //snippet stage must succeed for anything else to happen.
      if (snippetOutput.isSuccess) {

//        if (logToCli) cliLogger.evaluatingFinders

        implicit val variableManager = new VariableManager(lens.variables, snippetOutput.get.parser.identifierNodeDesc)
        implicit val subcontainersManager = new SubContainerManager(lens.subcontainers, snippetOutput.get.containerMapping)

        val finderStage = new FinderStage(snippetOutput.get)
        val finderStageOutput = Try(finderStage.run)

        if (finderStageOutput.isSuccess) {
//          if (logToCli) cliLogger.writingParser
          val parser = Try(new ParserFactoryStage(snippetOutput.get, finderStageOutput.get).run)

          if (parser.isSuccess) {
//            if (logToCli) cliLogger.writingGenerator
            val renderer = Try(new RenderFactoryStage(snippetOutput.get, parser.get.parseGear).run)
            if (renderer.isSuccess) {

              val finalGear = Gear(lens.name, lens.packageRef.full, lens.schema, snippetOutput.get.enterOn, parser.get.parseGear.asInstanceOf[ParseAsModel], renderer.get.renderGear)

//              if (logToCli) cliLogger.gearFinished

              return Success(sourceLens, finalGear)

            } else errorAccumulator.handleFailure(renderer.failed)

          } else {
            errorAccumulator.handleFailure(parser.failed)
          }

        } else {
          errorAccumulator.handleFailure(finderStageOutput.failed)
        }

      } else {
        errorAccumulator.handleFailure(snippetOutput.failed)
      }

      Failure(lens, errorAccumulator)
    }
  }

}
