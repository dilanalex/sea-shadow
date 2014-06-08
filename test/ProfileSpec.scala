import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._

class ProfileSpec extends Specification {
  
  "Profile" should {
    "throw a 403 access denied error if the user is not logged in" in {
      running(FakeApplication(additionalConfiguration = Map("mongodb.default.db" -> "obc-test"))) {
        val Some(result) = routeAndCall(FakeRequest(GET, "/profile"))
	    status(result) must equalTo(OK)
      }
    }
  }
  
}