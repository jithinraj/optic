package com.opticdev.core.sourcegear.project.status

import java.util.Calendar

import com.opticdev.core.sourcegear.project._

class ProjectStatus(private var _sourceGearStatus: SourceGearStatus = Empty,
                    private var _monitoringStatus: MonitoringStatus = NotWatching,
                    private var _configStatus: ConfigStatus = ValidConfig,
                    private var _firstPass: FirstPassStatus = NotStarted,
                    private var _lastUpdate: LastUpdateDate = LastUpdateDate(Calendar.getInstance().getTime)) {

  //@todo consolidate replace with macros
  def sourceGearStatus = _sourceGearStatus
  def sourceGearStatus_=(s: SourceGearStatus): Unit = {
    val changed = _sourceGearStatus != s
    _sourceGearStatus = s
    if (changed) notify(s)
  }

  def monitoringStatus = _monitoringStatus
  def monitoringStatus_=(s: MonitoringStatus): Unit = {
    val changed = _monitoringStatus != s
    _monitoringStatus = s
    if (changed) notify(s)
  }

  def configStatus = _configStatus
  def configStatus_=(s: ConfigStatus): Unit = {
    val changed = _configStatus != s
    _configStatus = s
    if (changed) notify(s)
  }

  def firstPass = _firstPass
  def firstPass_=(s: FirstPassStatus): Unit = {
    val changed = _firstPass != s
    _firstPass = s
    if (changed) notify(s)
  }

  def lastUpdate = _lastUpdate
  def lastUpdate_=(s: LastUpdateDate): Unit = {
    val changed = _lastUpdate != s
    _lastUpdate = s
    if (changed) notify(s)
  }

  private var sgChangedCallbacks = Set[(SourceGearStatus)=> Unit]()
  def sourcegearChanged(callback: (SourceGearStatus)=> Unit) = {
    sgChangedCallbacks = sgChangedCallbacks + callback
  }

  private var monitoringChangedCallbacks = Set[(MonitoringStatus)=> Unit]()
  def monitoringChanged(callback: (MonitoringStatus)=> Unit) = {
    monitoringChangedCallbacks = monitoringChangedCallbacks + callback
  }

  private var configChangedCallbacks = Set[(ConfigStatus)=> Unit]()
  def configChanged(callback: (ConfigStatus)=> Unit) = {
    configChangedCallbacks = configChangedCallbacks + callback
  }

  private var firstPassChangedCallbacks = Set[(FirstPassStatus)=> Unit]()
  def firstPassChanged(callback: (FirstPassStatus)=> Unit) = {
    firstPassChangedCallbacks = firstPassChangedCallbacks + callback
  }

  private def notify(status: ProjectStatusCase) = status match {
    case a: SourceGearStatus => sgChangedCallbacks.foreach(i=> i(a))
    case a: MonitoringStatus => monitoringChangedCallbacks.foreach(i=> i(a))
    case a: ConfigStatus => configChangedCallbacks.foreach(i=> i(a))
    case a: FirstPassStatus => firstPassChangedCallbacks.foreach(i=> i(a))
    case _ =>
  }

  lazy val immutable : ImmutableProjectStatus = new ImmutableProjectStatus(this)

}