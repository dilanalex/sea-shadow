/*import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.main.Execute

class CustomerRegisterSpec extends Specification {

  "Tests for Customer Registration" should {

    "route and call to GET /register" in {
      val Some(result) = routeAndCall(FakeRequest(GET, "/register"))
      status(result) must equalTo(OK)
      contentType(result) must beSome("text/html")
      charset(result) must beSome("utf-8")
      contentAsString(result) must contain("Customer Registration")

    }

    "Test from within a browser" in {
      running(TestServer(7055), FIREFOX) { browser =>
        browser.goTo("http://localhost:7055/register")

        browser.$("#email").text("")
        browser.$("#password").text("")
        browser.$("#register").submit()
        browser.pageSource must contain("This field is required")

        browser.$("#email").text("kapila1@inbay.co.uk")
        browser.$("#password").text("")
        browser.$("#register").submit()
        browser.pageSource must contain("This field is required")

        browser.$("#email").text("")
        browser.$("#password").text("secret")
        browser.$("#register").submit()
        browser.pageSource must contain("This field is required")

        browser.$("#email").text("kapila51@inbay.co.uk")
        browser.$("#password").text("secret111")
        browser.$("#register").submit()
        browser.pageSource must contain("Customer Registered!")

        browser.$("#email").text("kapila51@inbay.co.uk")
        browser.$("#password").text("secret111")
        browser.$("#register").submit()
        browser.pageSource must contain("Customer exist!")
     
      }
    }

  }
}*/