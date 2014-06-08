package utils

import java.awt.image.BufferedImage
import org.imgscalr.Scalr
import javax.imageio.ImageIO
import java.io.File
import play.Play
import play.api.Play.current
import play.api.mvc.Controller
import org.apache.commons.io.FilenameUtils

/** provides image manipulation functions
 *
 */
object ImageProcess {

  val sourceImagePath = Play.application.path + "/public/images/products/"
  val resizedImagePath = Play.application.path + "/public/images/products/resized/"
  val noImageName = "no_Image.jpg"

  /*
   *  Dynamic image resizing
   */
  def pictureResize(imageName: String, width: Int, height: Int): File = {

    var resizedFile = new File(
      resizedImagePath + width.toString() + "x" + height.toString() + "-" + imageName)

    var sourceImage: BufferedImage = null

    if (!resizedFile.exists) {

      try {
        sourceImage = ImageIO.read(new File(sourceImagePath + imageName))

        ImageIO.write(Scalr.resize(sourceImage,
          Scalr.Method.QUALITY, width, height),
          FilenameUtils.getExtension(imageName), resizedFile);

      } catch {
        case _ =>
          val resizedNoImageName =
            width.toString() + "x" + height.toString() + "-" + noImageName

          resizedFile = new File(resizedImagePath + resizedNoImageName)

          if (!resizedFile.exists) {

            sourceImage = ImageIO.read(new File(sourceImagePath + noImageName))

            ImageIO.write(Scalr.resize(sourceImage,
              Scalr.Method.QUALITY, width, height),
              FilenameUtils.getExtension(noImageName), resizedFile);
          }
      }

    }
    return resizedFile
  }

  /*
   *  Uploads a image to the location in the application path
   *
   */

  def uploadImage(
    request: play.api.mvc.Request[play.api.mvc.MultipartFormData[play.api.libs.Files.TemporaryFile]],
    imagePath: String, fileName: String, fieldName: String): Boolean = {
    var ret = false
    try {
      request.body.file(fieldName).map { picture =>
        var filename = picture.filename
        if (!filename.isEmpty) {
          filename = fileName
          picture.ref.moveTo(new File(Play.application.path + imagePath +
            fileName), true)
          ret = true
        } else {
          ret = false
        }
      }
    } catch {
      case _ =>
        return ret
    }
    return ret
  }

}