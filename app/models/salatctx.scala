package models

import com.novus.salat.Context
import play.api.Play
import play.api.Play._
import com.novus.salat._

package object salatctx {

  /*type Salat = com.novus.salat.annotations.Salat
  type EnumAs = com.novus.salat.annotations.EnumAs
  type Ignore = com.novus.salat.annotations.Ignore
  type Key = com.novus.salat.annotations.Key
  type Persist = com.novus.salat.annotations.Persist*/

  /** Here is where we define the custom Play serialization context,
   *  including the Play classloader.
   */
  implicit val ctx = {
    val c = new Context {
      val name = "play-context"
      //override val typeHintStrategy = StringTypeHintStrategy(
      //        when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
    }
    c.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
    c.registerClassLoader(Play.classloader)

    c
  }

}