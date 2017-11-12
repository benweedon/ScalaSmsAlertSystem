package controllers

import javax.inject.{Inject, Singleton}

import models.{Address, Alert}
import models.SubscriberRepository
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AlertController @Inject()(cc: ControllerComponents, repo: SubscriberRepository, messagesApi: MessagesApi)
                               (implicit ec: ExecutionContext)
  extends AbstractController(cc) with play.api.i18n.I18nSupport {

  val alertForm = Form(
    mapping(
      "address" -> nonEmptyText
    )(Alert.apply)(Alert.unapply)
  )
  def get = Action { implicit request =>
    Ok(views.html.alert(alertForm))
  }

  def post = Action.async { implicit request =>
    alertForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.alert(formWithErrors)))
      },
      alert => {
        val addresses = Address.geocode(alert.address)
        if (addresses.length == 0) {
          // TODO (benweedon 11/11/2017): Build in a mechanism for users to
          // retry if no address match was found.
          throw new IllegalArgumentException("no matching addresses found")
        } else if (addresses.length > 1) {
          // TODO (benweedon 11/11/2017): Build in a mechanism for users to
          // validate which of the potential addresses they intended.
          throw new IllegalArgumentException("the address entered was ambiguous")
        }
        // TODO (benweedon 11/11/2017): Find a better way to do this than
        // defining a new alert. Perhaps, for example, the form should just
        // produce an address string, rather than an alert object.
        val alertToSend = Alert(addresses.head.toString)
        repo.listActive().map { subscribers =>
          val messages = alertToSend.sendAlert(subscribers, messagesApi)
          Redirect(routes.HomeController.index())
            .flashing("success" -> ("Done! Messages sent with IDs " + messages.map(_.getSid()).mkString(",")))
        }
      }
    )
  }
}
