
package utils

import java.io.File
import java.util.regex.Pattern
import play.api.Play
import play.api.Play.current

/** Provides methods for file operations
 *
 */
object Files {

  /*
   * deletes a File from the play app path
   * Sample path variable "/public/images/products/"
   * 
   */

  def deleteFile(fileName: String, filePath: String): Boolean = {
    return new File(Play.application.path + filePath + fileName).delete()
  }
  
  def deleteFilePattern(dir: File, pattern: Pattern): Unit = {  
    var files = dir.listFiles() 
    if (files != null) {
      for (file <- files) {
        if (file.isDirectory()) {
          deleteFilePattern(file, pattern)
        } else if (pattern.matcher(file.getName()).matches()) {       
          new File(file.getAbsolutePath()).delete()       
        }
      }
    }
  } 

}