package com.thenewmotion.akka.http

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import akka.actor.{ActorSystem, Props}
import Http._


class AkkaHttpServlet extends HttpServlet {

  private[http] var _actorSystem: Option[ActorHttpSystem] = None

  override def init() {
    super.init()

    val system = ActorHttpSystem()
    system.actorOf(Props[EndpointsActor], system.endpointsPath)
    _actorSystem = Some(system)
    onSystemInit(system)
  }

  override def destroy() {
    super.destroy()

    _actorSystem.foreach {
      system =>
        onSystemDestroy(system)
        system.shutdown()
    }
    _actorSystem = None
  }

  def onSystemInit(system: ActorHttpSystem) {}
  def onSystemDestroy(system: ActorHttpSystem) {}

  override def doPost(req: HttpServletRequest, res: HttpServletResponse) {doActor(req, res)}
  override def doPut(req: HttpServletRequest, res: HttpServletResponse) {doActor(req, res)}
  override def doDelete(req: HttpServletRequest, res: HttpServletResponse) {doActor(req, res)}
  override def doTrace(req: HttpServletRequest, res: HttpServletResponse) {doActor(req, res)}
  override def doHead(req: HttpServletRequest, res: HttpServletResponse) {doActor(req, res)}
  override def doGet(req: HttpServletRequest, res: HttpServletResponse) {doActor(req, res)}

  private def doActor(req: HttpServletRequest, res: HttpServletResponse) {
    val system = _actorSystem.get
    val props = Props[AsyncActor].withDispatcher("akka.http.actor.dispatcher")
    val actor = system.actorOf(props)

    val asyncContext = req.startAsync()
    asyncContext.setTimeout(system.asyncTimeout)
    asyncContext.addListener(new Listener(actor, system))

    actor ! asyncContext
  }
}