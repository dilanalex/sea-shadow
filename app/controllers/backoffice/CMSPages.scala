package controllers.backoffice

import play.api._
import play.api.mvc._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.salatctx._
import models._
import helpers.Page
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.Forms._
import play.api.libs.json.JsValue
import models.CMSPage
import models.CMSPage
import models.CMSPage

object CMSPages extends Controller {

  def pageForm(implicit request: RequestHeader): Form[CMSPage] = Form(
    mapping(
      "pageId" -> nonEmptyText,
      "title" -> nonEmptyText,
      "url" -> nonEmptyText,
      "content" -> nonEmptyText,
      "published" -> optional(boolean),
      "label" -> optional(text))((
        pageId,
        title,
        url,
        content,
        published,
        label) =>
        CMSPage(
          id = new ObjectId(pageId),
          title = title,
          url = url,
          content = content,
          published = (published),
          label = label))(
        cmspage => Some(
          cmspage.id.toString,
          cmspage.title,
          cmspage.url,
          cmspage.content,
          cmspage.published,
          cmspage.label)))
  
  def show(id: ObjectId) = Action { implicit request =>
    Ok(views.html.backoffice.cms.pageshow(pageForm.fill(getPage(id))))
  }
  
  /** Display the paginated list of Pages (Static).
   *
   *  @param page Current page number (starts from 0)
   *  @param orderBy Column to be sorted
   *  @param filter Filter applied on Country names
   */
  def list(page: Int, orderBy: Int, filter: String = "%") = Action {
    implicit request =>
      val offest = 10 * page
      var pattern = ".*(?i)" + filter + ".*";
      var pages = models.CMSPage.find(MongoDBObject("title" -> pattern.r))
        .skip(offest).limit(10).toList.sortBy(c => c.title)
      var records =
        models.CMSPage.find(MongoDBObject("title" -> pattern.r)).size
      Ok(views.html.backoffice.cms.pagelist(Page(pages, page, offest,
        records), orderBy, filter))
  }
  
  def getPage(id: ObjectId): CMSPage = {
    CMSPage.findOne(MongoDBObject("_id" -> id)).getOrElse(new CMSPage)
  }
  
  def save = Action(parse.urlFormEncoded(maxLength = 1024 * 1000)) { implicit request =>
     println(request.body.get("pageId").get)
    var pageId =
      request.body.get("pageId").get.head.toString()
      println(pageId)
    pageForm.bindFromRequest.fold(form => {
      BadRequest(views.html.backoffice.cms.pageshow(form))
    },
      pageForm => {
        if (CMSPage.findOne(
          MongoDBObject("_id" -> new ObjectId(pageId))).isDefined) {
          CMSPage.update(MongoDBObject(
            "_id" -> new ObjectId(pageId)),
            grater[CMSPage].asDBObject(pageForm),
            false,
            false)
          Redirect(controllers.backoffice.routes.CMSPages.show(
            new ObjectId(pageId))).flashing("message" -> "Page updated !!!")
        } else {
          CMSPage.save(pageForm)
          Redirect(controllers.backoffice.routes.CMSPages.show(
            new ObjectId(pageId))).flashing("message" -> "Page Saved !!!")
        }
      })
  }
  
  def load(pattern: String) = Action {
    implicit request =>
    val url = "/basic/"+pattern  
    val page = CMSPage.findOne(MongoDBObject("url" -> url)).getOrElse(new CMSPage)
      Ok(views.html.backoffice.cms.templatepage(page))
  }
}